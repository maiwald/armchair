(ns armchair.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [clojure.set :refer [difference]]
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

;; Resources

(defn new-id [db resource-type]
  (let [items (get db resource-type)]
    (+ 1 (reduce max 0 (keys items)))))

(defn with-new-position [func]
  (fn [db]
    (let [position-id (new-id db :positions)]
      (func (assoc-in db [:positions position-id] [20 20])
            position-id))))

(reg-event-db
  :create-character
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
  :create-location
  (with-new-position
    (fn [db position-id]
      (let [id (new-id db :locations)]
        (assoc-in db [:locations id] {:id id
                                      :display-name (str "location #" id)
                                      :position-id position-id})))))

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
  :create-line
  (with-new-position
    (fn [db position-id]
      (let [id (new-id db :lines)]
        (assoc-in db [:lines id] {:id id
                                  :character-id nil
                                  :dialogue-id (:selected-dialogue-id db)
                                  :position-id position-id
                                  :text (str "Line #" id)})))))

(reg-event-db
  :update-line
  (fn [db [_ id field value]]
    (let [newValue (case field
                     :character-id (int value)
                     value)]
      (assoc-in db [:lines id field] newValue))))

(reg-event-db
  :delete-line
  (fn [db [_ id]]
    (let [line-connections (filter (fn [[start end]] (or (= start id)
                                                         (= end id)))
                                   (:connections db))]
      (if (or (empty? line-connections)
              ^boolean (.confirm js/window (str "Really delete Line #" id "?")))
        (-> db
            (update :lines dissoc id)
            (update :connections difference line-connections))
        db))))

(reg-event-db
  :open-line-modal
  (fn [db [_ id]]
    (if-not (contains? db :modal)
      (assoc db :modal {:line-id id})
      (throw (js/Error. "Attempting to open a modal while modal is open!")))))

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

(defn dragging? [dragging]
  (and dragging
       (every? #(> 2 (.abs js/Math %)) (:delta dragging))))

(reg-event-db
  :move-pointer
  (fn [db [_ position]]
    (if-let [start (get-in db [:dragging :start])]
      (assoc-in db [:dragging :delta] (position-delta start position))
      db)))

(reg-event-db
  :start-connection
  (fn [db [_ line-id position]]
    (if-not (= (get-in db [:dragging :connection-start]) line-id)
      (assoc db :dragging {:connection-start line-id
                           :start position
                           :delta [0 0]})
      db)))

(reg-event-db
  :end-connection
  (fn [db [_ end-id]]
    (if-let [start-id (get-in db [:dragging :connection-start])]
      (if-not (= start-id end-id)
        (update db :connections conj [start-id end-id])
        db)
      db)))

(reg-event-db
  :start-drag
  (fn [db [_ position-ids position]]
    (if-not (= position-ids (get-in db [:dragging :position-ids]))
      (assoc db :dragging {:position-ids position-ids
                           :start position
                           :delta [0 0]})
      db)))

(reg-event-db
  :end-drag
  (fn [db _]
    (if-let [{:keys [delta position-ids]} (:dragging db)]
      (-> db
          (update :positions translate-positions position-ids delta)
          (dissoc :dragging)))))
