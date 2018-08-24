(ns armchair.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx after]]
            [clojure.set :refer [difference]]
            [clojure.spec.alpha :as s]
            [armchair.db :as db]
            [armchair.util :refer [translate-positions position-delta]]))

(def spec-interceptor (after (fn [db]
                               (when-not (s/valid? :armchair.db/state db)
                                 (let [explain (s/explain-data :armchair.db/state db)]
                                   (.log js/console (:cljs.spec.alpha/problems explain)))))))

(reg-event-db
  :initialize-db
  [spec-interceptor]
  (fn [_ _]
    db/default-db))

(reg-event-db
  :reset-db
  [spec-interceptor]
  (fn [db _]
    (merge db (select-keys db/default-db [:positions
                                          :characters
                                          :dialogues
                                          :locations
                                          :location-connections
                                          :lines
                                          :line-connections]))))

;; Resources

(defn new-id [db resource-type]
  (let [items (get db resource-type)]
    (+ 1 (reduce max 0 (keys items)))))

(defn with-new-position [func]
  (fn [db ctx]
    (let [position-id (new-id db :positions)]
      (func (assoc-in db [:positions position-id] [20 20])
            position-id
            ctx))))

(reg-event-db
  :create-character
  [spec-interceptor]
  (fn [db]
    (let [id (new-id db :characters)
          new-character {:id id :color "black" :display-name (str "Character #" id)}]
      (update db :characters assoc id new-character))))

(reg-event-db
  :delete-character
  [spec-interceptor]
  (fn [db [_ id]]
    (if (zero? (db/line-count-for-character (:lines db) id))
      (update db :characters dissoc id)
      db)))

(reg-event-db
  :update-character
  [spec-interceptor]
  (fn [db [_ id field value]]
    (assoc-in db [:characters id field] value)))

(reg-event-db
  :open-character-modal
  [spec-interceptor]
  (fn [db [_ id]]
    (assert (not (contains? db :modal))
            "Attempting to open a modal while modal is open!")
    (assoc db :modal {:character-id id})))

(reg-event-db
  :create-location
  [spec-interceptor]
  (with-new-position
    (fn [db position-id]
      (let [id (new-id db :locations)]
        (assoc-in db [:locations id] {:id id
                                      :display-name (str "location #" id)
                                      :position-id position-id})))))

(reg-event-db
  :delete-location
  [spec-interceptor]
  (fn [db [_ id]]
    (let [location-connections (filter #(contains? % id)
                                       (:location-connections db))]
      (if (or (empty? location-connections)
              ^boolean (.confirm js/window (str "Really delete location #" id "?")))
        (-> db
            (update :locations dissoc id)
            (update :location-connections difference location-connections))
        db))))

(reg-event-db
  :update-location
  [spec-interceptor]
  (fn [db [_ id field value]]
    (assoc-in db [:locations id field] value)))

(reg-event-db
  :open-location-modal
  [spec-interceptor]
  (fn [db [_ id]]
    (assert (not (contains? db :modal))
            "Attempting to open a modal while modal is open!")
    (assoc db :modal {:location-id id})))

(reg-event-db
  :create-line
  [spec-interceptor]
  (with-new-position
    (fn [db position-id [_ dialogue-id]]
      (let [id (new-id db :lines)]
        (assoc-in db [:lines id] {:id id
                                  :character-id nil
                                  :dialogue-id dialogue-id
                                  :position-id position-id
                                  :text nil})))))

(reg-event-db
  :update-line
  [spec-interceptor]
  (fn [db [_ id field value]]
    (let [newValue (case field
                     :character-id (int value)
                     value)]
      (assoc-in db [:lines id field] newValue))))

(reg-event-db
  :delete-line
  [spec-interceptor]
  (fn [db [_ id]]
    (let [line-connections (filter #(contains? (set %) id)
                                   (:line-connections db))]
      (if (or (empty? line-connections)
              ^boolean (.confirm js/window (str "Really delete Line #" id "?")))
        (-> db
            (update :lines dissoc id)
            (update :line-connections difference line-connections))
        db))))

(reg-event-db
  :open-line-modal
  [spec-interceptor]
  (fn [db [_ id]]
    (assert (not (contains? db :modal))
            "Attempting to open a modal while modal is open!")
    (assoc db :modal {:line-id id})))

;; Page

(reg-event-db
  :close-modal
  [spec-interceptor]
  (fn [db [_ modal-fn | args]]
    (dissoc db :modal)))

(reg-event-db
  :show-page
  [spec-interceptor]
  (fn [db [_ page payload]]
    (if payload
      (assoc db :current-page {:name page :payload payload})
      (assoc db :current-page {:name page}))))

;; Mouse, Drag & Drop

(reg-event-db
  :move-pointer
  [spec-interceptor]
  (fn [db [_ position]]
    (assoc db :pointer position)))

(reg-event-db
  :start-connecting-lines
  [spec-interceptor]
  (fn [db [_ line-id position]]
    (assert (not (contains? db :connecting))
            "Attempting to start connecting lines while already in progress!")
    (assoc db
           :connecting {:start-position position
                        :line-id line-id}
           :pointer position)))

(reg-event-db
  :end-connecting-lines
  [spec-interceptor]
  (fn [db [_ end-id]]
    (assert (some? (:connecting db))
            "Attempting to end connecting while not in progress!")
    (let [start-id (get-in db [:connecting :line-id])
          new-db (dissoc db :connecting :pointer)]
      (if-not (= start-id end-id)
        (update new-db :line-connections conj [start-id end-id])
        new-db))))

(reg-event-db
  :start-connecting-locations
  [spec-interceptor]
  (fn [db [_ location-id position]]
    (assert (not (contains? db :connecting))
            "Attempting to start connecting locations while already in progress!")
    (assoc db
           :connecting {:start-position position
                        :location-id location-id}
           :pointer position)))

(reg-event-db
  :end-connecting-locations
  [spec-interceptor]
  (fn [db [_ end-id]]
    (assert (some? (:connecting db))
            "Attempting to end connecting while not in progress!")
    (let [start-id (get-in db [:connecting :location-id])
          new-db (dissoc db :connecting :pointer)]
      (if-not (= start-id end-id)
        (update new-db :location-connections conj #{start-id end-id})
        new-db))))


(reg-event-db
  :abort-connecting
  [spec-interceptor]
  (fn [db _] (dissoc db :connecting)))

(reg-event-db
  :start-dragging
  [spec-interceptor]
  (fn [db [_ position-ids position]]
    (assert (not (contains? db :dragging))
            "Attempting to start drag while already in progress!")
    (assoc db
           :dragging {:position-ids position-ids
                      :start-position position}
           :pointer position)))

(reg-event-db
  :end-dragging
  [spec-interceptor]
  (fn [{:keys [dragging pointer] :as db} _]
    (assert (some? dragging)
            "Attempting to end drag while not in progress!")
    (let [{:keys [start-position position-ids]} dragging
          delta (position-delta start-position pointer)]
      (-> db
          (update :positions translate-positions position-ids delta)
          (dissoc :dragging :pointer)))))
