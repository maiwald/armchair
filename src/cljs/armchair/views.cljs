(ns armchair.views
  (:require [re-frame.core :as re-frame]
            [clojure.spec.alpha :as s]
            [reagent.core :as r]
            [armchair.game :refer [start-game end-game]]
            [armchair.slds :as slds]
            [armchair.util :refer [translate-position]]
            [armchair.config :as config]
            [clojure.core.async :refer [put!]]))

;; Helpers

(def <sub (comp deref re-frame.core/subscribe))
(def >evt re-frame.core/dispatch)

(defn stop-e! [e]
  (.preventDefault e)
  (.stopPropagation e))

(defn e-> [handler]
  (fn [e]
    (stop-e! e)
    (handler e)))

(defn e->val [e]
  (-> e .-target .-value))

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

;; Drag & Drop

(defn start-dragging-handler [position-ids]
  (e-> #(when (left-button? %)
          (>evt [:start-dragging position-ids (e->graph-pointer %)]))))

(defn connection [{:keys [key-prop kind start end]}]
  [:line {:class ["graph__connection"
                  (when (= kind :connector) "graph__connection_is-connector")]
          :x1 (first start)
          :y1 (second start)
          :x2 (first end)
          :y2 (second end)}])

(defn drag-item [{:keys [position position-id]} component]
  (let [dragging? (<sub [:dragging-item? position-id])]
    [:div {:class ["graph__item"
                   (when dragging? "graph__item_is-dragging")]
           :on-mouse-down (start-dragging-handler #{position-id})
           :on-mouse-up (e-> #(when dragging? (>evt [:end-dragging])))
           :style {:left (first position)
                   :top (second position)}}
     component]))

(defn drag-canvas [{:keys [items kind item-component]} & connection-children]
  (let [position-ids (->> items vals (map :position-id) set)
        connecting? (some? (<sub [:connector]))
        dragging? (<sub [:dragging?])]
    [:div {:class (cond-> ["graph"]
                    dragging? (conj "graph_is-dragging")
                    connecting? (conj "graph_is-connecting"))
           :on-mouse-down (start-dragging-handler position-ids)
           :on-mouse-move (e-> #(when (or dragging? connecting?)
                                  (>evt [:move-pointer (e->graph-pointer %)])))
           :on-mouse-up (e-> #(cond
                                connecting? (>evt [:abort-connecting])
                                dragging? (>evt [:end-dragging])))}
     (into [:div] connection-children)
     (for [[id item] items]
       ^{:key (str kind id)} [drag-item item [item-component item]])]))

;; Components

(defn icon [glyph]
  [:i {:class (str "fas fa-" glyph)}])

(defn npc-line-component [{:keys [id initial-line? text character-name character-color]}]
  (let [connecting? (some? (<sub [:connector]))]
    [:div {:class "line"
           :on-mouse-up (when connecting? #(>evt [:end-connecting-lines id]))
           :style {:border-color character-color
                   :width (str config/line-width "px")
                   }}
     [:div {:class "line__meta"}
      [:p {:class "id"} (str "#" id)]
      [:p {:class "name"} character-name]
      [:ul {:class "actions" :on-mouse-down stop-e!}
       (when-not initial-line?
         [:li {:class "action" :on-click #(>evt [:delete-line id])}
          [icon "trash"]])
       [:li {:class "action" :on-click #(>evt [:open-npc-line-modal id])}
        [icon "edit"]]]]
     [:div {:class "line__text"
            :style {:height (str config/line-height "px")}}
      [:p text]
      [:div {:class "action action_connect"
             :on-mouse-down (e-> #(when (left-button? %)
                                    (>evt [:start-connecting-lines id (e->graph-pointer %)])))}
       [icon "project-diagram"]]]
     ]))

(defn player-line-component [{:keys [id initial-line? options]}]
  (let [connecting? (some? (<sub [:connector]))]
    [:div {:class "line"
           :on-mouse-up (when connecting? #(>evt [:end-connecting-lines id]))
           :style {:width (str config/line-width "px")}}
     [:div {:class "line__meta"}
      [:p {:class "id"} (str "#" id)]
      [:p {:class "name"} "Player"]
      [:ul {:class "actions" :on-mouse-down stop-e!}
       (when-not initial-line?
         [:li {:class "action" :on-click #(>evt [:delete-line id])}
          [icon "trash"]])
       [:li {:class "action" :on-click #(>evt [:open-player-line-modal id])}
        [icon "edit"]]]]
     [:ul {:class "line__options"}
      (map-indexed (fn [index option]
                     [:li {:key (str "line-option" id ":" index)
                           :class "line__text"
                           :style {:height (str config/line-height "px")}}
                      [:p (:text option)]
                      [:div {:class "action action_connect"
                             :on-mouse-down (e-> #(when (left-button? %)
                                                    (>evt [:start-connecting-lines id (e->graph-pointer %) index])))}
                       [icon "project-diagram"]]])
                   options)]]))

(defn line-component [line]
  (case (:kind line)
    :npc [npc-line-component line]
    :player [player-line-component line]))

(defn npc-connection [start-position end-position]
  [connection {:start (translate-position start-position [(- config/line-width 15) (+ 33 (/ config/line-height 2))])
               :end (translate-position end-position [15 (+ 33 (/ config/line-height 2))])}])

(defn player-connection [start-position index end-position]
  [connection {:start (translate-position start-position [(- config/line-width 15)
                                                          (+ 33
                                                             (/ config/line-height 2)
                                                             (* index config/line-height))])
               :end (translate-position end-position [15 (+ 33 (/ config/line-height 2))])}])

(defn dialogue-component [dialogue-id]
  (if dialogue-id
    (let [{:keys [lines npc-connections player-connections]} (<sub [:dialogue dialogue-id])
          get-pos #(get-in lines [% :position])]
      [:div {:class "full-page"}
       [:div {:class "new-item-button"}
        [slds/add-button "New Player Line" #(>evt [:create-player-line dialogue-id])]
        [slds/add-button "New NPC Line" #(>evt [:create-npc-line dialogue-id])]]
       [drag-canvas {:kind "line"
                     :items lines
                     :item-component line-component}
        [:svg {:class "graph__connection-container" :version "1.1"
               :baseProfile "full"
               :xmlns "http://www.w3.org/2000/svg"}
         (when-let [connector (<sub [:connector])]
           [connection connector])
         (for [[start end] npc-connections]
           ^{:key (str "line-connection:" start "->" end)}
           [npc-connection (get-pos start) (get-pos end)])
         (for [[start index end] player-connections]
           ^{:key (str "response-connection:" start ":" index "->" end)}
           [player-connection (get-pos start) index (get-pos end)])]]])
    [:span "No dialogue selected!"]))

(defn npc-line-form-modal []
  (if-let [line-id (:npc-line-id (<sub [:modal]))]
    (let [line (<sub [:line line-id])
          update-handler (fn [field] #(>evt [:update-line line-id field (e->val %)]))]
      [slds/modal {:title (str "Line #" line-id)
                   :close-handler #(>evt [:close-modal])
                   :content [slds/form
                             [slds/input-select {:label "Character"
                                                 :on-change (update-handler :character-id)
                                                 :options (<sub [:character-options])
                                                 :value (:character-id line)}]
                             [slds/input-textarea {:label "Text"
                                                   :on-change (update-handler :text)
                                                   :value (:text line)}]]}])))

(defn player-line-form-modal []
  (if-let [line-id (:player-line-id (<sub [:modal]))]
    (let [line (<sub [:line line-id])]
      [slds/modal {:title (str "Line #" line-id)
                   :close-handler #(>evt [:close-modal])
                   :content [:div {:class "player-line-form"}
                             [slds/form
                              (map-indexed
                                (fn [index option]
                                  [:div {:key index
                                         :class "player-line-form__response"}
                                   [:div {:class "text"}
                                    [slds/input-textarea {:label (str "Response " (inc index))
                                                          :on-change #(>evt [:update-option line-id index (e->val %)])
                                                          :value (:text option)}]]
                                   [:ul {:class "actions actions_vertial"}
                                    [:li {:class "action" :on-click #(>evt [:delete-option line-id index])}
                                     [icon "trash"]]
                                    (when-not (= index 0)
                                      [:li {:class "action" :on-click #(>evt [:move-option line-id index :up])}
                                       [icon "arrow-up"]])
                                    (when-not (= index (dec (count (:options line))))
                                      [:li {:class "action" :on-click #(>evt [:move-option line-id index :down])}
                                       [icon "arrow-down"]])]])
                                (:options line))]
                             [slds/add-button "New Option" #(>evt [:add-option line-id])]]}])))

(defn character-form-modal []
  (if-let [character-id (:character-id (<sub [:modal]))]
    (let [character (<sub [:character character-id])
          update-handler (partial record-update-handler :character character-id)]
      [slds/modal {:title (:display-name character)
                   :close-handler #(>evt [:close-modal])
                   :content [slds/form
                             [slds/input-text {:label "Name"
                                               :on-change (update-handler :display-name)
                                               :value (:display-name character)}]
                             [slds/input-text {:label "Color"
                                               :on-change (update-handler :color)
                                               :value (:color character)}]]}])))

(defn character-management []
  (let [characters (<sub [:character-list])]
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
  (if-let [location-id (:location-id (<sub [:modal]))]
    (let [location (<sub [:location location-id])
          update-handler (partial record-update-handler :location location-id)]
      [slds/modal {:title (:display-name location)
                   :close-handler #(>evt [:close-modal])
                   :content [slds/form
                             [slds/input-text {:label "Name"
                                               :on-change (update-handler :display-name)
                                               :value (:display-name location)}]]}])))

(defn location-component [{:keys [id display-name dialogues] :as location}]
  (let [connecting? (some? (<sub [:connector]))]
    [:div {:class "location"
           :on-mouse-up (when connecting? #(>evt [:end-connecting-locations id]))
           :style {:width (str config/line-width "px")}}
     [:div {:class "location__meta"}
      [:p {:class "id"} (str "#" id)]
      [:p {:class "name"} display-name]
      [:ul {:class "actions"
            :on-mouse-down stop-e!}
       [:li {:class "action"
             :on-click #(>evt [:delete-location id])}
        [icon "trash"]]
       [:li {:class "action"
              :on-click #(>evt [:open-location-modal id])}
        [icon "edit"]]
       [:li {:class "action action_connect"
              :on-mouse-down (e-> #(when (left-button? %)
                                     (>evt [:start-connecting-locations id (e->graph-pointer %)])))}
        [icon "link"]]]]
     [:ul {:class "location__characters"}
      (for [dialogue dialogues]
        [:li {:key (str "location-dialogue-" id " - " (:id dialogue))}
         [:a {:style {:background-color (:character-color dialogue)}
              :on-mouse-down stop-e!
              :on-click #(>evt [:show-page "Dialogue" (:id dialogue)])}
          (:character-name dialogue)]])]]))


(defn location-connection [start end]
  [connection {:start (translate-position start [(/ config/line-width 2) 15])
               :end (translate-position end [(/ config/line-width 2) 15])}])

(defn location-management []
  (let [{:keys [locations connections]} (<sub [:location-map])
        get-pos #(get-in locations [% :position])]
    [:div {:class "full-page"}
     [:div {:class "new-item-button"}
      [slds/add-button "New" #(>evt [:create-location])]]
     [drag-canvas {:kind "location"
                   :items locations
                   :item-component location-component}
      [:svg {:class "graph__connection-container" :version "1.1"
             :baseProfile "full"
             :xmlns "http://www.w3.org/2000/svg"}
       (when-let [connector (<sub [:connector])]
         [connection connector])
       (for [[start end] connections]
         ^{:key (str "location-connection" start "->" end)}
         [location-connection (get-pos start) (get-pos end)])
       ]]]))

(defn game-canvas [game-data]
  (let [game-data (<sub [:game-data])
        canvas-ref (atom nil)
        game-input (atom nil)
        key-listener (fn [e]
                       (when-let [action (case (.-code e)
                                           ("ArrowUp" "KeyW" "KeyK") (put! @game-input [:move :up])
                                           ("ArrowRight" "KeyD" "KeyL") (put! @game-input [:move :right])
                                           ("ArrowDown" "KeyS" "KeyJ") (put! @game-input [:move :down])
                                           ("ArrowLeft" "KeyA" "KeyH") (put! @game-input [:move :left])
                                           "Space" (put! @game-input [:interact])
                                           nil)]
                         (.preventDefault e)))]
    (r/create-class
      {:component-did-mount (fn []
                              (reset! game-input (start-game (.getContext @canvas-ref "2d") game-data))
                              (.addEventListener js/document "keydown" key-listener))
       :component-will-unmount (fn []
                                 (.removeEventListener js/document "keydown" key-listener)
                                 (end-game))
       :reagent-render (fn []
                         [:div {:id "game-container"}
                          [:canvas {:id "game-canvas"
                                    :on-mouse-move #(let [c (relative-pointer % @canvas-ref)]
                                                      (put! @game-input [:cursor-position c]))
                                    :on-mouse-out #(put! @game-input [:cursor-position nil])
                                    :on-click #(let [c (relative-pointer % @canvas-ref)]
                                                 (put! @game-input [:animate c]))
                                    :height 450
                                    :width 800
                                    :ref (fn [el] (reset! canvas-ref el))}]])})))

(defn root []
  (let [{page-name :name page-payload :payload} (<sub [:current-page])
        pages (array-map
                "Game" [game-canvas]
                "Locations" [location-management]
                "Dialogue" [dialogue-component page-payload]
                "Characters" [character-management])
        link-map (map
                   (fn [name] [name #(>evt [:show-page name])])
                   (keys pages))]
    [:div {:id "page"}
     [npc-line-form-modal]
     [player-line-form-modal]
     [character-form-modal]
     [location-form-modal]
     [:a {:id "reset"
          :on-click #(>evt [:reset-db])} "reset"]
     [:div {:id "navigation"}
      [slds/global-navigation link-map page-name]]
     [:div {:id "content"}
      (get pages page-name [:div "Nothing"])]]))
