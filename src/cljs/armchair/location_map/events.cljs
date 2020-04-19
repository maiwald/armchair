(ns armchair.location-map.events
  (:require [armchair.events :refer [reg-event-meta]]
            [armchair.math :refer [round point-scale]]))

(reg-event-meta
  :location-map/update-scroll-center
  (fn [db [_ offset]]
    (assoc db :ui/location-map-scroll-center offset)))

(reg-event-meta
  :location-map/zoom-in
  (fn [db]
    (let [scale (:ui/location-map-zoom-scale db)
          new-scale (min 1.6 (round (+ scale 0.2) 1))]
      (-> db
          (assoc :ui/location-map-zoom-scale new-scale)
          (update :ui/location-map-scroll-center point-scale (/ new-scale scale))))))

(reg-event-meta
  :location-map/zoom-out
  (fn [db]
    (let [scale (:ui/location-map-zoom-scale db)
          new-scale (max 0.2 (round (- scale 0.2) 1))]
      (-> db
          (assoc :ui/location-map-zoom-scale new-scale)
          (update :ui/location-map-scroll-center point-scale (/ new-scale scale))))))
