(ns armchair.location-map.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [com.rpl.specter
             :refer [collect-one ALL FIRST LAST MAP-VALS]
             :refer-macros [select]]
            [armchair.config :as config]
            [armchair.util :as u]
            [armchair.math :as m :refer [Point translate-point point-delta]]))

(reg-sub
  :location-map
  :<- [:db-locations]
  :<- [:ui/positions]
  :<- [:ui/location-map-scroll-offset]
  (fn [[locations positions scroll-offset] [_ map-scale]]
    (let [connections (->> locations
                           (select [ALL (collect-one FIRST) LAST :connection-triggers MAP-VALS FIRST])
                           (map set)
                           distinct
                           (map sort))
          scale (* config/tile-size map-scale)
          positions (mapcat (fn [[id {{:keys [w h]} :dimension}]]
                              (let [p (positions id)]
                                [p (translate-point p (* w scale) (* h scale))]))
                            locations)
          dimensions (m/rect-resize
                       (m/containing-rect positions)
                       {:left 200
                        :right 200
                        :top 200
                        :bottom 200})]
      {:dimensions dimensions
       :scroll-offset scroll-offset
       :location-ids (keys locations)
       :connections connections})))

(reg-sub
  :location-map/location
  :<- [:db-locations]
  :<- [:ui/location-preview-cache]
  (fn [[locations preview-cache] [_ location-id]]
    (let [location (get locations location-id)
          preview-image-src (get preview-cache location-id)]
      {:display-name (:display-name location)
       :dimension (:dimension location)
       :preview-image-src preview-image-src})))


