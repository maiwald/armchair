(ns armchair.components.tile-map
  (:require [reagent.core :as r]
            [armchair.util :as u]))

(defn tile-dropzone []
  (let [current-tile (r/atom nil)]
    (letfn [(set-current-tile [tile] (reset! current-tile tile))
            (clear-current-tile [] (reset! current-tile nil))]
      (fn [{:keys [occupied on-drop zoom-scale]
            :or {zoom-scale 1}}]
        [:div {:class "dropzone"
               :on-drag-over (fn [e]
                               (u/prevent-e! e)
                               (set-current-tile (u/e->tile e zoom-scale)))
               :on-drag-leave clear-current-tile
               :on-drop (fn []
                          (when-not (contains? occupied @current-tile)
                            (on-drop @current-tile)))}
         (when @current-tile
           [:div {:class ["dropzone__interactor"
                          (when (contains? occupied @current-tile)
                            "dropzone__interactor_disabled")]
                  :style (u/tile-style @current-tile zoom-scale)}])]))))

(defn tile-select []
  (let [highlight-tile (r/atom nil)]
    (fn [{:keys [on-click on-drag-start on-drag-end zoom-scale occupied]
          :or {zoom-scale 1}}]
      (let [set-highlight (fn [e] (reset! highlight-tile (u/e->tile e zoom-scale)))
            clear-highlight (fn [] (reset! highlight-tile nil))]
        [:div {:class "tile-select"
               :draggable true
               :on-drag-start (fn [e]
                                (if (contains? occupied @highlight-tile)
                                  (do
                                    (.setDragImage (.-dataTransfer e) (js/Image.) 0 0)
                                    (on-drag-start (get occupied @highlight-tile)))
                                  (u/prevent-e! e)))
               :on-drag-end on-drag-end
               :on-mouse-move set-highlight
               :on-mouse-leave clear-highlight
               :on-click #(on-click @highlight-tile)}
         (when (some? @highlight-tile)
           [:div {:class "tile-select__highlight"
                  :style (u/tile-style @highlight-tile zoom-scale)}])]))))

