(ns armchair.events
  (:require [re-frame.core :as re-frame]
            [armchair.db :as db]
            [armchair.position :refer [translate-positions position-delta]]))

(re-frame/reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(re-frame/reg-event-db
  :start-drag
  (fn [db [_ line-id position]]
    (if-not (contains? (get-in db [:dragging :line-ids]) line-id)
      (assoc db :dragging {:line-ids #{line-id}
                           :start position
                           :delta [0 0]})
      db)))

(re-frame/reg-event-db
  :end-drag
  (fn  [db _]
    (let [{:keys [line-ids delta]} (:dragging db)]
      (-> db
          (update :lines translate-positions line-ids delta)
          (dissoc :dragging)))))

(re-frame/reg-event-db
  :move-pointer
  (fn [db [_ position]]
    (if-let [start (get-in db [:dragging :start])]
      (assoc-in db [:dragging :delta] (position-delta start position))
      db)))
