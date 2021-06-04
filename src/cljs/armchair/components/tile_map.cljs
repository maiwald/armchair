(ns armchair.components.tile-map
  (:require [reagent.core :as r]
            [armchair.util :as u]))

(defn tile-dropzone []
  (let [hover-tile (r/atom nil)]
    (letfn [(set-hover-tile [tile] (reset! hover-tile tile))
            (clear-hover-tile [] (reset! hover-tile nil))]
      (fn [{:keys [can-drop? on-drop on-drag-over on-drag-leave zoom-scale]
            :or {zoom-scale 1}}]
        [:div {:class "dropzone"
               :on-drag-over (fn [e]
                               (u/prevent-e! e)
                               (let [hover-tile (u/e->tile e zoom-scale)]
                                 (set-hover-tile hover-tile)
                                 (when (fn? on-drag-over) (on-drag-over hover-tile))))
               :on-drag-leave (fn [] (clear-hover-tile)
                                (when (fn? on-drag-leave) (on-drag-leave)))
               :on-drop (fn [e]
                          (let [drop-tile (u/e->tile e zoom-scale)]
                            (when (can-drop? drop-tile)
                              (on-drop drop-tile))))}
         (when (some? @hover-tile)
           [:div {:class ["dropzone__hover-tile"
                          (when-not (can-drop? @hover-tile)
                            "dropzone__hover-tile_disabled")]
                  :style (u/tile-style @hover-tile zoom-scale)}])]))))

(defn tile-select []
  (let [hover-tile (r/atom nil)]
    (letfn [(set-hover-tile [tile] (reset! hover-tile tile))
            (clear-hover-tile [] (reset! hover-tile nil))]
      (fn [{:keys [on-click on-drag-start on-drag-end zoom-scale]
            :or {zoom-scale 1}}]
        [:div {:class "tile-select"
               :draggable true
               :on-drag-start (fn [e]
                                (.setDragImage
                                  (.-dataTransfer e)
                                  (.getElementById js/document "default-dnd-ghost")
                                  12 24)
                                (on-drag-start e (u/e->tile e zoom-scale)))
               :on-drag-end on-drag-end
               :on-mouse-down u/stop-e!
               :on-mouse-move (fn [e] (set-hover-tile (u/e->tile e zoom-scale)))
               :on-mouse-leave clear-hover-tile
               :on-click (fn [e] (on-click (u/e->tile e zoom-scale)))}
         (when (some? @hover-tile)
           [:div {:class "tile-select__hover-tile"
                  :style (u/tile-style @hover-tile zoom-scale)}])]))))

(defn sprite-select []
  (let [hover-tile (r/atom nil)]
    (letfn [(set-hover-tile [tile] (reset! hover-tile tile))
            (clear-hover-tile [] (reset! hover-tile nil))]
      (fn [{:keys [selected on-select tile-size gutter offset]}]
        [:div {:class "tile-select"
               :on-mouse-down u/stop-e!
               :on-mouse-move (fn [e] (set-hover-tile (u/e->sprite e tile-size gutter offset)))
               :on-mouse-leave clear-hover-tile
               :on-click (fn [e] (on-select (u/e->sprite e tile-size gutter offset)))}
         (when (some? @hover-tile)
           [:div {:class "tile-select__hover-tile"
                  :style (u/sprite-style @hover-tile tile-size gutter offset)}])
         (when (some? selected)
           [:div {:class "tile-select__selected-tile"
                  :style (u/sprite-style selected tile-size gutter offset)}])]))))
