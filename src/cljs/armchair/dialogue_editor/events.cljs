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
                                 :text ""})))))

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
                                                 :text ""})))))

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
                                 :trigger-ids []})))))

(reg-event-data
  :dialogue-editor/delete-trigger
  (fn [db [_ trigger-node-id trigger-id]]
    (-> db
        (update :triggers dissoc trigger-id)
        (update-in [:lines trigger-node-id :trigger-ids]
                   (fn [ts]
                     (vec (remove #(= trigger-id %) ts)))))))

(reg-event-data
  :dialogue-editor/delete-node
  (fn [db [_ node-id]]
    (db/delete-node-with-references db node-id)))

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
  :dialogue-editor/delete-dialogue-state
  (fn [db [_ line-id]]
    (let [dialogue-id (get-in db [:lines line-id :dialogue-id])]
      (update-in db [:dialogues dialogue-id :states] dissoc line-id))))

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
    (let [{:keys [line-id index]} (:connecting db)
          start-line (get-in db [:lines line-id])
          id-path (case (:kind start-line)
                    :player (let [option-id (get-in start-line [:options index])]
                              [:player-options option-id :next-line-id])
                    :case [:lines line-id :clauses index]
                    [:lines line-id :next-line-id])]
      (cond-> (dissoc db :connecting :cursor)
        (not= line-id end-id) (assoc-in id-path end-id)))))

(reg-event-data
  :dialogue-editor/disconnect-line
  (fn [db [_ id]]
    (update-in db [:lines id] dissoc :next-line-id)))

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
                                                 :text ""})))))

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
      (update-in db [:player-options option-id] dissoc :next-line-id))))

(reg-event-data
  :dialogue-editor/disconnect-case-clause
  (fn [db [_ id switch-value-id]]
    (update-in db [:lines id :clauses] dissoc switch-value-id)))
