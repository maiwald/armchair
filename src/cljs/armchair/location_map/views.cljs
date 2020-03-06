(ns armchair.location-map.views
  (:require [armchair.components :as c :refer [drag-canvas graph-node connection]]
            [armchair.math :as m]
            [armchair.util :as u :refer [<sub >evt]]
            [armchair.config :as config]
            [armchair.routes :refer [>navigate]]
            [goog.functions :refer [debounce]]))

(def map-scale 0.5)

(defn location-component [location-id]
  (let [{:keys [dimension
                display-name
                preview-image-src]} (<sub [:location-map/location location-id])]
    [:div.location
     [graph-node {:title [:a {:on-click #(>navigate :location-edit :id location-id)
                              :on-mouse-down u/stop-e!}
                          display-name]
                  :item-id location-id
                  :width nil
                  :actions [["trash" "Delete"
                             #(when (js/confirm "Are you sure you want to delete this location?")
                                (>evt [:delete-location location-id]))]]}
      (if (some? preview-image-src)
        [:img {:src preview-image-src
               :on-click #(>evt [:inspect :location location-id])
               :style {:width (u/px (* config/tile-size (:w dimension) map-scale))
                       :height (u/px (* config/tile-size (:h dimension) map-scale))}}])]]))

(defn location-connection [dimensions start end]
  (let [start-pos (m/global-point (<sub [:ui/position start]) dimensions)
        end-pos (m/global-point (<sub [:ui/position end]) dimensions)]
    [connection {:start (m/translate-point start-pos (/ config/line-width 2) 15)
                 :end (m/translate-point end-pos (/ config/line-width 2) 15)}]))

(defn location-map []
  (let [update-offset (debounce
                        (fn [offset]
                          (>evt [:location-map/update-offset offset]))
                        200)
        on-scroll (fn [e]
                    (let [target (.-currentTarget e)
                          offset (m/Point. (.-scrollLeft target) (.-scrollTop target))]
                      (update-offset offset)))]
    (fn []
      (let [{:keys [dimensions scroll-offset location-ids connections]} (<sub [:location-map map-scale])]
        [drag-canvas {:kind "location"
                      :dimensions dimensions
                      :scroll-offset scroll-offset
                      :on-scroll on-scroll
                      :nodes {location-component location-ids}}
         [:svg {:class "graph__connection-container" :version "1.1"
                :baseProfile "full"
                :xmlns "http://www.w3.org/2000/svg"}
          (for [[start end] connections]
            ^{:key (str "location-connection" start "->" end)}
            [location-connection dimensions start end])]]))))
