(ns armchair.location-map.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
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
  (fn [[locations
        preview-cache
        [inspector-type {inspector-location-id :location-id}]]
       [_ location-id]]
    (let [location (get locations location-id)
          preview-image-src (get preview-cache location-id)]
      {:display-name (:display-name location)
       :dimension (:dimension location)
       :preview-image-src preview-image-src
       :inspecting? (and (= inspector-type :location)
                         (= inspector-location-id location-id))})))
(reg-sub
  :location-map/connections
  :<- [:db-locations]
  (fn [locations]
    (letfn [(->global-pos [location-id position]
              (m/global-point position (-> location-id locations :dimension)))]
      (->> locations
        (select [ALL (collect-one FIRST) LAST :connection-triggers ALL])
        (map (fn [[from-id [from-position [to-id to-position]]]]
               (vector [from-id (->global-pos from-id from-position)]
                       [to-id (->global-pos to-id to-position)])))))))

(reg-sub
  :location-map
  :<- [:db-locations]
  :<- [:ui/positions]
  :<- [:ui/location-map-scroll-offset]
  (fn [[locations positions scroll-offset] [_ map-scale]]
    (let [scale (* config/tile-size map-scale)
          map-dimensions (->> locations
                              (mapcat (fn [[id {{:keys [w h]} :dimension}]]
                                        (let [p (positions id)]
                                          [p (translate-point p (* w scale) (* h scale))])))
                              (m/containing-rect))]
      {:dimensions (m/rect-resize
                     map-dimensions
                     {:left 200 :right 200 :top 200 :bottom 200})
       :scroll-offset scroll-offset
       :location-ids (keys locations)})))
