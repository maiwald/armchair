(ns armchair.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx after]]
            [clojure.set :refer [difference]]
            [clojure.spec.alpha :as s]
            [armchair.db :as db]
            [armchair.routes :refer [routes]]
            [armchair.util :refer [map-values translate-positions position-delta]]))

(def spec-interceptor (after (fn [db]
                               (when-not (s/valid? :armchair.db/state db)
                                 (let [explain (s/explain-data :armchair.db/state db)]
                                   (js/console.log (:cljs.spec.alpha/problems explain)))))))

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
                                          :lines]))))

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

;; Character CRUD

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
    (cond-> db
      (zero? (db/line-count-for-character (:lines db) id))
      (update :characters dissoc id))))

(reg-event-db
  :update-character
  [spec-interceptor]
  (fn [db [_ id field value]]
    (assoc-in db [:characters id field] value)))

(reg-event-db
  :create-location
  [spec-interceptor]
  (with-new-position
    (fn [db position-id]
      (let [id (new-id db :locations)]
        (assoc-in db [:locations id] {:id id
                                      :display-name (str "location #" id)
                                      :position-id position-id})))))

;; Location CRUD

(reg-event-db
  :delete-location
  [spec-interceptor]
  (fn [db [_ id]]
    (let [location-connections (filter #(contains? % id)
                                       (:location-connections db))]
      (cond-> db
        (empty? location-connections)
        (-> (update :locations dissoc id)
            (update :location-connections difference location-connections))))))

(reg-event-db
  :update-location
  [spec-interceptor]
  (fn [db [_ id field value]]
    (assoc-in db [:locations id field] value)))

;; Line CRUD

(reg-event-db
  :create-npc-line
  [spec-interceptor]
  (with-new-position
    (fn [db position-id [_ dialogue-id]]
      (let [id (new-id db :lines)]
        (assoc-in db [:lines id] {:id id
                                  :kind :npc
                                  :character-id nil
                                  :dialogue-id dialogue-id
                                  :position-id position-id
                                  :text nil
                                  :next-line-id nil})))))

(reg-event-db
  :create-player-line
  [spec-interceptor]
  (with-new-position
    (fn [db position-id [_ dialogue-id]]
      (let [id (new-id db :lines)]
        (assoc-in db [:lines id] {:id id
                                  :kind :player
                                  :dialogue-id dialogue-id
                                  :position-id position-id
                                  :options []})))))

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
    (assert (not (contains? (->> db :dialogues vals (map :initial-line-id) set) id))
            "Initial lines cannot be deleted!")
    (letfn [(clear-line [line]
              (update line :next-line-id #(if (= id %) nil %)))
            (clear-options [line]
              (update line :options #(mapv clear-line %)))]
      (update db :lines #(map-values (fn [line]
                                       (case (:kind line)
                                         :npc (clear-line line)
                                         :player (clear-options line)))
                                     (dissoc % id))))))

(reg-event-db
  :move-option
  [spec-interceptor]
  (fn [db [_ line-id s-index direction]]
    (let [t-index (case direction
                    :up (dec s-index)
                    :down (inc s-index))
          swap-option (fn [options]
                        (replace {(get options s-index) (get options t-index)
                                  (get options t-index) (get options s-index)}
                                 options))]
      (update-in db [:lines line-id :options] swap-option))))

(reg-event-db
  :add-option
  [spec-interceptor]
  (fn [db [_ line-id]]
    (update-in db [:lines line-id :options] conj {:text ""
                                                  :next-line-id nil})))

(reg-event-db
  :update-option
  [spec-interceptor]
  (fn [db [_ line-id index text]]
    (assoc-in db [:lines line-id :options index :text] text)))

(reg-event-db
  :delete-option
  [spec-interceptor]
  (fn [db [_ line-id index]]
    (update-in db [:lines line-id :options] (fn [v] (vec (concat (take index v)
                                                                 (drop (inc index) v)))))))

(reg-event-db
  :set-infos
  [spec-interceptor]
  (fn [db [_ line-id info-ids]]
    (assert (= :npc (get-in db [:lines line-id :kind]))
            "Infos can only be set on NPC lines!")
    (assoc-in db [:lines line-id :info-ids] (set info-ids))))

(reg-event-db
  :set-required-info
  [spec-interceptor]
  (fn [db [_ line-id index info-ids]]
    (assert (= :player (get-in db [:lines line-id :kind]))
            "Required infos can only be set on player options!")
    (assoc-in db [:lines line-id :options index :required-info-ids] (set info-ids))))

;; Info CRUD

(reg-event-db
  :create-info
  [spec-interceptor]
  (fn [db]
    (let [id (new-id db :infos)]
      (update db :infos assoc id {:id id :text ""}))))

(reg-event-db
  :delete-info
  [spec-interceptor]
  (fn [db [_ id]]
    (update db :infos dissoc id)))

(reg-event-db
  :update-info
  [spec-interceptor]
  (fn [db [_ id text]]
    (assoc-in db [:infos id :description] text)))

;; Modal

(defn open-modal [modal-key]
  (fn [db [_ payload]]
    (assert (not (contains? db :modal))
            "Attempting to open a modal while modal is open!")
    (assoc db :modal {modal-key payload})))

(reg-event-db
  :open-character-modal
  [spec-interceptor]
  (open-modal :character-id))

(reg-event-db
  :open-info-modal
  [spec-interceptor]
  (open-modal :info-id))

(reg-event-db
  :open-npc-line-modal
  [spec-interceptor]
  (open-modal :npc-line-id))

(reg-event-db
  :open-player-line-modal
  [spec-interceptor]
  (open-modal :player-line-id))

;; Page

(reg-event-db
  :close-modal
  [spec-interceptor]
  (fn [db [_ modal-fn | args]]
    (dissoc db :modal)))

(reg-event-db
  :show-page
  [spec-interceptor]
  (fn [db [_ path]]
    (assoc db :current-page path)))

;; Mouse, Drag & Drop

(reg-event-db
  :move-pointer
  [spec-interceptor]
  (fn [db [_ position]]
    (assoc db :pointer position)))

(reg-event-db
  :start-connecting-lines
  [spec-interceptor]
  (fn [db [_ line-id position index]]
    (assert (not (contains? db :connecting))
            "Attempting to start connecting lines while already in progress!")
    (assoc db
           :connecting (cond-> {:start-position position
                                :line-id line-id}
                         (some? index) (assoc :index index))
           :pointer position)))

(reg-event-db
  :end-connecting-lines
  [spec-interceptor]
  (fn [db [_ end-id]]
    (assert (s/valid? :armchair.db/connecting-lines (:connecting db))
            "Attempting to end connecting with missing or invalid state!")
    (let [start-id (get-in db [:connecting :line-id])
          id-path (if-let [index (get-in db [:connecting :index])]
                    [:lines start-id :options index :next-line-id]
                    [:lines start-id :next-line-id])]
      (cond-> (dissoc db :connecting :pointer)
        (not= start-id end-id) (assoc-in id-path end-id)))))

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
