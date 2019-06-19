(ns armchair.dialogue-editor.events
  (:require [clojure.spec.alpha :as s]
            [armchair.db :as db]
            [armchair.config :as config]
            [armchair.events :refer [reg-event-data reg-event-meta]]
            [armchair.undo :refer [record-undo]]
            [armchair.util :as u]))

(reg-event-data
  :create-npc-line
  (fn [db [_ dialogue-id]]
    (let [id (random-uuid)
          character-id (get-in db [:dialogues dialogue-id :character-id])]
      (-> db
          (assoc-in [:ui/positions id] config/default-ui-position)
          (assoc-in [:lines id] {:entity/id id
                                 :entity/type :line
                                 :kind :npc
                                 :character-id character-id
                                 :dialogue-id dialogue-id
                                 :text ""
                                 :next-line-id nil})))))

(reg-event-data
  :create-player-line
  (fn [db [_ dialogue-id]]
    (let [id (random-uuid)
          option-id (random-uuid)]
      (-> db
          (assoc-in [:ui/positions id] config/default-ui-position)
          (assoc-in [:lines id] {:entity/id id
                                 :entity/type :line
                                 :kind :player
                                 :dialogue-id dialogue-id
                                 :options [option-id]})
          (assoc-in [:player-options option-id] {:entity/id option-id
                                                 :entity/type :player-option
                                                 :text ""
                                                 :next-line-id nil})))))

(reg-event-data
  :create-trigger-node
  (fn [db [_ dialogue-id]]
    (let [id (random-uuid)]
      (-> db
          (assoc-in [:ui/positions id] config/default-ui-position)
          (assoc-in [:lines id] {:entity/id id
                                 :entity/type :line
                                 :kind :trigger
                                 :dialogue-id dialogue-id
                                 :next-line-id nil})))))

(reg-event-data
  :dialogue-editor/delete-trigger
  (fn [db [_ trigger-node-id trigger-id]]
    (-> db
        (update :triggers dissoc trigger-id)
        (update-in [:lines trigger-node-id :trigger-ids]
                   (fn [ts]
                     (vec (remove #(= trigger-id %) ts)))))))

(defn next-line-clearer [id]
  (fn [item]
    (cond-> item
      (= (:next-line-id item) id)
      (assoc :next-line-id nil))))

(reg-event-data
  :dialogue-editor/delete-trigger-node
  (fn [db [_ id]]
    (let [trigger-ids (get-in db [:lines id :trigger-ids])
          clear-line (next-line-clearer id)]
      (-> db
          (update :triggers #(apply dissoc % trigger-ids))
          (update :ui/positions dissoc id)
          (update :lines dissoc id)
          (u/update-values :player-options clear-line)
          (u/update-values :lines clear-line)))))

(defn initial-line? [db line-id]
  (let [dialogue-id (get-in db [:lines line-id :dialogue-id])]
    (= line-id (get-in db [:dialogues dialogue-id :initial-line-id]))))

(reg-event-data
  :update-line
  (fn [db [_ id field value]]
    (assert (not (and (= field :character-id)
                      (initial-line? db id)))
            "Cannot modify initial line's character!")
    (assoc-in db [:lines id field] value)))

(reg-event-data
  :delete-line
  (fn [db [_ id]]
    (assert (not (initial-line? db id))
            "Initial lines cannot be deleted!")
    (let [options (get-in db [:lines id :options])
          clear-line (next-line-clearer id)]
      (-> db
          (db/clear-dialogue-state id)
          (update :ui/positions dissoc id)
          (update :lines dissoc id)
          (update :player-options #(apply dissoc % options))
          (u/update-values :player-options clear-line)
          (u/update-values :lines clear-line)))))

(reg-event-data
  :dialogue-editor/delete-dialogue-state
  (fn [db [_ id]]
    (db/clear-dialogue-state db id)))

(reg-event-meta
  :start-connecting-lines
  (fn [db [_ line-id cursor index]]
    (assert (not (contains? db :connecting))
            "Attempting to start connecting lines while already in progress!")
    (assoc db
           :connecting {:cursor-start cursor
                        :index index
                        :line-id line-id}
           :cursor cursor)))

(reg-event-data
  :end-connecting-lines
  (fn [db [_ end-id]]
    (assert (s/valid? :armchair.db/connecting-lines (:connecting db))
            "Attempting to end connecting with missing or invalid state!")
    (let [{start-id :line-id index :index} (:connecting db)
          id-path (if-let [option-id (get-in db [:lines start-id :options index])]
                    [:player-options option-id :next-line-id]
                    [:lines start-id :next-line-id])]
      (cond-> (dissoc db :connecting :cursor)
        (not= start-id end-id) (assoc-in id-path end-id)))))

(reg-event-data
  :dialogue-editor/disconnect-line
  (fn [db [_ id]]
    (assoc-in db [:lines id :next-line-id] nil)))

;; Option events

(reg-event-data
  :dialogue-editor/move-option
  (fn [db [_ line-id s-index direction]]
    (let [t-index (case direction
                    :up (dec s-index)
                    :down (inc s-index))
          swap-option (fn [options]
                        (replace {(get options s-index) (get options t-index)
                                  (get options t-index) (get options s-index)}
                                 options))]
      (update-in db [:lines line-id :options] swap-option))))

(reg-event-data
  :dialogue-editor/add-option
  (fn [db [_ line-id]]
    (let [option-id (random-uuid)]
      (-> db
          (update-in [:lines line-id :options] conj option-id)
          (assoc-in [:player-options option-id] {:entity/id option-id
                                                 :entity/type :player-option
                                                 :text ""
                                                 :next-line-id nil})))))

(reg-event-data
  :dialogue-editor/update-option
  (fn [db [_ line-id index text]]
    (let [option-id (get-in db [:lines line-id :options index])]
      (assoc-in db [:player-options option-id :text] text))))

(reg-event-data
  :dialogue-editor/delete-option
  (fn [db [_ line-id index]]
    (let [option-id (get-in db [:lines line-id :options index])]
      (-> db
          (update-in [:lines line-id :options] #(u/removev % index))
          (update :player-options dissoc option-id)))))

(reg-event-data
  :dialogue-editor/disconnect-option
  (fn [db [_ id index]]
    (let [option-id (get-in db [:lines id :options index])]
      (assoc-in db [:player-options option-id :next-line-id] nil))))
