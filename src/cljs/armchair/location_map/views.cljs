(ns armchair.location-map.views
  (:require [reagent.core :as r]
            [armchair.components :as c]
            [armchair.components.tile-map :refer [tile-select tile-dropzone]]
            [armchair.math :as m]
            [armchair.util :as u :refer [<sub >evt e->left?]]
            [goog.functions :refer [debounce]]))

(defn drag-container []
  (let [dragging? (<sub [:dragging?])
        stop-dragging (when dragging? #(>evt [:end-dragging]))]
    (into [:div
           {:class ["drag-container" (when dragging? "drag-container_is-dragging")]
            :on-mouse-leave stop-dragging
            :on-mouse-move (when dragging? #(>evt [:move-cursor (u/e->point %)]))}]
          (r/children (r/current-component)))))

(defn location []
  (let [mouse-down-start (atom nil)]
    (fn [location-id]
      (let [{:keys [display-name
                    characters
                    preview-image-background-src
                    preview-image-foreground-src
                    preview-image-w
                    preview-image-h
                    zoom-scale
                    bounds
                    inspecting?]} (<sub [:location-map/location location-id])
            dragging? (<sub [:dragging-item? location-id])
            position (<sub [:location-map/location-position location-id])
            start-dragging (fn [e]
                             (when (e->left? e)
                               (u/prevent-e! e)
                               (>evt [:start-dragging #{location-id} (u/e->point e) zoom-scale])))
            stop-dragging (fn [e]
                            (when dragging?
                              (u/prevent-e! e)
                              (>evt [:end-dragging])))
            inspect-location #(>evt [:inspect :location location-id])]
        [:div {:class ["location"
                       (when inspecting? "location_is-inspecting")
                       (when dragging? "location_is-dragging")]
               :on-mouse-down u/stop-e!
               :style {:left (u/px (:x position))
                       :top (u/px (:y position))}}
         [:header {:class "location__header"
                   :on-mouse-down (fn [e]
                                    (reset! mouse-down-start (js/Date.now))
                                    (start-dragging e))
                   :on-mouse-up (fn [e]
                                  ;; should this count as click or just a drag?
                                  (when (and (some? @mouse-down-start)
                                             (< (- (js/Date.now) @mouse-down-start) 200))
                                    (inspect-location)
                                    (reset! mouse-down-start nil))
                                  (stop-dragging e))}
          [:p {:class "location__header__title"}
           display-name]]
         (if (and (some? preview-image-background-src)
                  (some? preview-image-foreground-src))
           [:div {:class "location__tilemap-wrapper"}
            [:div {:class "location__tilemap"
                   :style {:width (u/px preview-image-w)
                           :height (u/px preview-image-h)}}
             [:img {:src preview-image-background-src
                    :style {:width (u/px preview-image-w)
                            :height (u/px preview-image-h)}}]
             (when (seq characters)
               [:div
                (for [[tile {:keys [texture display-name inspecting?]}] characters]
                  [:div {:key (str "location-character:" location-id ",tile:" (pr-str tile))
                         :class ["location__tilemap__character" (when inspecting? "location__tilemap__character_is-inspecting")]
                         :style (u/tile-style tile zoom-scale)}
                   [c/sprite-texture texture display-name zoom-scale]])])
             [:img {:src preview-image-foreground-src
                    :style {:width (u/px preview-image-w)
                            :height (u/px preview-image-h)}}]
             [tile-select {:zoom-scale zoom-scale
                           :occupied (<sub [:location/occupied-tiles location-id])
                           :on-drag-start #(>evt [:start-entity-drag %])
                           :on-drag-end #(>evt [:stop-entity-drag])
                           :on-click #(>evt [:inspect :tile location-id (m/relative-point % bounds)])}]
             (when (<sub [:ui/dnd])
               [tile-dropzone {:zoom-scale zoom-scale
                               :occupied (<sub [:location/occupied-tiles location-id])
                               :on-drop #(>evt [:drop-entity location-id (m/relative-point % bounds)])}])]]
           [:div {:class "location__loading"
                  :style {:width (u/px preview-image-w)
                          :height (u/px preview-image-h)}}
            [c/spinner]])]))))

(defn location-connection [[start-location start-tile]
                           [end-location end-tile]]
  (let [tile-size (<sub [:location-map/tile-size])
        {start-tile :tile start-tile-center :tile-center} (<sub [:location-map/connection-position start-location start-tile])
        {end-tile-center :tile-center} (<sub [:location-map/connection-position end-location end-tile])]
    [:<>
     [:rect {:stroke "red"
             :stroke-width 1
             :fill "none"
             :x (:x start-tile)
             :y (:y start-tile)
             :width tile-size
             :height tile-size}]
     [:rect {:fill "yellow"
             :x (- (:x end-tile-center) 3)
             :y (- (:y end-tile-center) 3)
             :width 6
             :height 6}]
     [:line {:class ["location-connection"]
             :x1 (:x start-tile-center)
             :y1 (:y start-tile-center)
             :x2 (:x end-tile-center)
             :y2 (:y end-tile-center)}]]))

(defn connections []
  (let [cs (<sub [:location-map/connections])]
    [:svg {:class "location-connections" :version "1.1"
           :baseProfile "full"
           :xmlns "http://www.w3.org/2000/svg"}
     (for [[start end] cs]
       ^{:key (str "location-connection" start "->" end)}
       [location-connection start end])]))


(defn e->scroll-center [e]
  (let [target (.-currentTarget e)]
    (m/Point. (+ (.-scrollLeft target) (/ (.-clientWidth target) 2))
              (+ (.-scrollTop target) (/ (.-clientHeight target) 2)))))

(defn location-map []
  (let [update-scroll-center (debounce #(>evt [:location-map/update-scroll-center %]) 200)
        on-scroll (comp update-scroll-center e->scroll-center)]
    (fn []
      (let [{:keys [bounds scroll-center location-ids zoom-scale]} (<sub [:location-map])]
        [c/scroll-container {:width (:w bounds)
                             :height (:h bounds)
                             :scroll-center scroll-center
                             :zoom-scale zoom-scale
                             :on-scroll on-scroll}
         [drag-container
          (for [id location-ids]
            ^{:key (str "location:" id)}
            [location id])
          [connections]]]))))
