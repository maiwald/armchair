(ns armchair.events
  (:require [re-frame.core :refer [reg-event-db]]
            [armchair.db :as db :refer [line-data]]
            [armchair.position :refer [translate-positions position-delta]]))

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  :select-line
  (fn [db [_ line-id]]
    (if-not (= (:selection db) line-id)
      (let [data (line-data (get-in db [:lines line-id]))
            form-data (merge data
                             {:id line-id :_original data})]
        (assoc db
               :selection line-id
               :line-form-data form-data))
      db)))

(reg-event-db
  :update-line-form
  (fn [db [_ field value]]
    (let [newValue (condp = field
                     :character-id (int value)
                     value)]
      (assoc-in db [:line-form-data field] newValue))))

(reg-event-db
  :save-line-form
  (fn [db _]
    (let [line-id (get-in db [:line-form-data :id])
          data (line-data (get-in db [:line-form-data]))]
      (-> db
          (update-in [:lines line-id] merge data)
          (update-in [:line-form-data :_original] merge data)))))

(reg-event-db
  :start-drag
  (fn [db [_ line-id position]]
    (if-not (contains? (get-in db [:dragging :line-ids]) line-id)
      (assoc db :dragging {:line-ids #{line-id}
                           :start position
                           :delta [0 0]})
      db)))

(reg-event-db
  :start-drag-all
  (fn [db [_ position]]
    (assoc db :dragging {:line-ids (-> db :lines keys set)
                         :start position
                         :delta [0 0]})))

(reg-event-db
  :end-drag
  (fn  [db _]
    (let [{:keys [line-ids delta]} (:dragging db)]
      (-> db
          (update :lines translate-positions line-ids delta)
          (dissoc :dragging)))))

(reg-event-db
  :move-pointer
  (fn [db [_ position]]
    (if-let [start (get-in db [:dragging :start])]
      (assoc-in db [:dragging :delta] (position-delta start position))
      db)))
