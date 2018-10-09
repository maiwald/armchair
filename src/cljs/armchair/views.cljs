(ns armchair.views
  (:require [re-frame.core :as re-frame]
            [clojure.spec.alpha :as s]
            [reagent.core :as r]
            [armchair.game :refer [start-game end-game]]
            [armchair.views.location-editor :refer [location-editor]]
            [armchair.slds :as slds]
            [armchair.util :refer [translate-point]]
            [armchair.config :as config]
            [armchair.routes :refer [routes >navigate]]
            [armchair.textures :refer [character-textures texture-path]]
            [bidi.bidi :refer [match-route]]
            [clojure.core.async :refer [put!]]))

;; Helpers

(def <sub (comp deref re-frame.core/subscribe))
(def >evt re-frame.core/dispatch)

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

(defn relative-cursor [e elem]
  (let [rect (.getBoundingClientRect elem)]
    [(- (.-clientX e) (.-left rect))
     (- (.-clientY e) (.-top rect))]))

(defn e->graph-cursor [e]
  (relative-cursor e (-> js/document
                         (.getElementsByClassName "graph")
                         (aget 0))))

(def left-button? #(zero? (.-button %)))


;; Drag & Drop

(defn start-dragging-handler [ids]
  (e-> #(when (left-button? %)
          (>evt [:start-dragging ids (e->graph-cursor %)]))))

(defn connection [{kind :kind [x1 y1] :start [x2 y2] :end}]
  [:line {:class ["graph__connection"
                  (when (= kind :connector) "graph__connection_is-connector")]
          :x1 x1
          :y1 y1
          :x2 x2
          :y2 y2}])

(defn drag-item [item-id component]
  (let [position (<sub [:ui/position item-id])
        dragging? (<sub [:dragging-item? item-id])
        start-dragging (start-dragging-handler #{item-id})
        stop-dragging (when dragging? (e-> #(>evt [:end-dragging])))]
    [:div {:class ["graph__item"
                   (when dragging? "graph__item_is-dragging")]
           :on-mouse-down start-dragging
           :on-mouse-up stop-dragging
           :style {:left (first position)
                   :top (second position)}}
     [component item-id]]))

(defn drag-canvas [{:keys [item-ids kind item-component]} & connection-children]
  (let [connecting? (some? (<sub [:connector]))
        dragging? (<sub [:dragging?])
        mouse-down (start-dragging-handler (set item-ids))
        mouse-move (e-> #(when (or dragging? connecting?)
                           (>evt [:move-cursor (e->graph-cursor %)])))
        mouse-up (cond
                   connecting? (e-> #(>evt [:abort-connecting]))
                   dragging? (e-> #(>evt [:end-dragging])))]
    [:div {:class (cond-> ["graph"]
                    dragging? (conj "graph_is-dragging")
                    connecting? (conj "graph_is-connecting"))
           :on-mouse-down mouse-down
           :on-mouse-move mouse-move
           :on-mouse-up mouse-up}
     (into [:div] connection-children)
     (for [id item-ids]
       ^{:key (str kind id)} [drag-item id item-component])]))

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
                                    (>evt [:start-connecting-lines id (e->graph-cursor %)])))}
       [icon "project-diagram" "Connect"]]]]))

(defn player-line-component [{:keys [id options]}]
  (let [connecting? (some? (<sub [:connector]))]
    [:div {:class "line"
           :on-mouse-up (when connecting? #(>evt [:end-connecting-lines id]))
           :style {:width (str config/line-width "px")}}
     [:div {:class "line__header"}
      [:p {:class "name"} "Player"]
      [:ul {:class "actions" :on-mouse-down stop-e!}
       [:li {:class "action" :on-click #(when (js/confirm "Are your sure you want to delete this line?")
                                          (>evt [:delete-line id]))}
        [icon "trash" "Delete"]]
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
                                                    (>evt [:start-connecting-lines id (e->graph-cursor %) index])))}
                       [icon "project-diagram" "Connect"]]])
                   options)]]))

(defn line-component [line-id]
  (let [line (<sub [:dialogue/line line-id])]
    (case (:kind line)
      :npc [npc-line-component line]
      :player [player-line-component line])))

(defn npc-connection [start end]
  (let [start-pos (<sub [:ui/position start])
        end-pos (<sub [:ui/position end])]
    [connection {:start (translate-point start-pos [(- config/line-width 15)
                                                    (+ 33 (/ config/line-height 2))])
                 :end (translate-point end-pos [15 (+ 33 (/ config/line-height 2))])}]))

(defn player-connection [start index end]
  (let [start-pos (<sub [:ui/position start])
        end-pos (<sub [:ui/position end])]
    [connection {:start (translate-point start-pos [(- config/line-width 15)
                                                    (+ 33
                                                       (/ config/line-height 2)
                                                       (* index config/line-height))])
                 :end (translate-point end-pos [15 (+ 33 (/ config/line-height 2))])}]))

(defn dialogue-editor [dialogue-id]
  (if-let [{:keys [line-ids npc-connections player-connections]} (<sub [:dialogue dialogue-id])]
    [:div {:class "full-page"}
     [:div {:class "new-item-button"}
      [slds/add-button "New Player Line" #(>evt [:create-player-line dialogue-id])]
      [slds/add-button "New NPC Line" #(>evt [:create-npc-line dialogue-id])]]
     [drag-canvas {:kind "line"
                    :item-ids line-ids
                    :item-component line-component}
      [:svg {:class "graph__connection-container" :version "1.1"
             :baseProfile "full"
             :xmlns "http://www.w3.org/2000/svg"}
       (when-let [connector (<sub [:connector])]
         [connection connector])
       (for [[start end] npc-connections]
         ^{:key (str "line-connection:" start "->" end)}
         [npc-connection start end])
       (for [[start index end] player-connections]
         ^{:key (str "response-connection:" start ":" index "->" end)}
         [player-connection start index end])]]]
    [:span "No dialogue selected!"]))

(defn dialogue-management []
  (let [dialogues (<sub [:dialogue-list])]
    [slds/resource-page "Dialogues"
     {:columns [:location :texture :character :description :actions]
      :collection (vals dialogues)
      :cell-views {:character (fn [{:keys [id display-name]}]
                                [:a {:on-click #(>evt [:open-character-modal id])}
                                 display-name])
                   :texture (fn [texture]
                              [:img {:src (texture-path texture)}])
                   :location (fn [{:keys [id display-name]}]
                               [:a {:on-click #(>navigate :location-edit :id id)}
                                display-name])
                   :actions (fn [_ {id :id}]
                              [:div {:class "slds-text-align_right"}
                               [slds/symbol-button "trash-alt" {:on-click #(when (js/confirm "Are you sure you want to delete this dialogue?")
                                                                             (>evt [:delete-dialogue id]))}]
                               [slds/symbol-button "edit" {:on-click #(>navigate :dialogue-edit :id id)}]])}
      :new-resource #(>evt [:open-dialogue-creation-modal])}]))

(defn character-management []
  (let [characters (<sub [:character-list])]
    [slds/resource-page "Characters"
     {:columns [:texture :display-name :color :line-count :actions]
      :collection (vals characters)
      :cell-views {:color (fn [color] [slds/badge color color])
                   :texture (fn [texture]
                              [:img {:src (texture-path texture)}])
                   :actions (fn [_ {:keys [id line-count]}]
                              [:div {:class "slds-text-align_right"}
                               (when (zero? line-count)
                                 [slds/symbol-button "trash-alt" {:on-click #(when (js/confirm "Are you sure you want to delete this character?")
                                                                               (>evt [:delete-character id]))}])
                               [slds/symbol-button "edit" {:on-click #(>evt [:open-character-modal id])}]])}
      :new-resource #(>evt [:create-character])}]))

(defn info-management []
  (let [infos (<sub [:info-list])]
    [slds/resource-page "Infos"
     {:columns [:description :actions]
      :collection (vals infos)
      :cell-views {:actions (fn [_ {id :id}]
                              [:div {:class "slds-text-align_right"}
                               [slds/symbol-button "trash-alt" {:on-click #(when (js/confirm "Are you sure you want to delete this info?")
                                                                             (>evt [:delete-info id]))}]
                               [slds/symbol-button "edit" {:on-click #(>evt [:open-info-modal id])}]])}
      :new-resource #(>evt [:create-info])}]))

(defn location-component [location-id]
  (let [{:keys [display-name dialogues]} (<sub [:location-map/location location-id])
        connecting? (some? (<sub [:connector]))]
    [:div {:class "location"
           :on-mouse-up (when connecting? #(>evt [:end-connecting-locations location-id]))
           :style {:width (str config/line-width "px")}}
     [:div {:class "location__header"}
      [:p {:class "name"} display-name]
      [:ul {:class "actions"
            :on-mouse-down stop-e!}
       [:li {:class "action"
             :on-click #(when (js/confirm "Are you sure you want to delete this location?")
                          (>evt [:delete-location location-id]))}
        [icon "trash" "Delete"]]
       [:li {:class "action"
             :on-click #(>navigate :location-edit :id location-id)}
        [icon "edit" "Edit"]]
       [:li {:class "action action_connect"
             :on-mouse-down (e-> #(when (left-button? %)
                                    (>evt [:start-connecting-locations location-id (e->graph-cursor %)])))}
        [icon "project-diagram" "Connect"]]]]
     [:ul {:class "location__characters"}
      (for [{:keys [dialogue-id npc-name npc-color]} dialogues]
        [:li {:key (str "location-dialogue-" location-id " - " dialogue-id)}
         [:a {:style {:background-color npc-color}
              :on-mouse-down stop-e!
              :on-click #(>navigate :dialogue-edit :id dialogue-id)}
          npc-name]])]]))

(defn location-connection [start end]
  (let [start-pos (<sub [:ui/position start])
        end-pos (<sub [:ui/position end])]
    [connection {:start (translate-point start-pos [(/ config/line-width 2) 15])
                 :end (translate-point end-pos [(/ config/line-width 2) 15])}]))

(defn location-management []
  (let [{:keys [location-ids connections]} (<sub [:location-map])]
    [:div {:class "full-page"}
     [:div {:class "new-item-button"}
      [slds/add-button "New" #(>evt [:location/create])]]
     [drag-canvas {:kind "location"
                    :item-ids location-ids
                    :item-component location-component}
      [:svg {:class "graph__connection-container" :version "1.1"
             :baseProfile "full"
             :xmlns "http://www.w3.org/2000/svg"}
       (when-let [connector (<sub [:connector])]
         [connection connector])
       (for [[start end] connections]
         ^{:key (str "location-connection" start "->" end)}
         [location-connection start end])]]]))

;; Game canvas

(defn game-canvas [game-data]
  (let [game-data (<sub [:game-data])
        background-canvas (atom nil)
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
                              (.getContext @background-canvas "2d")
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
                     :ref (fn [el] (reset! background-canvas el))}]
           [:canvas {:on-mouse-move #(let [c (relative-cursor % @entity-canvas)]
                                       (put! @game-input [:cursor-position c]))
                     :on-mouse-out #(put! @game-input [:cursor-position nil])
                     :on-click #(let [c (relative-cursor % @entity-canvas)]
                                  (put! @game-input [:animate c]))
                     :height 450
                     :width 800
                     :ref (fn [el] (reset! entity-canvas el))}]]])})))

;; Modals

(defn dialogue-creation-modal [{:keys [character-id description]}]
  [slds/modal {:title "Create Dialogue"
               :confirm-handler #(>evt [:create-dialogue])
               :close-handler #(>evt [:close-modal])}
   [slds/form
    [slds/input-select {:label "Character"
                        :on-change #(>evt [:dialogue-creation-update :character-id (uuid (e->val %))])
                        :options (<sub [:character-options])
                        :value character-id}]
    [slds/input-textarea {:label "Description"
                          :on-change #(>evt [:dialogue-creation-update :description (e->val %)])
                          :value description}]]])

(defn npc-line-form-modal [line-id]
  (let [line (<sub [:dialogue/modal-line line-id])]
    [slds/modal {:title (str "Line #" line-id)
                 :close-handler #(>evt [:close-modal])}
     [slds/form
      [slds/input-select {:label "Character"
                          :disabled (:initial-line? line)
                          :on-change #(>evt [:update-line line-id :character-id (uuid (e->val %))])
                          :options (<sub [:character-options])
                          :value (:character-id line)}]
      [slds/input-textarea {:label "Text"
                            :on-change #(>evt [:update-line line-id :text (e->val %)])
                            :value (:text line)}]
      [slds/multi-select {:label "Infos"
                          :options (clj->js (<sub [:info-options]))
                          :values (:info-ids line)
                          :on-change #(>evt [:set-infos line-id (map uuid %)])}]]]))

(defn player-line-form-modal-option [line-id index total-count]
  (let [{:keys [text required-info-ids]} (<sub [:dialogue/player-line-option line-id index])
        info-options (<sub [:info-options])]
    [:div { :class "player-line-form__response"}
     [:div {:class "text"}
      [slds/input-textarea {:label (str "Response " (inc index))
                            :on-change #(>evt [:update-option line-id index (e->val %)])
                            :value text}]
      [slds/multi-select {:label "Required Infos"
                          :options info-options
                          :values required-info-ids
                          :on-change #(>evt [:set-required-info line-id index (map uuid %)])}]]
     [:ul {:class "actions actions_vertial"}
      [:li {:class "action" :on-click #(when (js/confirm "Are you sure you want to delete this option?")
                                         (>evt [:delete-option line-id index]))}
       [icon "trash" "Delete"]]
      (when-not (= index 0)
        [:li {:class "action" :on-click #(>evt [:move-option line-id index :up])}
         [icon "arrow-up" "Move up"]])
      (when-not (= index (dec total-count))
        [:li {:class "action" :on-click #(>evt [:move-option line-id index :down])}
         [icon "arrow-down" "Move down"]])]]))

(defn player-line-form-modal [line-id]
  (let [{:keys [option-count]} (<sub [:dialogue/modal-line line-id])]
    [slds/modal {:title (str "Line #" line-id)
                 :close-handler #(>evt [:close-modal])}
     [:div {:class "player-line-form"}
      [slds/form
       (for [index (range option-count)]
         ^{:key (str "option:" line-id ":" index)}
         [player-line-form-modal-option line-id index option-count])
       [slds/add-button "New Option" #(>evt [:add-option line-id])]]]]))

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
                        :value (:color character)}]
      [slds/input-select {:label "Avatar"
                          :options (mapv #(vector % %) character-textures)
                          :value (:texture character)
                          :on-change #(>evt [:update-character character-id :texture (keyword (e->val %))])}]]]))

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
      :dialogue-creation [dialogue-creation-modal (:dialogue-creation modal)]
      :npc-line-id       [npc-line-form-modal (:npc-line-id modal)]
      :player-line-id    [player-line-form-modal (:player-line-id modal)]
      :character-id      [character-form-modal (:character-id modal)]
      :info-id           [info-form-modal (:info-id modal)])))

;; Root

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
                                                 :dialogues "Dialogues"
                                                 :characters "Characters"
                                                 :infos "Infos")
                               :current-page page-name
                               :click-handler #(>navigate %)}]]
     [:div {:id "content"}
      (case page-name
        :game          [game-canvas]
        :locations     [location-management]
        :location-edit [location-editor (uuid (:id page-params))]
        :dialogues     [dialogue-management]
        :dialogue-edit [dialogue-editor (uuid (:id page-params))]
        :characters    [character-management]
        :infos         [info-management]
        [:div "Page not found"])]]))
