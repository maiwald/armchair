(ns armchair.location-map.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub subscribe]]
            [com.rpl.specter
             :refer [collect-one ALL FIRST LAST MAP-VALS]
             :refer-macros [select]]
            [armchair.config :as config]
            [armchair.util :as u]
            [armchair.math :as m :refer [Point translate-point point-delta]]))

(reg-sub
  :location-map/location
  :<- [:db-locations]
  :<- [:ui/location-preview-cache]
  :<- [:ui/inspector]
  :<- [:ui/location-map-zoom-scale]
  (fn [[locations
        preview-cache
        [inspector-type {inspector-location-id :location-id}]
        zoom-scale]
       [_ location-id]]
    (let [{:keys [display-name]
           {:keys [w h]} :dimension} (get locations location-id)
          preview-image-src (get preview-cache location-id)]
      {:display-name display-name
       :zoom-scale zoom-scale
       :preview-image-src preview-image-src
       :preview-image-w (* zoom-scale config/tile-size w)
       :preview-image-h (* zoom-scale config/tile-size h)
       :inspecting? (and (= inspector-type :location)
                         (= inspector-location-id location-id))})))

(reg-sub
  :location-map/dimensions
  :<- [:db-locations]
  :<- [:ui/positions]
  :<- [:ui/location-map-zoom-scale]
  (fn [[locations positions zoom-scale]]
    (let [scale (* config/tile-size zoom-scale)
          map-dimensions (->> locations
                              (mapcat (fn [[id {{:keys [w h]} :dimension}]]
                                        (let [p (-> (positions id)
                                                    (m/point-scale zoom-scale))]
                                          [p (translate-point p (* w scale) (* h scale))])))
                              (m/containing-rect))]
      (m/rect-resize
        map-dimensions
        (zipmap [:left :right :top :bottom]
                (repeat (* zoom-scale 400)))))))

(reg-sub
  :location-map/location-position
  (fn [[_ location-id]]
    [(subscribe [:ui/position location-id])
     (subscribe [:ui/location-map-zoom-scale])
     (subscribe [:location-map/dimensions])])
  (fn [[position zoom-scale dimensions]]
    (-> position
        (m/point-scale zoom-scale)
        (m/global-point dimensions))))

(reg-sub
  :location-map/connections
  :<- [:db-locations]
  :<- [:ui/location-map-zoom-scale]
  (fn [[locations zoom-scale]]
    (let [center-tile-offset (+ 1 (* (/ config/tile-size 2) zoom-scale))]
      (letfn [(tile-offset [location-id position]
                (-> position
                    (m/global-point (-> location-id locations :dimension))
                    (m/point-scale (* config/tile-size zoom-scale))
                    (m/translate-point center-tile-offset center-tile-offset)))]
        (->> locations
          (select [ALL (collect-one FIRST) LAST :connection-triggers ALL])
          (map (fn [[from-id [from-position [to-id to-position]]]]
                 (vector [from-id (tile-offset from-id from-position)]
                         [to-id (tile-offset to-id to-position)]))))))))

(reg-sub
  :location-map
  :<- [:db-locations]
  :<- [:location-map/dimensions]
  :<- [:ui/location-map-scroll-offset]
  (fn [[locations dimensions scroll-offset]]
    {:dimensions dimensions
     :scroll-offset scroll-offset
     :location-ids (keys locations)}))
