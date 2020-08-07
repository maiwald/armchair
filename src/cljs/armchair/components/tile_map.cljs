(ns armchair.components.tile-map
  (:require [reagent.core :as r]
            [armchair.util :as u]))

(defn tile-dropzone []
  (let [hover-tile (r/atom nil)]
    (letfn [(set-hover-tile [tile] (reset! hover-tile tile))
            (clear-hover-tile [] (reset! hover-tile nil))]
      (fn [{:keys [occupied on-drop zoom-scale]
            :or {zoom-scale 1}}]
        [:div {:class "dropzone"
               :on-drag-over (fn [e]
                               (u/prevent-e! e)
                               (set-hover-tile (u/e->tile e zoom-scale)))
               :on-drag-leave clear-hover-tile
               :on-drop (fn [e]
                          (let [drop-tile (u/e->tile e zoom-scale)]
                            (when-not (contains? occupied drop-tile)
                              (on-drop drop-tile))))}
         (when (some? @hover-tile)
           [:div {:class ["dropzone__hover-tile"
                          (when (contains? occupied @hover-tile)
                            "dropzone__hover-tile_disabled")]
                  :style (u/tile-style @hover-tile zoom-scale)}])]))))

(defn tile-select []
  (let [hover-tile (r/atom nil)]
    (letfn [(set-hover-tile [tile] (reset! hover-tile tile))
            (clear-hover-tile [] (reset! hover-tile nil))]
      (fn [{:keys [on-click on-drag-start on-drag-end zoom-scale occupied]
            :or {zoom-scale 1}}]
        [:div {:class "tile-select"
               :draggable true
               :on-drag-start (fn [e]
                                (let [tile (u/e->tile e zoom-scale)]
                                  (if (contains? occupied tile)
                                    (do
                                      (.setDragImage (.-dataTransfer e) (js/Image.) 0 0)
                                      (on-drag-start (get occupied tile)))
                                    (u/prevent-e! e))))
               :on-drag-end on-drag-end
               :on-mouse-down u/stop-e!
               :on-mouse-move (fn [e] (set-hover-tile (u/e->tile e zoom-scale)))
               :on-mouse-leave clear-hover-tile
               :on-click (fn [e] (on-click (u/e->tile e zoom-scale)))}
         (when (some? @hover-tile)
           [:div {:class "tile-select__hover-tile"
                  :style (u/tile-style @hover-tile zoom-scale)}])]))))
