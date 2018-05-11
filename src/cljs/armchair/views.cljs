(ns armchair.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [armchair.game :refer [start-game end-game]]
            [armchair.slds :as slds]
            [armchair.position :refer [apply-delta]]
            [armchair.config :as config]
            [clojure.core.async :refer [put!]]))

;; Helpers

(def <sub (comp deref re-frame.core/subscribe))
(def >evt re-frame.core/dispatch)

(defn e-> [handler]
  (fn [e]
    (.preventDefault e)
    (.stopPropagation e)
    (handler e)))

(defn relative-pointer [e elem]
  (let [rect (.getBoundingClientRect elem)]
    [(- (.-clientX e) (.-left rect))
     (- (.-clientY e) (.-top rect))]))

(defn e->graph-pointer [e]
  (relative-pointer e (-> js/document
                          (.getElementsByClassName "graph")
                          (aget 0))))

(def left-button? #(zero? (.-button %)))

(defn record-update-handler [record-type id field]
  (let [record-event (keyword (str "update-" (name record-type)))]
    (fn [event]
      (>evt [record-event id field (-> event .-target .-value)]))))

;; Graph (drag & drop)

(defn start-dragging-handler [position-ids]
  (e-> #(when (left-button? %)
          (>evt [:start-dragging position-ids (e->graph-pointer %)]))))

(defn graph-item [{:keys [position position-id]} component]
  (let [dragging? (<sub [:dragging? position-id])]
    [:div {:class ["graph__item"
                   (when dragging? "graph__item_is-dragging")]
           :on-mouse-down (start-dragging-handler #{position-id})
           :on-mouse-up (e-> #(when dragging? (>evt [:end-dragging])))
           :style {:left (first position)
                   :top (second position)}}
     component]))

(defn graph-connection [{:keys [kind start end]}]
  [:line {:class ["graph__connection"
                  (when (= kind :connector) "graph__connection_is-connector")]
          :x1 (first start)
          :y1 (second start)
          :x2 (first end)
          :y2 (second end)}])

(defn graph [{:keys [items kind item-component connections connection-transform]
              :or {connections '()
                   connection-transform identity}}]
  (let [position-ids (->> items vals (map :position-id) set)
        connector (<sub [:connector])
        connecting? (some? connector)
        dragging? (<sub [:dragging?])]
    [:div {:class ["graph"
                   (when dragging? "graph_is-dragging")
                   (when connecting? "graph_is-connecting")]
           :on-mouse-down (start-dragging-handler position-ids)
           :on-mouse-move (e-> #(when (or dragging? connecting?)
                                  (>evt [:move-pointer (e->graph-pointer %)])))
           :on-mouse-up (e-> #(cond
                                connecting? (>evt [:abort-connecting])
                                dragging? (>evt [:end-dragging])))}
     [:svg {:class "graph__connection-container" :version "1.1"
            :baseProfile "full"
            :xmlns "http://www.w3.org/2000/svg"}
      (for [[start end] connections]
        ^{:key (str kind ":" start "->" end)}
        [graph-connection (connection-transform {:start (get-in items [start :position])
                                                 :end (get-in items [end :position])})])
      (when connecting? [graph-connection connector])]
     (for [[id item] items]
       ^{:key (str kind id)} [graph-item item [item-component item]])]))

;; Components

(defn icon [glyph]
  [:i {:class (str "fas fa-" glyph)}])

(defn line-component [{:keys [id text character-color] :as line}]
  (let [connecting? (some? (<sub [:connector]))]
    [:div {:class "line"
           :on-mouse-up (when connecting? #(>evt [:end-connecting-lines id]))
           :style {:border-color character-color
                   :width (str config/line-width "px")}}
     [:p text]
     [:div {:class "item-actions"
            :on-mouse-down (e-> identity)}
      [:div {:class "item-action"
             :on-click #(>evt [:delete-line id])}
       [icon "trash"]]
      [:div {:class "item-action"
             :on-click #(>evt [:open-line-modal id])}
       [icon "edit"]]
      [:div {:class "item-action item-action_connect"
             :on-mouse-down (e-> #(when (left-button? %)
                                    (>evt [:start-connecting-lines id (e->graph-pointer %)])))}
       [icon "link"]]]]))

(defn line-form-modal []
  (let [{:keys [line-id]} (<sub [:modal])
        update-handler (partial record-update-handler :line line-id)]
    (if-let [line (get (<sub [:lines]) line-id)]
      (let [{:keys [id text character-id color]} line
            characters (<sub [:characters])]
        [slds/modal {:title (str "Line #" id)
                     :close-handler #(>evt [:close-modal])
                     :content [slds/form
                               [slds/input-select {:label "Character"
                                                   :on-change (update-handler :character-id)
                                                   :options (map (fn [[k c]] [k (:display-name c)]) characters)
                                                   :value character-id}]
                               [slds/input-textarea {:label "Text"
                                                     :on-change (update-handler :text)
                                                     :value text}]]}]))))

(defn dialogue-component []
  (let [{:keys [lines connections]} (<sub [:dialogue])]
    [:div {:class "full-page"}
     [:div {:class "new-item-button"}
      [slds/add-button "New" #(>evt [:create-line])]]
     [graph {:kind "line"
             :items lines
             :connections connections
             :connection-transform (fn [{:keys [start end]}]
                                     {:start (apply-delta start [(- config/line-width 15) 15])
                                      :end (apply-delta end [15 15])})
             :item-component line-component}]]))

(defn character-form-modal []
  (let [{:keys [character-id]} (<sub [:modal])
        update-handler (partial record-update-handler :character character-id)]
    (if-let [character (get (<sub [:characters]) character-id)]
      (let [{:keys [display-name color]} character]
        [slds/modal {:title display-name
                     :close-handler #(>evt [:close-modal])
                     :content [slds/form
                               [slds/input-text {:label "Name"
                                                 :on-change (update-handler :display-name)
                                                 :value display-name}]
                               [slds/input-text {:label "Color"
                                                 :on-change (update-handler :color)
                                                 :value color}]]}]))))

(defn character-management []
  (let [characters (<sub [:characters])]
    [slds/resource-page "Characters"
     {:columns [:id :display-name :color :lines :actions]
      :collection (vals characters)
      :cell-views {:color slds/color-cell
                   :actions (fn [{:keys [id lines]} _]
                              [:div {:class "slds-text-align_right"}
                               (when (zero? lines)
                                 [slds/symbol-button "trash-alt" {:on-click #(>evt [:delete-character id])}])
                               [slds/symbol-button "edit" {:on-click #(>evt [:open-character-modal id])}]])}
      :new-resource #(>evt [:create-character])}]))

(defn location-form-modal []
  (let [{:keys [location-id]} (<sub [:modal])
        update-handler (partial record-update-handler :location location-id)]
    (if-let [location (get (<sub [:locations]) location-id)]
      (let [display-name (:display-name location)]
        [slds/modal {:title display-name
                     :close-handler #(>evt [:close-modal])
                     :content [slds/form
                               [slds/input-text {:label "Name"
                                                 :on-change (update-handler :display-name)
                                                 :value display-name}]]}]))))

(defn location-component [{:keys [id display-name] :as location}]
  (let [connecting? (some? (<sub [:connector]))]
    [:div {:class "location"
           :on-mouse-up (when connecting? #(>evt [:end-connecting-locations id]))
           :style {:width (str config/line-width "px")}}
     [:p {:class "name"} display-name]
     [:div {:class "item-actions"
            :on-mouse-down (e-> identity)}
      [:div {:class "item-action"
             :on-click #(>evt [:delete-location id])}
       [icon "trash"]]
      [:div {:class "item-action"
             :on-click #(>evt [:open-location-modal id])}
       [icon "edit"]]
      [:div {:class "item-action item-action_connect"
             :on-mouse-down (e-> #(when (left-button? %)
                                    (>evt [:start-connecting-locations id (e->graph-pointer %)])))}
       [icon "link"]]]]))

(defn location-management []
  (let [{:keys [locations connections]} (<sub [:location-map])]
    [:div {:class "full-page"}
     [:div {:class "new-item-button"}
      [slds/add-button "New" #(>evt [:create-location])]]
     [graph {:kind "location"
             :items locations
             :connections connections
             :connection-transform (fn [{:keys [start end]}]
                                     {:start (apply-delta start [(/ config/line-width 2) 15])
                                      :end (apply-delta end [(/ config/line-width 2) 15])})
             :item-component location-component}]]))

(defn game-canvas []
  (let [canvas-ref (atom nil)
        game-input (atom nil)]
    (r/create-class
      {:component-did-mount (fn []
                              (reset! game-input (start-game (.getContext @canvas-ref "2d"))))
       :component-will-unmount (fn [] (end-game))
       :reagent-render (fn []
                         [:div {:id "game-container"}
                          [:canvas {:id "game-canvas"
                                    :on-mouse-move #(put! @game-input [:highlight (relative-pointer % @canvas-ref)])
                                    :on-mouse-out #(put! @game-input [:highlight nil])
                                    :height 450
                                    :width 800
                                    :ref (fn [el] (reset! canvas-ref el))}]])})))

(defn root []
  (let [current-page (<sub [:current-page])
        pages (array-map
                "Dialogue" [dialogue-component]
                "Characters" [character-management]
                "Locations" [location-management]
                "Game" [game-canvas])
        link-map (map
                   (fn [name] [name #(>evt [:show-page name])])
                   (keys pages))]
    [:div {:id "page"}
     [line-form-modal]
     [character-form-modal]
     [location-form-modal]
     [:a {:id "reset"
          :on-click #(>evt [:reset-db])} "reset"]
     [:div {:id "navigation"}
      [slds/global-navigation link-map current-page]]
     [:div {:id "content"}
      (get pages current-page [:div "Nothing"])]]))
