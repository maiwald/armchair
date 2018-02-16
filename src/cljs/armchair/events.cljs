(ns armchair.events
  (:require [re-frame.core :refer [reg-event-db]]
            [armchair.db :as db]
            [armchair.position :refer [translate-positions position-delta]]))

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  :select-line
  (fn [db [_ line-id]]
    (if-not (= (:selected-line-id db) line-id)
      (assoc db :selected-line-id line-id)
      db)))

(reg-event-db
  :deselect-line
  (fn [db [_ line-id]]
    (dissoc db :selected-line-id)))

(reg-event-db
  :select-character
  (fn [db [_ character-id]]
    (if-not (= (:selected-character-id db) character-id)
      (assoc db :selected-character-id character-id)
      db)))

(defn new-id [db resource-type]
  (let [items (get db resource-type)]
    (+ 1 (reduce max (keys items)))))

(reg-event-db
  :create-new-character
  (fn [db]
    (let [id (new-id db :characters)
          new-character {:id id :color "black" :display-name (str "Character #" id)}]
      (update db :characters assoc id new-character))))

(reg-event-db
  :show-page
  (fn [db [_ page]]
    (assoc db :current-page page)))

(reg-event-db
  :update-line
  (fn [db [_ id field value]]
    (let [newValue (case field
                     :character-id (int value)
                     value)]
      (assoc-in db [:lines id field] newValue))))

(reg-event-db
  :update-character
  (fn [db [_ id field value]]
    (assoc-in db [:characters id field] value)))

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
