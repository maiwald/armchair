(ns armchair.location-editor.views
  (:require [reagent.core :as r]
            [armchair.slds :as slds]
            [armchair.config :as config]
            [armchair.routes :refer [>navigate]]
            [armchair.util :refer [px <sub >evt stop-e! e-> e->val rect->0 rect-width rect-height once tile->coord coord->tile translate-point relative-cursor]]
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
    (letfn [(get-tile [e] (-> (relative-cursor e (.-currentTarget e))
                              coord->tile
                              (translate-point delta)))
            (set-current-tile [e] (reset! current-tile (get-tile e)))
            (clear-current-tile [] (reset! current-tile nil))
            (start-painting [] (reset! painted-tiles #{}))
            (stop-painting [] (reset! painted-tiles nil))
            (paint [e]
              (let [tile (get-tile e)]
                (when (and (some? @painted-tiles)
                           (not (contains? @painted-tiles tile)))
                  (swap! painted-tiles conj tile)
                  (on-paint tile))))]
      (fn []
        [:div {:class "level-layer"
               :on-mouse-enter set-current-tile
               :on-mouse-leave clear-current-tile
               :on-mouse-down #(do (start-painting)
                                   (paint %))
               :on-mouse-move #(do (set-current-tile %)
                                   (paint %))
               :on-mouse-up stop-painting}
         (when-let [tile @current-tile]
           (let [[x y] (tile->coord tile)]
             [:div {:class "interactor interactor_paint"
                    :style {:height (px config/tile-size)
                            :width (px config/tile-size)
                            :top (px y)
                            :left (px x)}}]))]))))

(defn location-editor-sidebar-paint [location-id]
  (let [{active-texture :active-texture} (<sub [:location-editor/ui])]
    [slds/label "Background Textures"
     [:ul {:class "tile-grid"}
      (for [texture background-textures]
        [:li {:key (str "texture-select:" texture)
              :title texture
              :class ["tile-grid__item"
                      (when (= texture active-texture) "tile-grid__item_active")]}
         [:a {:on-click #(>evt [:location-editor/set-active-texture texture])}
          [:img {:src (texture-path texture)}]]])]]))

(defn location-editor-sidebar-resize [location-id]
  [slds/label "Resize level"
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
      [:a {:on-click #(>evt [:location-editor/resize-larger location-id :right])} [icon "arrow-right" "extend"]]]]]])

(defn location-editor-sidebar-npcs [location-id]
  (let [available-npcs (<sub [:location-editor/available-npcs location-id])
        dnd-character-id (:character-id (<sub [:dnd-payload]))]
    [:div
      [slds/label "Player"
       [:ul {:class "tile-list"}
        [:li {:class "tile-list__item"
              :draggable true
              :on-drag-start (fn [e]
                               (set-dnd-texture! e)
                               (>evt [:location-editor/start-entity-drag :player]))}
         [dnd-texture :player]
         [:span {:class "tile-list__item__image"
                 :style {:width (str config/tile-size "px")
                         :height (str config/tile-size "px")}}
           [:img {:title "Player" :src (texture-path :player)}]]
         [:span {:class "tile-list__item__label"} "Player"]]]]
      [slds/label "Available Characters"
       [:ul {:class "tile-list"}
        (for [[character-id {:keys [display-name texture]}] available-npcs]
          [:li {:key (str "character-select" display-name)
                :class "tile-list__item"
                :draggable true
                :on-drag-start (fn [e]
                                 (set-dnd-texture! e)
                                 (>evt [:location-editor/start-entity-drag {:character-id character-id}]))}
           [dnd-texture texture]
           [:span {:class "tile-list__item__image"
                   :style {:width (str config/tile-size "px")
                           :height (str config/tile-size "px")}}
             [:img {:title display-name :src (texture-path texture)}]]
           [:span {:class "tile-list__item__label"} display-name]])
        (when (and (some? dnd-character-id)
                   (not (contains? available-npcs dnd-character-id)))
          [:li {:class "tile-list__item tile-list__item_dropzone"
                :on-drag-over stop-e!
                :on-drop #(>evt [:location-editor/remove-character dnd-character-id])}
           [:span {:class "tile-list__item__image"
                   :style {:width (str config/tile-size "px")
                           :height (str config/tile-size "px")}}
            [icon "trash" "Drop here to remove."]]
           [:span {:class "tile-list__item__label"} "Drop here to remove."]])]]]))

(defn location-editor-sidebar-connections [location-id]
  [slds/label "Assigned Connections"
   [:ul {:class "tile-list"}
    (for [[target-loctation-id {:keys [display-name]}] (<sub [:location-editor/connected-locations location-id])]
      [:li {:key (str "connection-select" display-name)
            :class "tile-list__item"
            :draggable true
            :on-drag-start (fn [e]
                             (set-dnd-texture! e)
                             (>evt [:location-editor/start-entity-drag {:connection-trigger target-loctation-id}]))}
       [dnd-texture :exit]
       [:img {:title display-name :src (texture-path :exit)}]
       [:span display-name]])]])

(defn location-editor-sidebar [location-id]
  (let [{:keys [display-name]} (<sub [:location-editor/location location-id])
        {:keys [tool]} (<sub [:location-editor/ui])]
    [slds/form
     [slds/input-text {:label "Name"
                       :on-change #(>evt [:location-editor/update-name location-id (e->val %)])
                       :value display-name}]
     [slds/radio-button-group {:label "Tools"
                               :options [[:background-painter [icon "paint-roller" "Background"]]
                                         [:resize [icon "expand" "Resize"]]
                                         [:collision [icon "walking" "Collision"]]
                                         [:npcs-select [icon "users" "Characters"]]
                                         [:connection-select [icon "external-link-alt" "Connections"]]]
                               :active tool
                               :on-change #(>evt [:location-editor/set-tool %])}]
     (case tool
       :background-painter [location-editor-sidebar-paint location-id]
       :resize [location-editor-sidebar-resize location-id]
       :npcs-select [location-editor-sidebar-npcs location-id]
       :connection-select [location-editor-sidebar-connections location-id]
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
            :style (apply tile-style (rect->0 rect tile))}
      (f tile)])])

(defn background-tiles [rect background]
  (do-all-tiles rect "background"
                (fn [tile]
                  [:img {:src (texture-path (get background tile))}])))

(defn do-some-tiles [rect coll layer-title f]
  [:div {:class "level-layer"}
   (for [[tile item] coll]
     [:div {:key (str "location-cell:" layer-title ":" tile)
            :class "level-layer__tile"
            :style (apply tile-style (rect->0 rect tile))}
      (f tile item)])])

(defn player-layer [rect position]
  (do-some-tiles rect {position :player} "player"
                 (fn []
                   [:img {:src (texture-path :player)
                          :title "Player"}])))

(defn npc-layer [rect npcs]
  (do-some-tiles rect npcs "npc"
                 (fn [tile {:keys [display-name texture]}]
                   [:img {:src (texture-path texture)
                          :title display-name}])))

(defn conntection-trigger-layer [rect connection-triggers]
  (do-some-tiles rect connection-triggers "connection-trigger"
                 (fn [tile {:keys [id display-name]}]
                   [:img {:src (texture-path :exit)
                          :title (str "to " display-name)}])))

(defn location-editor-canvas [location-id]
  (let [{:keys [dimension background walk-set connection-triggers]} (<sub [:location-editor/location location-id])
        npcs (<sub [:location-editor/npcs location-id])
        {:keys [tool highlight painting?]} (<sub [:location-editor/ui])
        dnd-payload (<sub [:dnd-payload])
        player-position (<sub [:location-editor/player-position location-id])]
    [:div {:class "level"
           :on-mouse-leave #(>evt [:location-editor/unset-highlight])
           :style {:width (str (* config/tile-size (rect-width dimension)) "px")
                   :height (str (* config/tile-size (rect-height dimension)) "px")}}
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
       (do-all-tiles dimension "walkable-area"
                     (fn [tile]
                       [:div {:class ["interactor"
                                      (if (contains? walk-set tile)
                                        "interactor_walkable"
                                        "interactor_not-walkable")]
                              :on-click #(>evt [:location-editor/flip-walkable location-id tile])}]))
       :npcs-select
       [:div
        (when player-position
          (do-some-tiles dimension {player-position :player} "player-select"
                       (fn []
                         [:div {:class "interactor interactor_draggable"
                                :title "Player"
                                :draggable true
                                :on-drag-start (fn [e]
                                                 (set-dnd-texture! e)
                                                 (>evt [:location-editor/start-entity-drag :player]))}
                          [dnd-texture :player]])))
        (do-some-tiles dimension npcs "npc-select"
                       (fn [_ {:keys [id dialogue-id texture display-name]}]
                         [:div {:class "interactor interactor_draggable"
                                :title display-name
                                :draggable true
                                :on-click #(>navigate :dialogue-edit :id dialogue-id)
                                :on-drag-start (fn [e]
                                                 (set-dnd-texture! e)
                                                 (>evt [:location-editor/start-entity-drag {:character-id id}]))}
                          [dnd-texture texture]]))
        (when-let [character-id (:character-id dnd-payload)]
          (do-all-tiles dimension "dropzone"
                        (fn [tile]
                          [:div {:class ["interactor" (when (= tile highlight) "interactor_dropzone")]
                                 :on-drag-over (e-> (once #(>evt [:location-editor/set-highlight tile])))
                                 :on-drop #(>evt [:location-editor/move-character location-id character-id tile])}])))
        (when (= dnd-payload :player)
          (do-all-tiles dimension "dropzone"
                        (fn [tile]
                          [:div {:class ["interactor" (when (= tile highlight) "interactor_dropzone")]
                                 :on-drag-over (e-> (once #(>evt [:location-editor/set-highlight tile])))
                                 :on-drop #(>evt [:location-editor/move-player location-id tile])}])))]
       :connection-select
       [:div
        (do-some-tiles dimension connection-triggers "connection-select"
                       (fn [tile {id :entity/id :keys [display-name]}]
                         [:div {:class "interactor interactor_draggable"
                                :title (str "to " display-name)
                                :draggable true
                                :on-drag-start (fn [e]
                                                 (set-dnd-texture! e)
                                                 (>evt [:location-editor/start-entity-drag {:connection-trigger id}]))}
                          [dnd-texture :exit]]))
        (when-let [target (:connection-trigger dnd-payload)]
          (do-all-tiles dimension "dropzone"
                        (fn [tile]
                          [:div {:class ["interactor" (when (= tile highlight) "interactor_dropzone")]
                                 :on-drag-over (e-> (once #(>evt [:location-editor/set-highlight tile])))
                                 :on-drop #(>evt [:location-editor/move-trigger location-id target tile])}])))]
       nil)]))

(defn location-editor [location-id]
  [:div {:class "location-editor"
         :on-drag-end #(>evt [:location-editor/stop-entity-drag])}
   [:div {:class "location-editor__sidebar"}
    [location-editor-sidebar location-id]]
   [:div {:class "location-editor__canvas"}
    [location-editor-canvas location-id]]])
