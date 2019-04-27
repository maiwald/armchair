(ns armchair.location-editor.views
  (:require [reagent.core :as r]
            [armchair.slds :as slds]
            [armchair.input :as input]
            [armchair.components :as c]
            [armchair.config :as config]
            [armchair.routes :refer [>navigate]]
            [armchair.util :as u :refer [px <sub >evt e-> e->val]]
            [armchair.textures :refer [texture-path background-textures]]))

(defn icon [glyph title]
  [:i {:class (str "fas fa-" glyph)
       :title title}])

(defn dnd-texture [texture]
  [:div.dnd-texture
   [:img {:src (texture-path texture)
          :style {:height (str config/tile-size "px")
                  :width (str config/tile-size "px")
                  :max-width (str config/tile-size "px")
                  :max-height (str config/tile-size "px")}}]])

(defn set-dnd-texture! [e]
  (let [offset (/ config/tile-size 2)
        image (.querySelector (.-currentTarget e) ".dnd-texture img")]
    (.setDragImage (.-dataTransfer e) image offset offset)))

(defn tile-paint-canvas [{:keys [on-paint]
                          [delta _] :dimension}]
  (let [painted-tiles (r/atom nil)
        current-tile (r/atom nil)]
    (letfn [(get-tile [e] (u/coord->tile (u/relative-cursor e (.-currentTarget e))))
            (set-current-tile [e] (reset! current-tile (get-tile e)))
            (clear-current-tile [] (reset! current-tile nil))
            (start-painting [] (reset! painted-tiles #{}))
            (stop-painting [] (reset! painted-tiles nil))
            (paint [e]
              (let [tile (u/translate-point (get-tile e) delta)]
                (when (and (some? @painted-tiles)
                           (not (contains? @painted-tiles tile)))
                  (swap! painted-tiles conj tile)
                  (on-paint tile))))]
      (fn []
        [:div {:class "level-layer"
               :on-mouse-enter set-current-tile
               :on-mouse-leave clear-current-tile
               :on-mouse-down (fn [e]
                                (when (u/e->left? e)
                                  (start-painting)
                                  (paint e)))
               :on-mouse-move (fn [e]
                                (set-current-tile e)
                                (paint e))
               :on-mouse-up stop-painting}
         (when-let [tile @current-tile]
           (let [[x y] (u/tile->coord tile)]
             [:div {:class "interactor interactor_paint"
                    :style {:height (px config/tile-size)
                            :width (px config/tile-size)
                            :top (px y)
                            :left (px x)}}]))]))))

(defn sidebar-widget [{title :title}]
  (into [:div {:class "location-editor__sidebar-widget"}
         [:div {:class "location-editor__sidebar-widget__title"} title]]
        (r/children (r/current-component))))

(defn sidebar-info [location-id]
  (let [display-name (<sub [:location-editor/display-name location-id])]
    [sidebar-widget {:title "Location Name"}
     [input/text
      {:on-change #(>evt [:location-editor/update-name location-id (e->val %)])
       :value display-name}]]))

(defn sidebar-resize [location-id]
  (let [{:keys [width height]} (<sub [:location-editor/dimensions location-id])]
    [sidebar-widget {:title (str "Size: " width "x" height)}
     [:div {:class "resize-container"}
      [:div {:class "resize-container__reference"}
       [:div {:class "resizer resizer_horizontal resizer_top"}
        [:a {:on-click #(>evt [:location-editor/resize-larger location-id :up])} [icon "arrow-up" "extend"]]
        [:a {:on-click #(>evt [:location-editor/resize-smaller location-id :up])} [icon "arrow-down" "shrink"]]]
       [:div {:class "resizer resizer_horizontal resizer_bottom"}
        [:a {:on-click #(>evt [:location-editor/resize-smaller location-id :down])} [icon "arrow-up" "shrink"]]
        [:a {:on-click #(>evt [:location-editor/resize-larger location-id :down])} [icon "arrow-down" "extend"]]]
       [:div {:class "resizer resizer_vertical resizer_left"}
        [:a {:on-click #(>evt [:location-editor/resize-larger location-id :left])} [icon "arrow-left" "extend"]]
        [:a {:on-click #(>evt [:location-editor/resize-smaller location-id :left])} [icon "arrow-right" "shrink"]]]
       [:div {:class "resizer resizer_vertical resizer_right"}
        [:a {:on-click #(>evt [:location-editor/resize-smaller location-id :right])} [icon "arrow-left" "shrink"]]
        [:a {:on-click #(>evt [:location-editor/resize-larger location-id :right])} [icon "arrow-right" "extend"]]]]]]))


(defn sidebar-paint [location-id]
  (let [{:keys [active-texture]} (<sub [:location-editor/ui])]
    [sidebar-widget {:title "Background Textures"}
     [:ul {:class "tile-grid"}
      (for [texture background-textures]
        [:li {:key (str "texture-select:" texture)
              :title texture
              :class ["tile-grid__item"
                      (when (= texture active-texture) "tile-grid__item_active")]}
         [:a {:on-click #(>evt [:location-editor/set-active-texture texture])}
          [c/sprite-texture texture]]])]]))

(defn sidebar-collision [location-id]
  (let [{:keys [active-walk-state]} (<sub [:location-editor/ui])]
    [sidebar-widget {:title "Collision State"}
     [:ul {:class "tile-grid"}
      (for [walk-state (list true false)]
        [:li {:key (str "walk-state-select:" walk-state)
              :title (if walk-state "walkable" "not walkable")
              :class ["tile-grid__item"
                      (when (= walk-state active-walk-state) "tile-grid__item_active")]
              :style {:background-color "#fff"}}
         [:a {:on-click #(>evt [:location-editor/set-active-walk-state walk-state])
              :style {:height (px config/tile-size)
                      :width (px config/tile-size)
                      :background-color (if walk-state
                                          "rgba(0, 255, 0, .4)"
                                          "rgba(255, 0, 0, .4)")}}]])]]))

(defn sidebar-player []
  [sidebar-widget {:title "Player"}
   [:ul {:class "tile-list"}
    [:li {:class "tile-list__item"
          :draggable true
          :on-drag-start (fn [e]
                           (set-dnd-texture! e)
                           (.setData (.-dataTransfer e) "text/plain" ":player")
                           (>evt [:location-editor/start-entity-drag :player]))}
     [dnd-texture :player]
     [:span {:class "tile-list__item__image"
             :style {:width (str config/tile-size "px")
                     :height (str config/tile-size "px")}}
      [c/sprite-texture :human "Player"]]
     [:span {:class "tile-list__item__label"} "Player"]]]])

(defn sidebar-npcs [location-id]
  (let [available-npcs (<sub [:location-editor/available-npcs location-id])
        dnd-character-id (:character-id (<sub [:dnd-payload]))]
    [sidebar-widget {:title "Available Characters"}
     [:ul {:class "tile-list"}
      (for [[character-id {:keys [display-name texture]}] available-npcs]
        [:li {:key (str "character-select" display-name)
              :class "tile-list__item"
              :draggable true
              :on-drag-start (fn [e]
                               (set-dnd-texture! e)
                               (.setData (.-dataTransfer e) "text/plain" display-name)
                               (>evt [:location-editor/start-entity-drag {:character-id character-id}]))}
         [dnd-texture texture]
         [:span {:class "tile-list__item__image"
                 :style {:width (str config/tile-size "px")
                         :height (str config/tile-size "px")}}
          [c/sprite-texture texture display-name]]
         [:span {:class "tile-list__item__label"} display-name]])
      (when (and (some? dnd-character-id)
                 (not (contains? available-npcs dnd-character-id)))
        [:li {:class "tile-list__item tile-list__item_dropzone"
              :on-drag-over u/prevent-e!
              :on-drop (e-> #(>evt [:location-editor/remove-character dnd-character-id]))}
         [:span {:class "tile-list__item__image"
                 :style {:width (str config/tile-size "px")
                         :height (str config/tile-size "px")}}
          [icon "trash" "Drop here to remove."]]
         [:span {:class "tile-list__item__label"} "Drop here to remove."]])]
     (if (empty? available-npcs) "All Characters are placed in locations.")
     [c/button {:title "Create Character"
                :icon "plus"
                :on-click #(>evt [:open-character-modal])}]]))

(defn sidebar-connections [location-id]
  [sidebar-widget {:title "Assigned Connections"}
   [:ul {:class "tile-list"}
    (for [[target-loctation-id {:keys [display-name]}] (<sub [:location-editor/connected-locations location-id])]
      [:li {:key (str "connection-select" display-name)
            :class "tile-list__item"
            :draggable true
            :on-drag-start (fn [e]
                             (set-dnd-texture! e)
                             (.setData (.-dataTransfer e) "text/plain" display-name)
                             (>evt [:location-editor/start-entity-drag {:connection-trigger target-loctation-id}]))}
       [dnd-texture :exit]
       [:img {:src (texture-path :exit)
              :title display-name}]
       [:span display-name]])]])

(defn sidebar [location-id]
  (let [{:keys [tool]} (<sub [:location-editor/ui])]
    [:div
     [c/tabs {:items [[:info "Info"]
                      [:background-painter "Background"]
                      [:collision "Collision"]
                      [:npcs-select "Entities"]]
              :active tool
              :on-change #(>evt [:location-editor/set-tool %])}]
     (case tool
       :info [:div
              [sidebar-info location-id]
              [sidebar-resize location-id]]
       :background-painter [sidebar-paint location-id]
       :collision [sidebar-collision]
       :npcs-select [:div
                     [sidebar-player]
                     [sidebar-npcs location-id]
                     [sidebar-connections location-id]]
       nil)]))

(defn tile-style [x y]
  {:width (str config/tile-size "px")
   :height (str config/tile-size "px")
   :top (* y config/tile-size)
   :left (* x config/tile-size)})

(defn do-all-tiles [[[x1 y1] [x2 y2] :as rect] layer-title f]
  [:div {:class "level-layer"}
   (for [x (range x1 (inc x2))
         y (range y1 (inc y2))
         :let [tile [x y]]]
     [:div {:key (str "location-cell:" layer-title ":" tile)
            :class "level-layer__tile"
            :style (apply tile-style (u/rect->0 rect tile))}
      (f tile)])])

(defn dropzone [{:keys [dimension highlight on-drop]}]
  [do-all-tiles dimension "dropzone"
   (fn [tile]
     [:div {:class ["interactor" (when (= tile highlight) "interactor_dropzone")]
            :on-drag-over u/prevent-e!
            :on-drag-enter (e-> #(>evt [:location-editor/set-highlight tile]))
            :on-drop (e-> #(on-drop tile))}])])

(defn background-tiles [rect background]
  [do-all-tiles rect "background"
   (fn [tile]
     [c/sprite-texture (get background tile)])])

(defn do-some-tiles [rect coll layer-title f]
  (for [[tile item] coll]
    [:div {:key (str "location-cell:" layer-title ":" tile)
           :class "level-layer__tile"
           :style (apply tile-style (u/rect->0 rect tile))}
     (f tile item)]))

(defn player-layer [rect position]
  [:div {:class "level-layer"}
   (do-some-tiles rect {position :player} "player"
                  (fn []
                    [c/sprite-texture :human "Player"]))])

(defn npc-layer [rect npcs]
  [:div {:class "level-layer"}
   (do-some-tiles rect npcs "npc"
                  (fn [tile {:keys [display-name texture]}]
                    [c/sprite-texture texture display-name]))])

(defn conntection-trigger-layer [rect connection-triggers]
  [:div {:class "level-layer"}
   (do-some-tiles rect connection-triggers "connection-trigger"
                  (fn [tile {:keys [id display-name]}]
                    [:img {:src (texture-path :exit)
                           :title (str "to " display-name)}]))])

(defn canvas [location-id]
  (let [{:keys [dimension background walk-set connection-triggers]} (<sub [:location-editor/location location-id])
        npcs (<sub [:location-editor/npcs location-id])
        {:keys [tool highlight painting?]} (<sub [:location-editor/ui])
        dnd-payload (<sub [:dnd-payload])
        player-position (<sub [:location-editor/player-position location-id])]
    [:div {:class "level-wrap"}
     [:div {:class "level"
            :on-mouse-leave #(>evt [:location-editor/unset-highlight])
            :style {:width (str (* config/tile-size (u/rect-width dimension)) "px")
                    :height (str (* config/tile-size (u/rect-height dimension)) "px")}}
      [background-tiles dimension background]
      [npc-layer dimension npcs]
      (when player-position [player-layer dimension player-position])
      [conntection-trigger-layer dimension connection-triggers]

      (case tool
        :background-painter
        [tile-paint-canvas
         {:dimension dimension
          :on-paint #(>evt [:location-editor/paint location-id %])}]

        :collision
        [:div
         [do-all-tiles dimension "walkable-area"
          (fn [tile]
            [:div {:class ["interactor"
                           (if (contains? walk-set tile)
                             "interactor_walkable"
                             "interactor_not-walkable")]}])]
         [tile-paint-canvas
          {:dimension dimension
           :on-paint #(>evt [:location-editor/set-walkable location-id %])}]]
        :npcs-select
        [:div
         [:div {:class "level-layer"}
          (do-some-tiles dimension npcs "npc-select"
                         (fn [_ {:keys [id dialogue-id texture display-name]}]
                           [:div {:class "interactor interactor_draggable"
                                  :title display-name
                                  :draggable true
                                  :on-click #(>navigate :dialogue-edit :id dialogue-id)
                                  :on-drag-start (fn [e]
                                                   (set-dnd-texture! e)
                                                   (.setData (.-dataTransfer e) "text/plain" display-name)
                                                   (>evt [:location-editor/start-entity-drag {:character-id id}]))}
                            [dnd-texture texture]]))
          (do-some-tiles dimension connection-triggers "connection-select"
                         (fn [tile {id :entity/id :keys [display-name]}]
                           [:div {:class "interactor interactor_draggable"
                                  :title (str "to " display-name)
                                  :draggable true
                                  :on-click #(>navigate :location-edit :id id)
                                  :on-drag-start (fn [e]
                                                   (set-dnd-texture! e)
                                                   (.setData (.-dataTransfer e) "text/plain" display-name)
                                                   (>evt [:location-editor/start-entity-drag {:connection-trigger id}]))}
                            [dnd-texture :exit]]))]
         (when-let [character-id (:character-id dnd-payload)]
           [dropzone {:dimension dimension
                      :highlight highlight
                      :on-drop #(>evt [:location-editor/move-character location-id character-id %])}])

         (when (= dnd-payload :player)
           [dropzone {:dimension dimension
                      :highlight highlight
                      :on-drop #(>evt [:location-editor/move-player location-id %])}])

         (when-let [target (:connection-trigger dnd-payload)]
           [dropzone {:dimension dimension
                      :highlight highlight
                      :on-drop #(>evt [:location-editor/move-trigger location-id target %])}])]
        nil)]]))

(defn location-editor [location-id]
  (if (<sub [:location-editor/location-exists? location-id])
    [:div {:class "location-editor"
           :on-drag-end #(>evt [:location-editor/stop-entity-drag])}
     [:div {:class "location-editor__sidebar"}
      [sidebar location-id]]
     [:div {:class "location-editor__canvas"}
      [canvas location-id]]]
    "Location not found."))
