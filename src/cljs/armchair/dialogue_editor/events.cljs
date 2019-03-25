(ns armchair.dialogue-editor.events
  (:require [clojure.spec.alpha :as s]
            [re-frame.core :refer [reg-event-db reg-event-fx]]
            [armchair.db :as db]
            [armchair.config :as config]
            [armchair.events :refer [validate]]
            [armchair.undo :refer [record-undo]]
            [armchair.util :as u]))

(reg-event-db
  :create-npc-line
  [validate
   record-undo]
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
                                 :text nil
                                 :next-line-id nil})))))

(reg-event-db
  :create-player-line
  [validate
   record-undo]
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

(reg-event-db
  :create-trigger-node
  [validate
   record-undo]
  (fn [db [_ dialogue-id]]
    (let [id (random-uuid)]
      (-> db
          (assoc-in [:ui/positions id] config/default-ui-position)
          (assoc-in [:lines id] {:entity/id id
                                 :entity/type :line
                                 :kind :trigger
                                 :dialogue-id dialogue-id
                                 :next-line-id nil})))))

(reg-event-db
  :dialogue-editor/delete-trigger
  [validate
   record-undo]
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

(reg-event-db
  :dialogue-editor/delete-trigger-node
  [validate
   record-undo]
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

(reg-event-db
  :update-line
  [validate
   record-undo]
  (fn [db [_ id field value]]
    (assert (not (and (= field :character-id)
                      (initial-line? db id)))
            "Cannot modify initial line's character!")
    (assoc-in db [:lines id field] value)))

(reg-event-db
  :delete-line
  [validate
   record-undo]
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

(reg-event-db
  :dialogue-editor/delete-dialogue-state
  [validate
   record-undo]
  (fn [db [_ id]]
    (db/clear-dialogue-state db id)))

(reg-event-db
  :start-connecting-lines
  [validate]
  (fn [db [_ line-id cursor index]]
    (assert (not (contains? db :connecting))
            "Attempting to start connecting lines while already in progress!")
    (assoc db
           :connecting {:cursor-start cursor
                        :index index
                        :line-id line-id}
           :cursor cursor)))

(reg-event-db
  :end-connecting-lines
  [validate
   record-undo]
  (fn [db [_ end-id]]
    (assert (s/valid? :armchair.db/connecting-lines (:connecting db))
            "Attempting to end connecting with missing or invalid state!")
    (let [{start-id :line-id index :index} (:connecting db)
          id-path (if-let [option-id (get-in db [:lines start-id :options index])]
                    [:player-options option-id :next-line-id]
                    [:lines start-id :next-line-id])]
      (cond-> (dissoc db :connecting :cursor)
        (not= start-id end-id) (assoc-in id-path end-id)))))

(reg-event-db
  :dialogue-editor/disconnect-line
  [validate
   record-undo]
  (fn [db [_ id]]
    (assoc-in db [:lines id :next-line-id] nil)))

;; Option events

(reg-event-db
  :dialogue-editor/move-option
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
  :dialogue-editor/add-option
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
  :dialogue-editor/update-option
  [validate
   record-undo]
  (fn [db [_ line-id index text]]
    (let [option-id (get-in db [:lines line-id :options index])]
      (assoc-in db [:player-options option-id :text] text))))

(reg-event-db
  :dialogue-editor/delete-option
  [validate
   record-undo]
  (fn [db [_ line-id index]]
    (let [option-id (get-in db [:lines line-id :options index])]
      (-> db
          (update-in [:lines line-id :options] #(u/removev % index))
          (update :player-options dissoc option-id)))))

(reg-event-db
  :dialogue-editor/disconnect-option
  [validate
   record-undo]
  (fn [db [_ id index]]
    (let [option-id (get-in db [:lines id :options index])]
      (assoc-in db [:player-options option-id :next-line-id] nil))))
