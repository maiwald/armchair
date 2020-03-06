(ns armchair.location-map.events
  (:require [armchair.events :refer [reg-event-meta]]))

(reg-event-meta
  :location-map/update-offset
  (fn [db [_ offset]]
    (assoc db :ui/location-map-scroll-offset offset)))
