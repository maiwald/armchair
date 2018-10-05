(ns armchair.views.location-editor
  (:require [armchair.slds :as slds]
            [armchair.config :as config]
            [armchair.util :refer [rect->0 rect-width rect-height once]]
            [armchair.textures :refer [texture-path background-textures]]))

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

;; Components

(defn icon [glyph title]
  [:i {:class (str "fas fa-" glyph)
       :title title}])

;; Location Editor

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

(defn location-editor-sidebar-paint [location-id]
  (let [{active-texture :active-texture} (<sub [:location-editor/ui])]
    [slds/label "Background Textures"
     [:ul {:class "tile-grid"}
      (for [texture background-textures]
        [:li {:key (str "texture-select:" texture)
              :title texture
              :class ["tile-grid__item"
                      (when (= texture active-texture) "tile-grid__item_active")]}
         [:a {:on-click #(>evt [:set-active-texture texture])}
          [:img {:src (texture-path texture)}]]])]]))

(defn location-editor-sidebar-resize [location-id]
  [slds/label "Resize level"
   [:div {:class "resize-container"}
    [:div {:class "resize-container__reference"}
     [:div {:class "resizer resizer_horizontal resizer_top"}
      [:a {:on-click #(>evt [:resize-larger location-id :up])} [icon "arrow-up" "extend"]]
      [:a {:on-click #(>evt [:resize-smaller location-id :up])} [icon "arrow-down" "shrink"]]]
     [:div {:class "resizer resizer_horizontal resizer_bottom"}
      [:a {:on-click #(>evt [:resize-smaller location-id :down])} [icon "arrow-up" "shrink"]]
      [:a {:on-click #(>evt [:resize-larger location-id :down])} [icon "arrow-down" "extend"]]]
     [:div {:class "resizer resizer_vertical resizer_left"}
      [:a {:on-click #(>evt [:resize-larger location-id :left])} [icon "arrow-left" "extend"]]
      [:a {:on-click #(>evt [:resize-smaller location-id :left])} [icon "arrow-right" "shrink"]]]
     [:div {:class "resizer resizer_vertical resizer_right"}
      [:a {:on-click #(>evt [:resize-smaller location-id :right])} [icon "arrow-left" "shrink"]]
      [:a {:on-click #(>evt [:resize-larger location-id :right])} [icon "arrow-right" "extend"]]]]]])

(defn location-editor-sidebar-npcs [location-id]
  (let [available-npcs (<sub [:location-editor/available-npcs])
        dnd-entity (:entity (<sub [:dnd-payload]))]
    [slds/label "Available NPCs"
     [:ul {:class "tile-list"}
      (for [[character-id {:keys [display-name texture]}] available-npcs]
        [:li {:key (str "character-select" display-name)
              :class "tile-list__item"
              :draggable true
              :on-drag-start (fn [e]
                               (set-dnd-texture! e)
                               (>evt [:start-entity-drag {:entity character-id}]))}
         [dnd-texture texture]
         [:span {:class "tile-list__item__image"
                 :style {:width (str config/tile-size "px")
                         :height (str config/tile-size "px")}}
           [:img {:title display-name :src (texture-path texture)}]]
         [:span {:class "tile-list__item__label"} display-name]])
      (when (and (some? dnd-entity)
                 (not (contains? available-npcs dnd-entity)))
        [:li {:class "tile-list__item tile-list__item_dropzone"
              :on-drag-over stop-e!
              :on-drop #(>evt [:remove-entity location-id dnd-entity])}
         [:span {:class "tile-list__item__image"
                 :style {:width (str config/tile-size "px")
                         :height (str config/tile-size "px")}}
          [icon "trash" "Drop here to remove."]]
         [:span {:class "tile-list__item__label"} "Drop here to remove."]])]]))

(defn location-editor-sidebar-connections [location-id]
  [slds/label "Assigned Connections"
   [:ul {:class "tile-list"}
    (for [[_ {target-id :id :keys [display-name]}] (<sub [:connected-locations location-id])]
      [:li {:key (str "connection-select" display-name)
            :class "tile-list__item"
            :draggable true
            :on-drag-start (fn [e]
                             (set-dnd-texture! e)
                             (>evt [:start-entity-drag {:connection-trigger target-id}]))}
       [dnd-texture :exit]
       [:img {:title display-name :src (texture-path :exit)}]
       [:span display-name]])]])

(defn location-editor-sidebar [location-id]
  (let [{:keys [name]} (<sub [:location-editor/everything location-id])
        {:keys [tool]} (<sub [:location-editor/ui])]
    [slds/form
     [slds/input-text {:label "Name"
                       :on-change #(>evt [:location-editor/update-name location-id (e->val %)])
                       :value name}]
     [slds/radio-button-group {:label "Tools"
                               :options [[:background-painter [icon "layer-group" "Background"]]
                                         [:resize [icon "arrows-alt" "Resize"]]
                                         [:collision [icon "walking" "Collision"]]
                                         [:npcs-select [icon "user" "NPCs"]]
                                         [:connection-select [icon "external-link-alt" "Connections"]]]
                               :active tool
                               :on-change #(>evt [:set-tool %])}]
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
  (let [{:keys [dimension background npcs walk-set connection-triggers]} (<sub [:location-editor/everything location-id])
        {:keys [tool highlight painting?]} (<sub [:location-editor/ui])
        dnd-payload (<sub [:dnd-payload])]
    [:div {:class "level"
           :on-mouse-leave #(>evt [:unset-highlight])
           :style {:width (str (* config/tile-size (rect-width dimension)) "px")
                   :height (str (* config/tile-size (rect-height dimension)) "px")}}
     [background-tiles dimension background]
     [npc-layer dimension npcs]
     [conntection-trigger-layer dimension connection-triggers]

     (case tool
       :background-painter
       (do-all-tiles dimension "paint"
                     (fn [tile]
                       [:div {:class "interactor interactor_paint"
                              :on-mouse-down (e-> #(>evt [:start-painting location-id tile]))
                              :on-mouse-over (e-> #(when painting? (>evt [:paint location-id tile])))
                              :on-mouse-up (e-> #(when painting? (>evt [:stop-painting])))}]))
       :collision
       (do-all-tiles dimension "walkable-area"
                     (fn [tile]
                       (let [walkable? (contains? walk-set tile)]
                         [:div {:class ["interactor"
                                        (if walkable?
                                          "interactor_walkable"
                                          "interactor_not-walkable")]
                                :on-click #(>evt [:set-walkable location-id tile (not walkable?)])}])))
       :npcs-select
       [:div
        (do-some-tiles dimension npcs "npc-select"
                       (fn [tile {:keys [id texture display-name]}]
                         [:div {:class "interactor interactor_draggable"
                                :title display-name
                                :draggable true
                                :on-drag-start (fn [e]
                                                 (set-dnd-texture! e)
                                                 (>evt [:start-entity-drag {:entity id}]))}
                          [dnd-texture texture]]))
        (when-let [entity (:entity dnd-payload)]
          (do-all-tiles dimension "dropzone"
                        (fn [tile]
                          [:div {:class ["interactor" (when (= tile highlight) "interactor_dropzone")]
                                 :on-drag-over (e-> (once #(>evt [:set-highlight tile])))
                                 :on-drop #(>evt [:move-entity location-id entity tile])}])))]
       :connection-select
       [:div
        (do-some-tiles dimension connection-triggers "connection-select"
                       (fn [tile {:keys [id display-name]}]
                         [:div {:class "interactor interactor_draggable"
                                :title (str "to " display-name)
                                :draggable true
                                :on-drag-start (fn [e]
                                                 (set-dnd-texture! e)
                                                 (>evt [:start-entity-drag {:connection-trigger id}]))}
                          [dnd-texture :exit]]))
        (when-let [target (:connection-trigger dnd-payload)]
          (do-all-tiles dimension "dropzone"
                        (fn [tile]
                          [:div {:class ["interactor" (when (= tile highlight) "interactor_dropzone")]
                                 :on-drag-over (e-> (once #(>evt [:set-highlight tile])))
                                 :on-drop #(>evt [:move-trigger location-id target tile])}])))]
       nil)]))

(defn location-editor [location-id]
  [:div {:class "location-editor"
         :on-drag-end #(>evt [:stop-entity-drag])}
   [:div {:class "location-editor__sidebar"}
    [location-editor-sidebar location-id]]
   [:div {:class "location-editor__canvas"}
    [location-editor-canvas location-id]]])
