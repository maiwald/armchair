(ns armchair.modals.events
  (:require [clojure.spec.alpha :as s]
            [clojure.string :refer [blank?]]
            [armchair.config :as config]
            [armchair.events :refer [reg-event-data reg-event-meta]]
            [armchair.undo :refer [record-undo]]
            [armchair.util :as u]))

(defn assert-no-open-modal [db]
  (assert (not (contains? db :modal))
          "Attempting to open a modal while modal is open!"))

(defn assert-conditions-modal [db]
  (assert (contains? (:modal db) :conditions-form)
          "No conditions form open. Cannot set value!"))

(reg-event-meta
  :close-modal
  (fn [db [_ modal-fn | args]]
    (dissoc db :modal)))

(reg-event-meta
  :modal/open-condition-modal
  (fn [db [_ line-id index]]
    (assert-no-open-modal db)
    (let [option-id (get-in db [:lines line-id :options index])
          {:keys [terms conjunction]
           :or {terms (vector {})
                conjunction :and}} (get-in db [:player-options option-id :condition])]
      (assoc-in db [:modal :conditions-form]
                {:player-option-id option-id
                 :conjunction conjunction
                 :terms terms}))))

(reg-event-meta
  :modal/update-condition-conjunction
  (fn [db [_ value]]
    (assert-conditions-modal db)
    (assoc-in db [:modal :conditions-form :conjunction] value)))

(reg-event-meta
  :modal/add-condition-term
  (fn [db]
    (assert-conditions-modal db)
    (update-in db [:modal :conditions-form :terms]
               conj {})))

(reg-event-meta
  :modal/remove-condition-term
  (fn [db [_ index]]
    (assert-conditions-modal db)
    (update-in db [:modal :conditions-form :terms]
               u/removev index)))

(reg-event-meta
  :modal/update-condition-term-switch
  (fn [db [_ index value]]
    (assert-conditions-modal db)
    (-> db
        (assoc-in [:modal :conditions-form :terms index :switch-id] value)
        (update-in [:modal :conditions-form :terms index] dissoc :switch-value-id))))

(reg-event-meta
  :modal/update-condition-term-operator
  (fn [db [_ index value]]
    (assert-conditions-modal db)
    (assoc-in db [:modal :conditions-form :terms index :operator] value)))

(reg-event-meta
  :modal/update-condition-term-value
  (fn [db [_ index value]]
    (assert-conditions-modal db)
    (assoc-in db [:modal :conditions-form :terms index :switch-value-id] value)))

(reg-event-data
  :modal/save-condition
  (fn [db]
    (assert-conditions-modal db)
    (let [{:keys [player-option-id conjunction terms]} (get-in db [:modal :conditions-form])
          condition {:conjunction conjunction
                     :terms terms}]
      (cond-> db
        (s/valid? :player-option/condition condition)
        (-> (dissoc :modal)
            (assoc-in [:player-options player-option-id :condition] condition))))))

(defn assert-switch-modal [db]
  (assert (contains? (:modal db) :switch-form)
          "No switch form open. Cannot set value!"))

(reg-event-meta
  :modal/open-switch-modal
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

(reg-event-meta
  :modal/update-switch-name
  (fn [db [_ value]]
    (assert-switch-modal db)
    (assoc-in db [:modal :switch-form :display-name] value)))

(reg-event-meta
  :modal/update-switch-value
  (fn [db [_ index value]]
    (assert-switch-modal db)
    (assoc-in db [:modal :switch-form :values index :display-name] value)))

(reg-event-meta
  :modal/remove-switch-value
  (fn [db [_ index]]
    (assert-switch-modal db)
    (assoc-in db [:modal :switch-form :values index :deleted] true)))

(reg-event-meta
  :modal/add-switch-value
  (fn [db _]
    (assert-switch-modal db)
    (let [value-id (random-uuid)]
      (-> db
          (update-in [:modal :switch-form :values] conj {:entity/id (random-uuid)
                                                         :entity/type :switch-value
                                                         :display-name ""})))))

(reg-event-data
  :modal/save-switch
  (fn [db _]
    (assert-switch-modal db)
    (let [modal-data (get-in db [:modal :switch-form])
          {values nil
           deleted-values true} (group-by :deleted (:values modal-data))
          {:keys [switch-id display-name]} modal-data
          id (or switch-id (random-uuid))
          value-map (into {} (for [v values] [(:entity/id v) v]))]
      (cond-> db
        (s/valid? :modal/switch-form {:switch-id id
                                      :display-name display-name
                                      :values values})
        (-> (dissoc :modal)
            (assoc-in [:switches id]
                      {:entity/id id
                       :entity/type :switch
                       :display-name display-name
                       :value-ids (mapv :entity/id values)})
            (update :switch-values merge value-map)
            (update :switch-values #(apply dissoc % (map :entity/id deleted-values))))))))

(defn assert-trigger-modal [db]
  (assert (contains? (:modal db) :trigger-creation)
          "No trigger creation initiated. Cannot set value!"))

(reg-event-meta
  :modal/open-trigger-creation
  (fn [db [_ node-id]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :trigger-creation]
              {:trigger-node-id node-id
               :switch-kind :dialogue-state})))

(reg-event-meta
  :modal/update-trigger-kind
  (fn [db [_ kind]]
    (assert-trigger-modal db)
    (update-in db [:modal :trigger-creation]
               (fn [t] (-> t
                           (assoc :switch-kind kind)
                           (dissoc :switch-id :switch-value))))))

(reg-event-meta
  :modal/update-trigger-switch-id
  (fn [db [_ id]]
    (assert-trigger-modal db)
    (update-in db [:modal :trigger-creation]
               (fn [t] (-> t
                           (assoc :switch-id id)
                           (dissoc :switch-value))))))

(reg-event-meta
  :modal/update-trigger-value
  (fn [db [_ value]]
    (assert-trigger-modal db)
    (assoc-in db [:modal :trigger-creation :switch-value] value)))

(reg-event-data
  :modal/save-trigger
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

(reg-event-meta
  :modal/update-connection-trigger-target
  (fn [db [_ target-id]]
    (assoc-in db [:modal :connection-trigger-creation :target-id] target-id)))

(reg-event-meta
  :modal/update-connection-trigger-position
  (fn [db [_ position]]
    (assoc-in db [:modal :connection-trigger-creation :target-position] position)))

(reg-event-meta
  :modal/save-connection-trigger
  (fn [db]
    (let [{:keys [location-id
                  location-position
                  target-id
                  target-position]} (get-in db [:modal :connection-trigger-creation])]
      (-> db
          (dissoc :modal)
          (assoc-in [:locations location-id :connection-triggers location-position]
                    [target-id target-position])))))

(reg-event-meta
  :open-character-modal
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

(reg-event-data
  :character-form/update
  (fn [db [_ field value]]
    (assert-character-modal db)
    (assoc-in db [:modal :character-form field] value)))

(reg-event-data
  :character-form/save
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

(reg-event-meta
  :open-location-creation
  (fn [db]
    (assert-no-open-modal db)
    (assoc-in db [:modal :location-creation] "")))

(defn assert-location-creation-modal [db]
  (assert (contains? (:modal db) :location-creation)
          "No Location creation modal present. Cannot create!"))

(reg-event-meta
  :update-location-creation-name
  (fn [db [_ display-name]]
    (assert-location-creation-modal db)
    (assoc-in db [:modal :location-creation] display-name)))

(reg-event-data
  :create-location
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

(reg-event-meta
  :open-npc-line-modal
  (fn [db [_ payload]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :npc-line-id] payload)))

(reg-event-meta
  :open-dialogue-creation-modal
  (fn [db _]
    (assert-no-open-modal db)
    (assoc-in db [:modal :dialogue-creation] {:character-id nil
                                              :synopsis nil})))
;; Dialogue state modal

(reg-event-meta
  :open-dialogue-state-modal
  (fn [db [_ line-id]]
    (assert-no-open-modal db)
    (let [dialogue-id (get-in db [:lines line-id :dialogue-id])
          description (get-in db [:dialogues dialogue-id :states line-id])]
      (assoc-in db [:modal :dialogue-state] {:line-id line-id
                                             :description description}))))

(defn assert-dialogue-state-modal [db]
  (assert (contains? (:modal db) :dialogue-state)
          "No dialogue state modal open. Cannot set value!"))

(reg-event-meta
  :dialogue-state-update
  (fn [db [_ description]]
    (assert-dialogue-state-modal db)
    (assoc-in db [:modal :dialogue-state :description] description)))

(reg-event-data
  :create-dialogue-state
  (fn [db]
    (assert-dialogue-state-modal db)
    (let [{:keys [line-id description]} (get-in db [:modal :dialogue-state])
          dialogue-id (get-in db [:lines line-id :dialogue-id])]
      (cond-> (dissoc db :modal)
        (not-empty description)
        (update-in [:dialogues dialogue-id :states] assoc line-id description)))))

(defn assert-dialogue-creation-modal [db]
  (assert (contains? (:modal db) :dialogue-creation)
          "No dialogue creation initiated. Cannot set value!"))

(reg-event-meta
  :dialogue-creation-update
  (fn [db [_ field value]]
    (assert-dialogue-creation-modal db)
    (assoc-in db [:modal :dialogue-creation field] value)))

(reg-event-data
  :create-dialogue
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

