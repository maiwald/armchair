(ns armchair.events
  (:require [re-frame.core :as re-frame]
            [armchair.db :as db]
            [armchair.position :refer [update-drag]]))

(re-frame/reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(re-frame/reg-event-db
  :start-drag
  (fn [db [_ line-id]]
    (if-not (= line-id (get-in db [:dragging :line-id]))
      (assoc db :dragging {:line-id line-id
                           :start (:pointer db)})
      db)))

(re-frame/reg-event-db
  :end-drag
  (fn  [db _]
    (let [{:keys [line-id start]} (:dragging db)]
      (-> db
          (update-in [:lines line-id] update-drag start (:pointer db))
          (dissoc :dragging)))))

(re-frame/reg-event-db
  :move-pointer
  (fn [db [_ position]]
    (assoc db :pointer position)))
