(ns armchair.location-editor.views
  (:require [reagent.core :as r]
            [armchair.components :as c]
            [armchair.components.tile-map :refer [tile-dropzone]]
            [armchair.config :as config]
            [armchair.math :refer [Point
                                   Rect
                                   relative-point
                                   global-point
                                   rect->point-seq
                                   rect-contains?]]
            [armchair.util :as u :refer [<sub >evt]]
            [armchair.textures :refer [image-path]]))

(defn tile-paint-canvas []
  (let [painted-tiles (r/atom nil)
        current-tile (r/atom nil)]
    (letfn [(set-current-tile [e] (reset! current-tile (u/e->tile e)))
            (clear-current-tile [] (reset! current-tile nil))
            (start-painting [] (reset! painted-tiles #{}))
            (stop-painting [] (reset! painted-tiles nil))
            (make-paint [paint-fn]
              (fn [e]
                (let [tile (u/e->tile e)]
                  (when (and (some? @painted-tiles)
                             (not (contains? @painted-tiles tile)))
                    (swap! painted-tiles conj tile)
                    (paint-fn tile)))))]
      (fn [{:keys [on-paint texture]}]
        (let [paint (make-paint on-paint)]
          [:div {:class "level__layer"
                 :on-mouse-enter set-current-tile
                 :on-mouse-leave clear-current-tile
                 :on-mouse-down (fn [e]
                                  (u/stop-e! e)
                                  (when (u/e->left? e)
                                    (start-painting)
                                    (paint e)))
                 :on-mouse-move (fn [e]
                                  (u/stop-e! e)
                                  (set-current-tile e)
                                  (paint e))
                 :on-mouse-up stop-painting}
           (if-let [tile @current-tile]
             (if (some? texture)
               [:div {:style (merge {:position "absolute"
                                     :opacity ".8"}
                                    (u/tile-style tile))}
                [c/sprite-texture texture]]
               [:div {:class "interactor interactor_paint"
                      :style (u/tile-style tile)}]))])))))

(defn do-all-tiles [rect layer-title f]
  [:<>
   (for [tile (rect->point-seq rect)
         :let [tile-data (f tile)]
         :when (some? tile-data)]
     [:div {:key (str "location-cell:" layer-title ":" (pr-str tile))
            :class "level__tile"
            :style (u/tile-style (global-point tile rect))}
      tile-data])])

(defn do-some-tiles [rect coll layer-title f]
  [:<>
   (for [[tile item] coll
         :when (rect-contains? rect tile)]
     [:div {:key (str "location-cell:" layer-title ":" (pr-str tile))
            :class "level__tile"
            :style (u/tile-style (global-point tile rect))}
      (f tile item)])])

(defn texture-layer [{:keys [location-id layer-id override-rect]}]
  (let [location (<sub [:location-editor/location location-id])
        rect (or override-rect (:bounds location))
        layer (get location layer-id)]
    [do-all-tiles rect (str "texture-layer:" layer-id)
     (fn [tile]
       (if-let [t (get layer tile)]
         [c/sprite-texture t]))]))

(defn player-tile [rect position]
  (when (rect-contains? rect position)
    [:div {:key (str "location-cell:player:" position)
           :class "level__tile"
           :style (u/tile-style (global-point position rect))}
     [c/sprite-texture ["hare.png" (Point. 6 0)] "Player"]]))

(defn entity-layer [location-id override-rect]
  (let [{:keys [player-position
                characters
                bounds]} (<sub [:location-editor/entity-layer location-id])
        rect (or override-rect bounds)]
    [:<>
     [do-some-tiles rect characters "npc"
      (fn [_tile {:keys [display-name texture]}]
        [c/sprite-texture texture display-name])]
     (when (some? player-position)
       [player-tile rect player-position])]))

(defn conntection-trigger-layer [location-id override-rect]
  (let [{:keys [bounds
                connection-triggers]} (<sub [:location-editor/connection-trigger-layer location-id])
        rect (or override-rect bounds)]
    [do-some-tiles rect connection-triggers "connection-trigger"
     (fn [_tile {:keys [display-name]}]
       [:img {:src (image-path "exit.png")
              :title (str "to " display-name)}])]))

(defn collision-layer [rect blocked]
  [do-all-tiles rect "collision"
   (fn [tile]
     [:div {:class ["interactor"
                    (if (contains? blocked tile)
                      "interactor_not-walkable"
                      "interactor_walkable")]}])])

(defn location-preview [location-id preview-tile]
  (let [tiles-around 3
        bounds (Rect. (- (:x preview-tile) tiles-around)
                      (- (:y preview-tile) tiles-around)
                      (inc (* 2 tiles-around))
                      (inc (* 2 tiles-around)))]
    [:div {:class "level"
           :style {:width (u/px (* config/tile-size (:w bounds)))
                   :height (u/px (* config/tile-size (:h bounds)))}}
     [texture-layer {:location-id location-id
                     :layer-id :background1
                     :override-rect bounds}]
     [texture-layer {:location-id location-id
                     :layer-id :background2
                     :override-rect bounds}]
     [texture-layer {:location-id location-id
                     :layer-id :foreground1
                     :override-rect bounds}]
     [texture-layer {:location-id location-id
                     :layer-id :foreground2
                     :override-rect bounds}]
     [entity-layer location-id bounds]
     [conntection-trigger-layer location-id bounds]
     [:div {:key "location-cell:highlight"
            :class "level__tile level__tile_highlight"
            :style (u/tile-style (global-point preview-tile bounds))}]]))

(defn tile-select [{:keys [bounds on-select selected selectable?]}]
  (letfn [(on-click [e]
            (let [tile (relative-point (u/e->tile e) bounds)]
              (when (or (not (fn? selectable?))
                        (selectable? tile))
                (on-select tile))))]
    [:div {:style {:position "absolute"
                   :top 0
                   :left 0
                   :width (u/px (* config/tile-size (:w bounds)))
                   :height (u/px (* config/tile-size (:h bounds)))}}
     (when selected
       [:div {:key "location-cell:selected"
              :class "level__tile level__tile_highlight"
              :style (u/tile-style (global-point selected bounds))}])
     (when (fn? selectable?)
       [do-all-tiles bounds "disabled-selectors"
        (fn [tile]
          (if-not (selectable? tile)
            [:div {:class ["interactor" "interactor_disabled"]}]))])
     [:div {:class "level__layer"
            :on-click on-click}]]))

(defn position-select [location-id on-select selected]
  (let [{:keys [bounds]} (<sub [:location-editor/location location-id])
        occupied (<sub [:location-editor/physically-occupied-tiles location-id])]
    [:div {:style {:overflow "scroll"
                   :background-color "#000"
                   :max-width (u/px 600)
                   :max-height (u/px 400)}}
     [:div {:class "level"
            :style {:width (u/px (* config/tile-size (:w bounds)))
                    :height (u/px (* config/tile-size (:h bounds)))
                    :margin "auto"}}
      [texture-layer {:location-id location-id
                      :layer-id :background1}]
      [texture-layer {:location-id location-id
                      :layer-id :background2}]
      [texture-layer {:location-id location-id
                      :layer-id :foreground1}]
      [texture-layer {:location-id location-id
                      :layer-id :foreground2}]
      [entity-layer location-id]
      [conntection-trigger-layer location-id]
      [tile-select {:bounds bounds
                    :on-select on-select
                    :selected selected
                    :selectable? (fn [tile] (not (contains? occupied tile)))}]]]))

(defn canvas [location-id]
  (let [{:keys [bounds blocked]} (<sub [:location-editor/location location-id])
        {:keys [active-layer
                visible-layers
                active-tool
                active-texture]} (<sub [:location-editor/ui])
        width (* config/tile-size (:w bounds))
        height (* config/tile-size (:h bounds))]
    [c/scroll-container {:width (* 3 width) :height (* 3 height)}
     [:div {:class "level-wrap"}
      [:div {:class "level"
             :style {:width (u/px width)
                     :height (u/px height)}}

       (for [[layer-id] (reverse config/location-editor-layers)
             :when (contains? visible-layers layer-id)]
         (case layer-id
           :entities
           ^{:key "layer::entities"}
           [entity-layer location-id]

           :collision
           ^{:key "layer::collision"}
           [collision-layer bounds blocked]

           :triggers
           ^{:key "layer::triggers"}
           [conntection-trigger-layer location-id]

           (:background1 :background2 :foreground1 :foreground2)
           ^{:key (str "layer:" layer-id)}
           [texture-layer {:location-id location-id
                           :layer-id layer-id}]))

       (case active-layer
         (:background1 :background2 :foreground1 :foreground2)
         [tile-paint-canvas
          {:texture (when (not= active-tool :eraser) active-texture)
           :on-paint #(>evt [:location-editor/paint location-id active-layer
                             (relative-point % bounds)])}]

         :collision
         [tile-paint-canvas
          {:on-paint #(>evt [:location-editor/set-walkable location-id
                             (relative-point % bounds)])}]

         nil)

       (when (<sub [:ui/dnd])
         [tile-dropzone {:occupied (<sub [:location/occupied-tiles location-id])
                         :on-drop #(>evt [:drop-entity location-id (relative-point % bounds)])}])]]]))

(defn location-editor [location-id]
  (if (<sub [:location-editor/location-exists? location-id])
    [canvas location-id]
    "Location not found."))
