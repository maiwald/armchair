(ns armchair.views
  (:require [re-frame.core :as re-frame]
            [clojure.spec.alpha :as s]
            [reagent.core :as r]
            [armchair.game :refer [start-game end-game]]
            [armchair.slds :as slds]
            [armchair.util :refer [translate-position]]
            [armchair.config :as config]
            [armchair.routes :refer [routes]]
            [armchair.textures :refer [background-textures texture-path]]
            [bidi.bidi :refer [match-route path-for]]
            [clojure.core.async :refer [put!]]))

;; Helpers

(def <sub (comp deref re-frame.core/subscribe))
(def >evt re-frame.core/dispatch)

(defn once [f]
  (let [called (atom false)]
    (fn [& args]
      (when-not @called
        (reset! called true)
        (apply f args)))))

(defn stop-e! [e]
  (.preventDefault e)
  (.stopPropagation e)
  e)

(defn e-> [handler]
  (comp handler stop-e!))

(defn e->val [e]
  (let [target (.-target e)]
    (case (.-type target)
      "checkbox" (.-checked target)
      (.-value target))))

(defn relative-pointer [e elem]
  (let [rect (.getBoundingClientRect elem)]
    [(- (.-clientX e) (.-left rect))
     (- (.-clientY e) (.-top rect))]))

(defn e->graph-pointer [e]
  (relative-pointer e (-> js/document
                          (.getElementsByClassName "graph")
                          (aget 0))))

(def left-button? #(zero? (.-button %)))

;; Navigation / History

(set! (.-onpopstate js/window)
      (fn [e] (>evt [:show-page (subs js/location.hash 1)])))

(defn >navigate [& args]
  (let [url (apply path-for (into [routes] args))]
    (js/history.pushState #js{} "" (str "#" url))
    (>evt [:show-page url])))

;; Drag & Drop

(defn start-dragging-handler [position-ids]
  (e-> #(when (left-button? %)
          (>evt [:start-dragging position-ids (e->graph-pointer %)]))))

(defn connection [{:keys [kind start end]}]
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

(defn icon [glyph title]
  [:i {:class (str "fas fa-" glyph)
       :title title}])

(defn npc-line-component [{:keys [id info-ids initial-line? text character-name character-color]}]
  (let [connecting? (some? (<sub [:connector]))]
    [:div {:class "line"
           :on-mouse-up (when connecting? #(>evt [:end-connecting-lines id]))
           :style {:border-color character-color
                   :width (str config/line-width "px")}}
     [:header {:class "line__header"}
      [:p {:class "id"} (str "#" id)]
      [:p {:class "name"} character-name]
      [:ul {:class "states"}
       (when initial-line?
         [:li {:class "state"} [icon "play-circle" "This is the initial line of this dialogue"]])
       (when-not (empty? info-ids)
         [:li {:class "state"} [icon "info-circle" "This line contains infos."]])]
      [:ul {:class "actions" :on-mouse-down stop-e!}
       (when-not initial-line?
         [:li {:class "action" :on-click #(when (js/confirm "Are your sure you want to delete this line?")
                                            (>evt [:delete-line id]))}
          [icon "trash" "Delete"]])
       [:li {:class "action" :on-click #(>evt [:open-npc-line-modal id])}
        [icon "edit" "Edit"]]]]
     [:div {:class "line__text"
            :style {:height (str config/line-height "px")}}
      [:p text]
      [:div {:class "action action_connect"
             :on-mouse-down (e-> #(when (left-button? %)
                                    (>evt [:start-connecting-lines id (e->graph-pointer %)])))}
       [icon "project-diagram" "Connect"]]]]))

(defn player-line-component [{:keys [id initial-line? options]}]
  (let [connecting? (some? (<sub [:connector]))]
    [:div {:class "line"
           :on-mouse-up (when connecting? #(>evt [:end-connecting-lines id]))
           :style {:width (str config/line-width "px")}}
     [:div {:class "line__header"}
      [:p {:class "id"} (str "#" id)]
      [:p {:class "name"} "Player"]
      [:ul {:class "actions" :on-mouse-down stop-e!}
       (when-not initial-line?
         [:li {:class "action" :on-click #(when (js/confirm "Are your sure you want to delete this line?")
                                            (>evt [:delete-line id]))}
          [icon "trash" "Delete"]])
       [:li {:class "action" :on-click #(>evt [:open-player-line-modal id])}
        [icon "edit" "Edit"]]]]
     [:ul {:class "line__options"}
      (map-indexed (fn [index option]
                     [:li {:key (str "line-option" id ":" index)
                           :class "line__text"
                           :style {:height (str config/line-height "px")}}
                      [:p
                       (when-not (empty? (:required-info-ids option))
                         [:span {:class "state"}
                          [icon "lock" "This option requires information."]])
                       (:text option)]
                      [:div {:class "action action_connect"
                             :on-mouse-down (e-> #(when (left-button? %)
                                                    (>evt [:start-connecting-lines id (e->graph-pointer %) index])))}
                       [icon "project-diagram" "Connect"]]])
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
  (if-let [{:keys [lines npc-connections player-connections]} (<sub [:dialogue dialogue-id])]
    (letfn [(get-pos [line-id] get-pos (get-in lines [line-id :position]))]
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

(defn character-management []
  (let [characters (<sub [:character-list])]
    [slds/resource-page "Characters"
     {:columns [:id :display-name :color :lines :actions]
      :collection (vals characters)
      :cell-views {:color slds/color-cell
                   :actions (fn [{:keys [id lines]} _]
                              [:div {:class "slds-text-align_right"}
                               (when (zero? lines)
                                 [slds/symbol-button "trash-alt" {:on-click #(when (js/confirm "Are you sure you want to delete this character?")
                                                                               (>evt [:delete-character id]))}])
                               [slds/symbol-button "edit" {:on-click #(>evt [:open-character-modal id])}]])}
      :new-resource #(>evt [:create-character])}]))

(defn info-management []
  (let [infos (<sub [:info-list])]
    [slds/resource-page "Infos"
     {:columns [:id :description :actions]
      :collection (vals infos)
      :cell-views {:actions (fn [{id :id}]
                              [:div {:class "slds-text-align_right"}
                               [slds/symbol-button "trash-alt" {:on-click #(when (js/confirm "Are you sure you want to delete this info?")
                                                                             (>evt [:delete-info id]))}]
                               [slds/symbol-button "edit" {:on-click #(>evt [:open-info-modal id])}]])}
      :new-resource #(>evt [:create-info])}]))

(defn location-component [{:keys [id display-name dialogues] :as location}]
  (let [connecting? (some? (<sub [:connector]))]
    [:div {:class "location"
           :on-mouse-up (when connecting? #(>evt [:end-connecting-locations id]))
           :style {:width (str config/line-width "px")}}
     [:div {:class "location__header"}
      [:p {:class "id"} (str "#" id)]
      [:p {:class "name"} display-name]
      [:ul {:class "actions"
            :on-mouse-down stop-e!}
       [:li {:class "action"
             :on-click #(when (js/confirm "Are you sure you want to delete this location?")
                          (>evt [:delete-location id]))}
        [icon "trash" "Delete"]]
       [:li {:class "action"
             :on-click #(>navigate :location-edit :id id)}
        [icon "edit" "Edit"]]
       [:li {:class "action action_connect"
             :on-mouse-down (e-> #(when (left-button? %)
                                    (>evt [:start-connecting-locations id (e->graph-pointer %)])))}
        [icon "project-diagram" "Connect"]]]]
     [:ul {:class "location__characters"}
      (for [dialogue dialogues]
        [:li {:key (str "location-dialogue-" id " - " (:id dialogue))}
         [:a {:style {:background-color (:character-color dialogue)}
              :on-mouse-down stop-e!
              :on-click #(>navigate :dialogue :id (:id dialogue))}
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
         [location-connection (get-pos start) (get-pos end)])]]]))

(set! (.-ondragend js/document)
      (fn [] (>evt [:stop-entity-drag])))

(defn start-entity-drag-handler [{:keys [id texture]}]
  (fn [e]
    (let [image (js/Image.)]
      (set! (.-src image) (texture-path texture))
      (-> e .-dataTransfer (.setDragImage image
                                          (/ config/tile-size 2)
                                          (/ config/tile-size 2))))
    (>evt [:start-entity-drag {:entity id}])))

(defn location-editor-sidebar [{:keys [tool active-texture]} {:keys [id display-name]}]
  (letfn [(update-display-name [e]
            (>evt [:update-location id :display-name (e->val e)]))]
    [slds/form
     [slds/input-text {:label "Name"
                       :on-change update-display-name
                       :value display-name}]
     [slds/checkbox-select {:label "Layers"
                            :options [[:background "Background" :disabled]
                                      [:collision "Collision"]
                                      [:npcs "NPCs"]
                                      [:triggers "Triggers"]]
                            :values #{:background :npcs}
                            :on-change #(js/console.log "selected" %)}]
     [slds/radio-button-group {:label "Tools"
                               :options [[:paint "Background"]
                                         [:select "NPCs"]]
                               :active tool
                               :on-change #(>evt [:set-tool %])}]
     (case tool
       :paint
       [slds/label "Background Textures"
        [:ul {:class "tile-grid"}
         (for [texture background-textures]
           [:li {:key (str "texture-select:" texture)
                 :class ["tile-grid__item"
                         (when (= texture active-texture) "tile-grid__item-active")]}
            [:a {:on-click #(>evt [:set-active-texture texture])}
             [:img {:src (texture-path texture)}]]])]]

       :select
       [slds/label "Available NPCs"
        [:ul {:class "tile-list"}
         (for [[_ {:keys [display-name texture] :as character}] (<sub [:character-list])]
           [:li {:key (str "character-select" display-name)
                 :class "tile-list__item"
                 :draggable true
                 :on-drag-start (start-entity-drag-handler character)}
            [:img {:title display-name :src (texture-path texture)}]
            [:span display-name]])]])]))

(defn location-editor-content [{:keys [highlight tool painting?]} {:keys [id level enemies]}]
  (let [level-width (count level)
        level-height (count (first level))
        dnd-payload (<sub [:dnd-payload])
        character-list (<sub [:character-list])]
    [:div {:class "level"
           :on-mouse-leave #(>evt [:unset-highlight])
           :style {:width (str (* config/tile-size level-width) "px")
                   :height (str (* config/tile-size level-height) "px")}}
     (for [x (range level-width)
           y (range level-height)]
       (let [texture-name (case (get-in level [x y])
                            0 "wall"
                            1 "grass")]
         [:div (merge {:key (str "location" id ":" x ":" y)
                       :class "level__cell"
                       :style {:width (str config/tile-size "px")
                               :height (str config/tile-size "px")}}
                      (case tool
                        :select
                        (when-let [entity (:entity dnd-payload)]
                          {:on-drag-over (e-> (once #(>evt [:set-highlight x y])))
                           :on-drop #(>evt [:move-entity id entity [x y]])})
                        :paint
                        {:on-mouse-down (e-> #(>evt [:start-painting id x y]))
                         :on-mouse-over (e-> #(when painting? (>evt [:paint id x y])))
                         :on-mouse-up (e-> #(when painting? (>evt [:stop-painting])))}))
          [:img {:class "no-drag"
                 :src (texture-path texture-name)}]
          (when-let [character-id (get enemies [x y])]
            (let [character (get character-list character-id)]
              [:img {:src (texture-path (:texture character))
                     :draggable true
                     :on-drag-start (start-entity-drag-handler character)}]))
          (when (= [x y] highlight)
            [:div {:class "highlight no-drag"}])]))]))

(defn location-editor [location-id]
  (let [location (<sub [:location location-id])
        editor-options (<sub [:location-editor-data])]
    [:div {:class "location-editor"}
     [:div {:class "location-editor__sidebar"}
      [location-editor-sidebar editor-options location]]
     [:div {:class "location-editor__content"}
      [location-editor-content editor-options location]]]))

(defn game-canvas [game-data]
  (let [game-data (<sub [:game-data])
        level-canvas (atom nil)
        entity-canvas (atom nil)
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
      {:display-name "game-canvas"
       :component-did-mount
       (fn []
         (reset! game-input (start-game
                              (.getContext @level-canvas "2d")
                              (.getContext @entity-canvas "2d")
                              game-data))
         (.addEventListener js/document "keydown" key-listener))

       :component-will-unmount
       (fn []
         (.removeEventListener js/document "keydown" key-listener)
         (end-game))

       :reagent-render
       (fn []
         [:div {:id "game"}
          [:div {:class "canvas-container"
                 :style {:width (str 800 "px")}}
           [:canvas {:height 450
                     :width 800
                     :ref (fn [el] (reset! level-canvas el))}]
           [:canvas {:on-mouse-move #(let [c (relative-pointer % @entity-canvas)]
                                       (put! @game-input [:cursor-position c]))
                     :on-mouse-out #(put! @game-input [:cursor-position nil])
                     :on-click #(let [c (relative-pointer % @entity-canvas)]
                                  (put! @game-input [:animate c]))
                     :height 450
                     :width 800
                     :ref (fn [el] (reset! entity-canvas el))}]]])})))

;; Modals

(defn npc-line-form-modal [line-id]
  (let [line (<sub [:line line-id])
        update-handler (fn [field] #(>evt [:update-line line-id field (e->val %)]))]
    [slds/modal {:title (str "Line #" line-id)
                 :close-handler #(>evt [:close-modal])}
     [slds/form
      [slds/input-select {:label "Character"
                          :on-change (update-handler :character-id)
                          :options (<sub [:character-options])
                          :value (:character-id line)}]
      [slds/input-textarea {:label "Text"
                            :on-change (update-handler :text)
                            :value (:text line)}]
      [slds/multi-select {:label "Infos"
                          :options (clj->js (<sub [:info-options]))
                          :values (:info-ids line)
                          :on-change #(>evt [:set-infos line-id %])}]]]))

(defn player-line-form-modal [line-id]
  (let [line (<sub [:line line-id])
        info-options (<sub [:info-options])]
    [slds/modal {:title (str "Line #" line-id)
                 :close-handler #(>evt [:close-modal])}
     [:div {:class "player-line-form"}
      [slds/form
       (map-indexed
         (fn [index option]
           [:div {:key (str "option:" line-id ":" index)
                  :class "player-line-form__response"}
            [:div {:class "text"}
             [slds/input-textarea {:label (str "Response " (inc index))
                                   :on-change #(>evt [:update-option line-id index (e->val %)])
                                   :value (:text option)}]
             [slds/multi-select {:label "Required Infos"
                                 :options info-options
                                 :values (:required-info-ids option)
                                 :on-change #(>evt [:set-required-info line-id index %])}]]
            [:ul {:class "actions actions_vertial"}
             [:li {:class "action" :on-click #(when (js/confirm "Are you sure you want to delete this option?")
                                                (>evt [:delete-option line-id index]))}
              [icon "trash" "Delete"]]
             (when-not (= index 0)
               [:li {:class "action" :on-click #(>evt [:move-option line-id index :up])}
                [icon "arrow-up" "Move up"]])
             (when-not (= index (dec (count (:options line))))
               [:li {:class "action" :on-click #(>evt [:move-option line-id index :down])}
                [icon "arrow-down" "Move down"]])]])
         (:options line))]
      [slds/add-button "New Option" #(>evt [:add-option line-id])]]]))

(defn character-form-modal [character-id]
  (let [character (<sub [:character character-id])
        update-handler (fn [field] #(>evt [:update-character character-id field (e->val %)]))]
    [slds/modal {:title (:display-name character)
                 :close-handler #(>evt [:close-modal])}
     [slds/form
      [slds/input-text {:label "Name"
                        :on-change (update-handler :display-name)
                        :value (:display-name character)}]
      [slds/input-text {:label "Color"
                        :on-change (update-handler :color)
                        :value (:color character)}]]]))

(defn info-form-modal [info-id]
  (let [info (<sub [:info info-id])
        update-description #(>evt [:update-info info-id (e->val %)])]
    [slds/modal {:title (:description info)
                 :close-handler #(>evt [:close-modal])}
     [slds/form
      [slds/input-text {:label "Description"
                        :on-change update-description
                        :value (:description info)}]]]))

(defn modal []
  (if-let [modal (<sub [:modal])]
    (condp #(contains? %2 %1) modal
      :npc-line-id    [npc-line-form-modal (:npc-line-id modal)]
      :player-line-id [player-line-form-modal (:player-line-id modal)]
      :character-id   [character-form-modal (:character-id modal)]
      :info-id        [info-form-modal (:info-id modal)])))

(defn root []
  (let [{page-name :handler
         page-params :route-params} (match-route routes (<sub [:current-page]))]
    [:div {:id "page"}
     [modal]
     [:a {:id "reset"
          :on-click #(>evt [:reset-db])} "reset"]
     [:div {:id "navigation"}
      [slds/global-navigation {:links (array-map :game "Game"
                                                 :locations "Locations"
                                                 :characters "Characters"
                                                 :infos "Infos")
                               :current-page page-name
                               :click-handler #(>navigate %)}]]
     [:div {:id "content"}
      (case page-name
        :game          [game-canvas]
        :locations     [location-management]
        :location-edit [location-editor (int (:id page-params))]
        :dialogue      [dialogue-component (int (:id page-params))]
        :characters    [character-management]
        :infos         [info-management]
        [:div "Page not found"])]]))
