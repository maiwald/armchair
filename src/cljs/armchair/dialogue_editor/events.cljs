(ns armchair.dialogue-editor.events
  (:require [clojure.spec.alpha :as s]
            [re-frame.core :refer [reg-event-db reg-event-fx]]
            [armchair.db :as db]
            [armchair.config :as config]
            [armchair.events :refer [validate]]
            [armchair.undo :refer [record-undo]]
            [armchair.util :as u]))

(reg-event-fx
  :create-npc-line
  [validate
   record-undo]
  (fn [{db :db} [_ dialogue-id]]
    (let [id (random-uuid)
          character-id (get-in db [:dialogues dialogue-id :character-id])]
      {:db (-> db
               (assoc-in [:ui/positions id] config/default-ui-position)
               (assoc-in [:lines id] {:entity/id id
                                      :entity/type :line
                                      :kind :npc
                                      :character-id character-id
                                      :dialogue-id dialogue-id
                                      :text nil
                                      :next-line-id nil}))
       :dispatch [:open-npc-line-modal id]})))

(reg-event-fx
  :create-player-line
  [validate
   record-undo]
  (fn [{db :db} [_ dialogue-id]]
    (let [id (random-uuid)]
      {:db (-> db
               (assoc-in [:ui/positions id] config/default-ui-position)
               (assoc-in [:lines id] {:entity/id id
                                      :entity/type :line
                                      :kind :player
                                      :dialogue-id dialogue-id
                                      :options []}))
       :dispatch [:open-player-line-modal id]})))

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

(reg-event-db
  :dialogue-editor/delete-trigger-node
  [validate
   record-undo]
  (fn [db [_ trigger-node-id]]
    (let [trigger-ids (get-in db [:lines trigger-node-id :trigger-ids])]
      (-> db
          (update :triggers #(apply dissoc % trigger-ids))
          (update :lines dissoc trigger-node-id)))))

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
    (let [{:keys [dialogue-id options]} (get-in db [:lines id])]
      (letfn [(clear-line [line]
                (update line :next-line-id #(if (= id %) nil %)))]
        (-> db
          (db/clear-dialogue-state id)
          (update :lines dissoc id)
          (update :player-options #(apply dissoc % options))
          (u/update-values :player-options clear-line)
          (u/update-values :lines (fn [line]
                                    (case (:kind line)
                                      :npc (clear-line line)
                                      :player line))))))))

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

(reg-event-db
  :dialogue-editor/disconnect-option
  [validate
   record-undo]
  (fn [db [_ id index]]
    (let [option-id (get-in db [:lines id :options index])]
      (assoc-in db [:player-options option-id :next-line-id] nil))))
