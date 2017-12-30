(ns armchair.events
  (:require [re-frame.core :as re-frame]
            [armchair.db :as db]
            [armchair.position :refer [translate-position position-delta]]))

(re-frame/reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(re-frame/reg-event-db
  :start-drag
  (fn [db [_ line-id position]]
    (if-not (= line-id (get-in db [:dragging :line-id]))
      (assoc db :dragging {:line-id line-id
                           :start position
                           :delta [0 0]})
      db)))

(re-frame/reg-event-db
  :end-drag
  (fn  [db _]
    (let [{:keys [line-id delta]} (:dragging db)]
      (-> db
          (update-in [:lines line-id] translate-position delta)
          (dissoc :dragging)))))

(re-frame/reg-event-db
  :move-pointer
  (fn [db [_ position]]
    (if-let [start (get-in db [:dragging :start])]
      (assoc-in db [:dragging :delta] (position-delta start position))
      db)))
