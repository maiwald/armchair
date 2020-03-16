(ns armchair.location-map.events
  (:require [armchair.events :refer [reg-event-meta]]))

(reg-event-meta
  :location-map/update-offset
  (fn [db [_ offset]]
    (assoc db :ui/location-map-scroll-offset offset)))

(reg-event-meta
  :location-map/zoom-in
  (fn [db]
    (update db :ui/location-map-zoom-scale
            (fn [scale] (min 1.5 (+ scale 0.1))))))

(reg-event-meta
  :location-map/zoom-out
  (fn [db]
    (update db :ui/location-map-zoom-scale
            (fn [scale] (max 0.2 (- scale 0.1))))))
