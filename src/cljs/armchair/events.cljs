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
    (merge db (select-keys db/default-db
                           [:characters :locations :lines :connections]))))

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
    (let [id (new-id db :lines)
          new-line {:id id
                    :character-id nil
                    :dialogue-id (:selected-dialogue-id db)
                    :position [20 20]
                    :text (str "Line #" id)}]
      (update db :lines assoc id new-line))))

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
  (fn [db [_ line-id position]]
    (if-not (contains? (get-in db [:dragging :line-ids]) line-id)
      (assoc db :dragging {:line-ids #{line-id}
                           :start position
                           :delta [0 0]})
      db)))

(reg-event-fx
  :end-drag
  (fn [{:keys [db]} [_ line-id]]
    (let [{:keys [line-ids connection-start delta]} (:dragging db)
          is-click? (= [0 0] delta)]
      (if (= line-ids #{line-id})
        (merge
          {:db (-> db
                   (update :lines translate-positions line-ids delta)
                   (dissoc :dragging))}
          (when is-click?
            {:dispatch [:select-line line-id]}))
        {:db (-> db
                 (update :connections conj [connection-start line-id])
                 (dissoc :dragging))}))))

(reg-event-db
  :start-drag-all
  (fn [db [_ position]]
    (let [all-line-ids (-> db :lines keys set)]
      (if-not (= all-line-ids (get-in db [:dragging :line-ids]))
        (assoc db :dragging {:line-ids all-line-ids
                             :start position
                             :delta [0 0]})
        db))))

(reg-event-fx
  :end-drag-all
  (fn [{:keys [db]} _]
    (let [{:keys [line-ids delta]} (:dragging db)
          is-click? (= [0 0] delta)]
      (merge
        {:db (-> db
                 (update :lines translate-positions line-ids delta)
                 (dissoc :dragging))}
        (when is-click? {:dispatch [:deselect-line]})))))

(reg-event-db
  :move-pointer
  (fn [db [_ position]]
    (if-let [start (get-in db [:dragging :start])]
      (assoc-in db [:dragging :delta] (position-delta start position))
      db)))
