(ns armchair.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [armchair.db :as db]
            [armchair.position :refer [translate-positions position-delta]]))

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  :reset-db
  (fn [db _]
    (merge db (select-keys db/default-db [:positions
                                          :characters
                                          :locations
                                          :lines
                                          :connections]))))

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

;; Resources

(defn new-id [db resource-type]
  (let [items (get db resource-type)]
    (+ 1 (reduce max 0 (keys items)))))

(reg-event-db
  :create-new-character
  (fn [db]
    (let [id (new-id db :characters)
          new-character {:id id :color "black" :display-name (str "Character #" id)}]
      (update db :characters assoc id new-character))))

(reg-event-db
  :delete-character
  (fn [db [_ id]]
    (if (zero? (db/line-count-for-character (:lines db) id))
      (update db :characters dissoc id)
      db)))

(reg-event-db
  :update-character
  (fn [db [_ id field value]]
    (assoc-in db [:characters id field] value)))

(reg-event-db
  :open-character-modal
  (fn [db [_ id]]
    (if-not (contains? db :modal)
      (assoc db :modal {:character-id id})
      (throw (js/Error. "Attempting to open a modal while modal is open!")))))

(reg-event-db
  :create-new-location
  (fn [db]
    (let [id (new-id db :locations)
          new-location {:id id :display-name (str "location #" id)}]
      (update db :locations assoc id new-location))))

(reg-event-db
  :delete-location
  (fn [db [_ id]]
    (update db :locations dissoc id)))

(reg-event-db
  :update-location
  (fn [db [_ id field value]]
    (assoc-in db [:locations id field] value)))

(reg-event-db
  :open-location-modal
  (fn [db [_ id]]
    (if-not (contains? db :modal)
      (assoc db :modal {:location-id id})
      (throw (js/Error. "Attempting to open a modal while modal is open!")))))

(reg-event-db
  :create-new-line
  (fn [db]
    (let [line-id (new-id db :lines)
          position-id (new-id db :positions)
          new-line {:id line-id
                    :character-id nil
                    :dialogue-id (:selected-dialogue-id db)
                    :position-id position-id
                    :text (str "Line #" line-id)}]
      (-> db
          (assoc-in [:lines line-id] new-line)
          (assoc-in [:positions position-id] [20 20])))))

(reg-event-db
  :update-line
  (fn [db [_ id field value]]
    (let [newValue (case field
                     :character-id (int value)
                     value)]
      (assoc-in db [:lines id field] newValue))))

;; Page

(reg-event-db
  :close-modal
  (fn [db [_ modal-fn | args]]
    (dissoc db :modal)))

(reg-event-db
  :show-page
  (fn [db [_ page]]
    (assoc db :current-page page)))

;; Mouse, Drag & Drop

(reg-event-db
  :start-connection
  (fn [db [_ line-id position]]
    (if-not (= (get-in db [:dragging :connection-start]) line-id)
      (assoc db :dragging {:connection-start line-id
                           :start position
                           :delta [0 0]})
      db)))

(reg-event-db
  :start-drag
  (fn [db [_ position-ids position]]
    (if-not (= position-ids (get-in db [:dragging :position-ids]))
      (assoc db :dragging {:position-ids position-ids
                           :start position
                           :delta [0 0]})
      db)))

(defn is-click? [delta]
  (every? #(> 2 (.abs js/Math %)) delta))

(reg-event-fx
  :end-drag-line
  (fn [{:keys [db]} [_ line-id]]
    (let [{:keys [position-ids connection-start delta]} (:dragging db)
          new-db (-> db
                     (update :positions translate-positions position-ids delta)
                     (dissoc :dragging))]
      (cond
        (is-click? delta) {:db new-db
                           :dispatch [:select-line line-id]}
        (not= line-id connection-start) {:db (update new-db :connections conj [connection-start line-id])}
        :else {:db new-db}))))

(reg-event-fx
  :end-drag-all
  (fn [{:keys [db]} _]
    (let [{:keys [position-ids delta]} (:dragging db)]
      (merge
        {:db (-> db
                 (update :positions translate-positions position-ids delta)
                 (dissoc :dragging))}
        (when (is-click? delta) {:dispatch [:deselect-line]})))))

(reg-event-db
  :move-pointer
  (fn [db [_ position]]
    (if-let [start (get-in db [:dragging :start])]
      (assoc-in db [:dragging :delta] (position-delta start position))
      db)))
