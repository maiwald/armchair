(ns armchair.location-map.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub subscribe]]
            [com.rpl.specter
             :refer [collect-one ALL FIRST LAST]
             :refer-macros [select]]
            [armchair.config :as config]
            [armchair.util :as u]
            [armchair.math :as m :refer [translate-point]]))

(reg-sub
  :location-map/location-characters
  (fn [[_ location-id]]
    [(subscribe [:db/location location-id])
     (subscribe [:db-characters])
     (subscribe [:ui/inspector])])
  (fn [[location characters [inspector-type {inspector-location-id :location-id
                                             inspector-location-position :location-position}]] [_ location-id]]
    (->> (:placements location)
         (u/map-values
           (fn [{:keys [character-id]} tile]
             (let [character (characters character-id)]
               (merge (select-keys character [:texture :display-name])
                      {:inspecting? (and (= inspector-type :placement)
                                         (= inspector-location-id location-id)
                                         (= inspector-location-position tile))}))))
         (u/map-keys (fn [tile] (m/global-point tile (:bounds location)))))))

(reg-sub
  :location-map/location
  (fn [[_ location-id]]
    [(subscribe [:db/location location-id])
     (subscribe [:location-map/location-characters location-id])
     (subscribe [:ui/location-preview-cache-background])
     (subscribe [:ui/location-preview-cache-foreground])
     (subscribe [:ui/inspector])
     (subscribe [:ui/location-map-zoom-scale])])
  (fn [[location
        characters
        preview-cache-background
        preview-cache-foreground
        [inspector-type {inspector-location-id :location-id}]
        zoom-scale]
       [_ location-id]]
    (let [{:keys [display-name]
           {:keys [w h] :as bounds} :bounds} location
          preview-image-background-src (get preview-cache-background location-id)
          preview-image-foreground-src (get preview-cache-foreground location-id)]
      {:display-name display-name
       :zoom-scale zoom-scale
       :bounds bounds
       :characters characters
       :preview-image-background-src preview-image-background-src
       :preview-image-foreground-src preview-image-foreground-src
       :preview-image-w (* zoom-scale config/tile-size w)
       :preview-image-h (* zoom-scale config/tile-size h)
       :inspecting? (and (= inspector-type :location)
                         (= inspector-location-id location-id))})))

(reg-sub
  :location-map/bounds
  :<- [:db-locations]
  :<- [:ui/positions]
  :<- [:ui/location-map-zoom-scale]
  (fn [[locations positions zoom-scale]]
    (let [scale (* config/tile-size zoom-scale)]
      (m/rect-resize
        (->> locations
             (mapcat (fn [[id {{:keys [w h]} :bounds}]]
                       (let [p (m/point-scale (positions id) zoom-scale)]
                         [p (translate-point p (* w scale) (* h scale))])))
             m/containing-rect)
        (zipmap [:left :right :top :bottom]
                (repeat (* zoom-scale 400)))))))

(reg-sub
  :location-map/location-position
  (fn [[_ location-id]]
    [(subscribe [:ui/position location-id])
     (subscribe [:ui/location-map-zoom-scale])
     (subscribe [:location-map/bounds])])
  (fn [[position zoom-scale bounds]]
    (-> position
        (m/point-scale zoom-scale)
        (m/global-point bounds))))

(reg-sub
  :location-map/connections
  :<- [:db-locations]
  :<- [:ui/location-map-zoom-scale]
  (fn [[locations zoom-scale]]
    (let [center-tile-offset (+ 1 (* (/ config/tile-size 2) zoom-scale))]
      (letfn [(tile-offset [location-id position]
                (-> position
                    (m/global-point (-> location-id locations :bounds))
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
  :<- [:location-map/bounds]
  :<- [:ui/location-map-scroll-center]
  :<- [:ui/location-map-zoom-scale]
  (fn [[locations bounds scroll-center zoom-scale]]
    {:bounds bounds
     :scroll-center scroll-center
     :zoom-scale zoom-scale
     :location-ids (keys locations)}))
