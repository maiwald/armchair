(ns armchair.views.location-editor
  (:require [armchair.slds :as slds]
            [armchair.config :as config]
            [armchair.util :refer [once]]
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

(defn set-drag-texture! [e texture]
  (let [offset (/ config/tile-size 2)
        image (js/Image.)]
    (set! (.-src image) (texture-path texture))
    (-> e .-dataTransfer (.setDragImage image offset offset))))

(defn location-editor-sidebar-paint []
  (let [{active-texture :active-texture} (<sub [:location-editor-data])]
    [slds/label "Background Textures"
     [:ul {:class "tile-grid"}
      (for [texture background-textures]
        [:li {:key (str "texture-select:" texture)
              :title texture
              :class ["tile-grid__item"
                      (when (= texture active-texture) "tile-grid__item_active")]}
         [:a {:on-click #(>evt [:set-active-texture texture])}
          [:img {:src (texture-path texture)}]]])]]))

(defn location-editor-sidebar-npcs [location-id]
  (let [available-npcs (<sub [:available-npcs location-id])
        dnd-entity (:entity (<sub [:dnd-payload]))]
    [slds/label "Available NPCs"
     [:ul {:class "tile-list"}
      (for [[_ {character-id :id :keys [display-name texture]}] available-npcs]
        [:li {:key (str "character-select" display-name)
              :class "tile-list__item"
              :draggable true
              :on-drag-start (fn [e]
                               (set-drag-texture! e texture)
                               (>evt [:start-entity-drag {:entity character-id}]))}
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
                             (set-drag-texture! e :marker)
                             (>evt [:start-entity-drag {:connection-trigger target-id}]))}
       [:img {:title display-name :src (texture-path :marker)}]
       [:span display-name]])]])

(defn location-editor-sidebar [location-id]
  (let [{:keys [display-name]} (<sub [:location location-id])
        {:keys [tool]} (<sub [:location-editor-data])]
    (letfn [(update-display-name [e]
              (>evt [:update-location location-id :display-name (e->val e)]))]
      [slds/form
       [slds/input-text {:label "Name"
                         :on-change update-display-name
                         :value display-name}]
       [slds/radio-button-group {:label "Tools"
                                 :options [[:paint [icon "layer-group" "Background"]]
                                           [:collision [icon "walking" "Collision"]]
                                           [:select [icon "user" "NPCs"]]
                                           [:connections [icon "external-link-alt" "Connections"]]]
                                 :active tool
                                 :on-change #(>evt [:set-tool %])}]
       (case tool
         :paint [location-editor-sidebar-paint]
         :select [location-editor-sidebar-npcs location-id]
         :connections [location-editor-sidebar-connections location-id]
         nil)])))

(defn tile-style [x y]
  {:width (str config/tile-size "px")
   :height (str config/tile-size "px")
   :top (* y config/tile-size)
   :left (* x config/tile-size)})

(defn tile [layer-title x y child]
  (into [:div {:key (str "location-cell:" layer-title ":" [x y])
               :class "level-layer__tile"
               :style (tile-style x y)}] child))

(defn do-all-tiles [level layer-title f]
  [:div {:class "level-layer"}
   (for [x (range (count level))
         y (range (count (first level)))
         :let [tile [x y]]]
     [:div {:key (str "location-cell:" layer-title ":" tile)
            :class "level-layer__tile"
            :style (tile-style x y)}
      (f tile)])])

(defn background [level]
  (do-all-tiles level "background"
                (fn [tile]
                  [:img {:src (texture-path (get-in level tile))}])))

(defn walkable-area [level walk-set]
  )

(defn do-some-tiles [coll layer-title f]
  [:div {:class "level-layer"}
   (for [[[x y :as tile] item] coll]
     [:div {:key (str "location-cell:" layer-title ":" tile)
            :class "level-layer__tile"
            :style (tile-style x y)}
      (f tile item)])])

(defn npc-layer [npcs]
  (do-some-tiles npcs "npc"
                 (fn [tile {:keys [display-name texture]}]
                   [:img {:src (texture-path texture)
                          :title display-name}])))

(defn conntection-trigger-layer [connection-triggers]
  (do-some-tiles connection-triggers "connection-trigger"
                 (fn [tile {:keys [id display-name]}]
                   [:img {:src (texture-path :marker)
                          :title (str "to " display-name)
                          :draggable true
                          :on-drag-start (fn [e]
                                           (set-drag-texture! e :marker)
                                           (>evt [:start-entity-drag {:connection-trigger id}]))}])))

(defn location-editor-canvas [location-id]
  (let [{:keys [level npcs walk-set connection-triggers]} (<sub [:location location-id])
        {:keys [tool highlight painting?]} (<sub [:location-editor-data])
        {:keys [width height]} (<sub [:level-dimensions location-id])
        dnd-payload (<sub [:dnd-payload])]
    [:div {:class "level"
           :on-mouse-leave #(>evt [:unset-highlight])
           :style {:width (str (* config/tile-size width) "px")
                   :height (str (* config/tile-size height) "px")}}
     [background level]
     [npc-layer npcs]
     [conntection-trigger-layer connection-triggers]

     (case tool
       :paint
       (do-all-tiles level "paint"
                     (fn [tile]
                       [:div {:class "interactor interactor_paint"
                              :on-mouse-down (e-> #(>evt [:start-painting location-id tile]))
                              :on-mouse-over (e-> #(when painting? (>evt [:paint location-id tile])))
                              :on-mouse-up (e-> #(when painting? (>evt [:stop-painting])))}]))
       :collision
       (do-all-tiles level "walkable-area"
                     (fn [tile]
                       [:div {:class ["interactor"
                                      (if (contains? walk-set tile)
                                        "interactor_walkable"
                                        "interactor_not-walkable")]
                              :on-click #(>evt [:flip-walkable location-id tile])}]))
       :select
       [:div
        (do-some-tiles npcs "npc-select"
                       (fn [tile {:keys [id texture display-name]}]
                         [:div {:class "interactor interactor_draggable"
                                :title display-name
                                :draggable true
                                :on-drag-start (fn [e]
                                                 (set-drag-texture! e texture)
                                                 (>evt [:start-entity-drag {:entity id}]))}]))
        (when-let [target (:entity dnd-payload)]
          (do-all-tiles level "dropzone"
                        (fn [tile]
                          [:div {:class ["interactor" (when (= tile highlight) "interactor_dropzone")]
                                 :on-drag-over (e-> (once #(>evt [:set-highlight tile])))
                                 :on-drop #(>evt [:move-entity location-id target tile])}])))]
       :connections
       [:div
        (do-some-tiles connection-triggers "connection-select"
                       (fn [tile {:keys [id display-name]}]
                         [:div {:class "interactor interactor_draggable"
                                :title (str "to " display-name)
                                :draggable true
                                :on-drag-start (fn [e]
                                                 (set-drag-texture! e :marker)
                                                 (>evt [:start-entity-drag {:connection-trigger id}]))}]))
        (when-let [target (:connection-trigger dnd-payload)]
          (do-all-tiles level "dropzone"
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
