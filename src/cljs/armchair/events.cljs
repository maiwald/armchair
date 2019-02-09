(ns armchair.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx after]]
            [clojure.set :refer [difference]]
            [clojure.string :refer [blank?]]
            [armchair.config :as config]
            [clojure.spec.alpha :as s]
            cljsjs.filesaverjs
            [armchair.db :as db :refer [default-db
                                        content-data
                                        serialize-db
                                        deserialize-db
                                        migrate]]
            [armchair.undo :refer [record-undo]]
            [armchair.routes :refer [routes]]
            [armchair.util :as u]))

(def validate
  (after (fn [db]
           (when-not (s/valid? :armchair.db/state db)
             (let [explain (s/explain-data :armchair.db/state db)]
               (js/console.log (:cljs.spec.alpha/problems explain)))))))

;; Initializer

(reg-event-db
  :initialize-db
  [validate]
  (fn [_ _] default-db))

(reg-event-db
  :reset-db
  [validate
   record-undo]
  (fn [db]
    (merge db (content-data default-db))))

(reg-event-fx
  :download-state
  (fn [{db :db}]
    (let [mime-type (str "application/json;charset="
                         (.-characterSet js/document))
          blob (new js/Blob
                    (clj->js [(serialize-db db)])
                    (clj->js {:type mime-type}))
          filename (str "armchair-save-"
                        (.toISOString (new js/Date))
                        ".json")]
      (js/saveAs blob filename))
    {}))

(reg-event-db
  :upload-state
  [validate
   record-undo]
  (fn [db [_ json]]
    (merge db (migrate (deserialize-db json)))))

;; Character CRUD

(reg-event-db
  :delete-character
  [validate
   record-undo]
  (fn [db [_ id]]
    (let [line-count (->> (:lines db)
                          (u/filter-map #(= (:character-id %) id))
                          count)]
      (cond-> db
        (zero? line-count)
        (update :characters dissoc id)))))

;; Location CRUD

(reg-event-db
  :delete-location
  [validate
   record-undo]
  (fn [db [_ id]]
    (let [location-connections (filter #(contains? % id)
                                       (:location-connections db))
          location-dialogue-ids (->> (:dialogues db)
                                     (u/where-map :location-id id)
                                     keys)
          connected-location-ids (->> (:locations db)
                                      (u/filter-map #(contains? (-> % :connection-triggers vals set) id))
                                      keys)]
      (-> db
          (update :locations dissoc id)
          (update :location-connections difference location-connections)
          (u/update-in-map :locations connected-location-ids update :connection-triggers (fn [cts] (u/filter-map #(not= id %) cts)))
          (u/update-in-map :dialogues location-dialogue-ids dissoc :location-id :location-position)))))

;; Line CRUD

(reg-event-db
  :move-option
  [validate
   record-undo]
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
  [validate
   record-undo]
  (fn [db [_ line-id]]
    (let [option-id (random-uuid)]
      (-> db
          (update-in [:lines line-id :options] conj option-id)
          (assoc-in [:player-options option-id] {:entity/id option-id
                                                 :entity/type :player-option
                                                 :text ""
                                                 :next-line-id nil})))))

(reg-event-db
  :update-option
  [validate
   record-undo]
  (fn [db [_ line-id index text]]
    (let [option-id (get-in db [:lines line-id :options index])]
      (assoc-in db [:player-options option-id :text] text))))

(reg-event-db
  :delete-option
  [validate
   record-undo]
  (fn [db [_ line-id index]]
    (let [option-id (get-in db [:lines line-id :options index])]
      (-> db
          (update-in [:lines line-id :options] #(u/removev % index))
          (update :player-options dissoc option-id)))))

;; Modal

(defn assert-no-open-modal [db]
  (assert (not (contains? db :modal))
          "Attempting to open a modal while modal is open!"))

(defn assert-switch-modal [db]
  (assert (contains? (:modal db) :switch-form)
          "No switch form open. Cannot set value!"))

(reg-event-db
  :modal/open-switch-modal
  [validate]
  (fn [db [_ id]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :switch-form]
              (if-let [{:keys [display-name value-ids]} (get-in db [:switches id])]
                {:switch-id id
                 :display-name display-name
                 :values (mapv (:switch-values db) value-ids)}
                {:display-name ""
                 :values (vector {:entity/id (random-uuid)
                                  :entity/type :switch-value
                                  :display-name "ON"}
                                 {:entity/id (random-uuid)
                                  :entity/type :switch-value
                                  :display-name "OFF"})}))))

(reg-event-db
  :modal/update-switch-name
  [validate]
  (fn [db [_ value]]
    (assert-switch-modal db)
    (assoc-in db [:modal :switch-form :display-name] value)))

(reg-event-db
  :modal/update-switch-value
  [validate]
  (fn [db [_ index value]]
    (assert-switch-modal db)
    (assoc-in db [:modal :switch-form :values index :display-name] value)))

(reg-event-db
  :modal/add-switch-value
  [validate]
  (fn [db _]
    (assert-switch-modal db)
    (let [value-id (random-uuid)]
      (-> db
        (update-in [:modal :switch-form :values] conj {:entity/id (random-uuid)
                                                       :entity/type :switch-value
                                                       :display-name ""})))))

(reg-event-db
  :modal/save-switch
  [validate
   record-undo]
  (fn [db _]
    (assert-switch-modal db)
    (let [modal (get-in db [:modal :switch-form])
          {:keys [switch-id display-name values]} modal
          id (or switch-id (random-uuid))
          value-map (into {} (map #(vector (:entity/id %) %) values))]
      (cond-> db
        (s/valid? :modal/switch-form modal)
        (-> (dissoc :modal)
            (assoc-in [:switches id]
                      {:entity/id id
                       :entity/type :switch
                       :display-name display-name
                       :value-ids (vec (keys value-map))})
            (update :switch-values merge value-map))))))

(defn assert-trigger-modal [db]
  (assert (contains? (:modal db) :trigger-creation)
          "No trigger creation initiated. Cannot set value!"))

(reg-event-db
  :modal/open-trigger-creation
  [validate]
  (fn [db [_ node-id]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :trigger-creation]
              {:trigger-node-id node-id
               :switch-kind :dialogue-state})))

(reg-event-db
  :modal/update-trigger-kind
  [validate]
  (fn [db [_ kind]]
    (assert-trigger-modal db)
    (update-in db [:modal :trigger-creation]
               (fn [t] (-> t
                           (assoc :switch-kind kind)
                           (dissoc :switch-id :switch-value))))))

(reg-event-db
  :modal/update-trigger-switch-id
  [validate]
  (fn [db [_ id]]
    (assert-trigger-modal db)
    (update-in db [:modal :trigger-creation]
               (fn [t] (-> t
                           (assoc :switch-id id)
                           (dissoc :switch-value))))))

(reg-event-db
  :modal/update-trigger-value
  [validate]
  (fn [db [_ value]]
    (assert-trigger-modal db)
    (assoc-in db [:modal :trigger-creation :switch-value] value)))

(reg-event-db
  :modal/save-trigger
  [validate
   record-undo]
  (fn [db]
    (assert-trigger-modal db)
    (let [trigger-id (random-uuid)
          {:keys [trigger-node-id switch-kind switch-id switch-value]} (get-in db [:modal :trigger-creation])
          trigger {:entity/id trigger-id
                   :entity/type :trigger
                   :switch-kind switch-kind
                   :switch-id switch-id
                   :switch-value switch-value}]
      (cond-> db
        (s/valid? :armchair.db/trigger trigger)
        (-> (update-in [:lines trigger-node-id :trigger-ids]
                       (fn [ts] (conj (vec ts) trigger-id)))
            (assoc-in [:triggers trigger-id] trigger)
            (dissoc :modal))))))

(reg-event-db
  :open-character-modal
  [validate]
  (fn [db [_ id]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :character-form]
      (if-let [{:keys [display-name color texture]} (get-in db [:characters id])]
        {:id id
         :display-name display-name
         :texture texture
         :color color}
        {:display-name ""
         :color (rand-nth config/color-grid)}))))

(defn assert-character-modal [db]
  (assert (contains? (:modal db) :character-form)
          "No character creation initiated. Cannot set value!"))

(reg-event-db
  :character-form/update
  [validate
   record-undo]
  (fn [db [_ field value]]
    (assert-character-modal db)
    (assoc-in db [:modal :character-form field] value)))

(reg-event-db
  :character-form/save
  [validate
   record-undo]
  (fn [db]
    (assert-character-modal db)
    (let [{:keys [id display-name texture color]} (get-in db [:modal :character-form])
          id (or id (random-uuid))
          character {:entity/id id
                     :entity/type :character
                     :display-name display-name
                     :texture texture
                     :color color}]
      (cond-> db
        (and (s/valid? :armchair.db/character character)
             (some? texture))
        (-> (assoc-in [:characters id] character)
            (dissoc :modal))))))

(reg-event-db
  :open-location-creation
  [validate]
  (fn [db]
    (assert-no-open-modal db)
    (assoc-in db [:modal :location-creation] "")))

(defn assert-location-creation-modal [db]
  (assert (contains? (:modal db) :location-creation)
          "No Location creation modal present. Cannot create!"))

(reg-event-db
  :update-location-creation-name
  [validate]
  (fn [db [_ display-name]]
    (assert-location-creation-modal db)
    (assoc-in db [:modal :location-creation] display-name)))

(reg-event-db
  :create-location
  [validate
   record-undo]
  (fn [db]
    (assert-location-creation-modal db)
    (let [id (random-uuid)
          display-name (get-in db [:modal :location-creation])]
      (-> db
          (dissoc :modal)
          (assoc-in [:ui/positions id] config/default-ui-position)
          (assoc-in [:locations id] {:entity/id id
                                     :entity/type :location
                                     :dimension [[0 0] [2 2]]
                                     :background {}
                                     :walk-set #{}
                                     :connection-triggers {}
                                     :display-name display-name})))))

(reg-event-db
  :open-npc-line-modal
  [validate]
  (fn [db [_ payload]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :npc-line-id] payload)))

(reg-event-db
  :open-player-line-modal
  [validate]
  (fn [db [_ payload]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :player-line-id] payload)))

(reg-event-db
  :open-dialogue-creation-modal
  [validate]
  (fn [db _]
    (assert-no-open-modal db)
    (assoc-in db [:modal :dialogue-creation] {:character-id nil
                                              :synopsis nil})))
;; Dialogue state modal

(reg-event-db
  :open-dialogue-state-modal
  [validate]
  (fn [db [_ line-id]]
    (assert-no-open-modal db)
    (let [dialogue-id (get-in db [:lines line-id :dialogue-id])
          description (get-in db [:dialogues dialogue-id :states line-id])]
      (assoc-in db [:modal :dialogue-state] {:line-id line-id
                                             :description description}))))

(defn assert-dialogue-state-modal [db]
  (assert (contains? (:modal db) :dialogue-state)
          "No dialogue state modal open. Cannot set value!"))

(reg-event-db
  :dialogue-state-update
  [validate]
  (fn [db [_ description]]
    (assert-dialogue-state-modal db)
    (assoc-in db [:modal :dialogue-state :description] description)))

(reg-event-db
  :create-dialogue-state
  [validate
   record-undo]
  (fn [db]
    (assert-dialogue-state-modal db)
    (let [{:keys [line-id description]} (get-in db [:modal :dialogue-state])
          dialogue-id (get-in db [:lines line-id :dialogue-id])]
      (cond-> (dissoc db :modal)
        (not-empty description)
        (update-in [:dialogues dialogue-id :states] assoc line-id description)))))

;; Dialogue CRUD

(defn assert-dialogue-creation-modal [db]
  (assert (contains? (:modal db) :dialogue-creation)
          "No dialogue creation initiated. Cannot set value!"))

(reg-event-db
  :dialogue-creation-update
  [validate]
  (fn [db [_ field value]]
    (assert-dialogue-creation-modal db)
    (assoc-in db [:modal :dialogue-creation field] value)))

(reg-event-db
  :create-dialogue
  [validate
   record-undo]
  (fn [db]
    (assert-dialogue-creation-modal db)
    (let [dialogue-id (random-uuid)
          line-id (random-uuid)
          modal-data (get-in db [:modal :dialogue-creation])]
      (if (or (blank? (:character-id modal-data))
              (blank? (:synopsis modal-data)))
        db
        (-> db
            (assoc-in [:dialogues dialogue-id] (merge modal-data
                                                      {:entity/id dialogue-id
                                                       :entity/type :dialogue
                                                       :initial-line-id line-id}))
            (assoc-in [:ui/positions line-id] config/default-ui-position)
            (assoc-in [:lines line-id] {:entity/id line-id
                                        :entity/type :line
                                        :kind :npc
                                        :character-id (:character-id modal-data)
                                        :dialogue-id dialogue-id
                                        :text nil
                                        :next-line-id nil})
            (dissoc :modal))))))

(reg-event-db
  :delete-dialogue
  [validate
   record-undo]
  (fn [db [_ dialogue-id]]
    (let [dialogue-states (get-in db [:dialogues dialogue-id :states])]
      (-> (loop [db db
                 [state-id & state-ids] (keys dialogue-states)]
            (if (nil? state-id)
              db
              (recur (db/clear-dialogue-state db state-id)
                     state-ids)))
          (update :dialogues dissoc dialogue-id)
          (update :lines #(u/filter-map (fn [{id :dialogue-id}] (not= id dialogue-id)) %))))))

;; Page

(reg-event-db
  :close-modal
  [validate]
  (fn [db [_ modal-fn | args]]
    (dissoc db :modal)))

(reg-event-db
  :show-page
  [validate]
  (fn [db [_ path]]
    (assoc db :current-page path)))

;; Mouse, Drag & Drop

(reg-event-db
  :move-cursor
  [validate]
  (fn [db [_ cursor]]
    (assoc db :cursor cursor)))

(reg-event-db
  :start-connecting-locations
  [validate]
  (fn [db [_ location-id cursor]]
    (assert (not (contains? db :connecting))
            "Attempting to start connecting locations while already in progress!")
    (assoc db
           :connecting {:cursor-start cursor
                        :location-id location-id}
           :cursor cursor)))

(reg-event-db
  :end-connecting-locations
  [validate
   record-undo]
  (fn [db [_ end-id]]
    (assert (some? (:connecting db))
            "Attempting to end connecting while not in progress!")
    (let [start-id (get-in db [:connecting :location-id])
          new-db (dissoc db :connecting :cursor)]
      (if-not (= start-id end-id)
        (update new-db :location-connections conj #{start-id end-id})
        new-db))))


(reg-event-db
  :abort-connecting
  [validate]
  (fn [db] (dissoc db :connecting)))

(reg-event-db
  :start-dragging
  [validate]
  (fn [db [_ ids cursor]]
    (cond-> db
      (not (contains? db :dragging)) (assoc :dragging {:ids ids
                                                       :cursor-start cursor}
                                            :cursor cursor))))

(reg-event-db
  :end-dragging
  [validate
   record-undo]
  (fn [{:keys [dragging cursor] :as db}]
    (assert (some? dragging)
            "Attempting to end drag while not in progress!")
    (let [{:keys [cursor-start ids]} dragging
          delta (u/point-delta cursor-start cursor)]
      (-> db
          (u/update-in-map :ui/positions ids u/translate-point delta)
          (dissoc :dragging :cursor)))))
