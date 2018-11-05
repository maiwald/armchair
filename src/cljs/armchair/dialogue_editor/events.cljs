(ns armchair.dialogue-editor.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [armchair.db :as db]
            [armchair.config :as config]
            [armchair.events :refer [validate]]
            [armchair.undo :refer [record-undo]]
            [armchair.util :refer [map-values]]))

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
    (let [dialogue-id (get-in db [:lines id :dialogue-id])]
      (letfn [(clear-line [line]
                (update line :next-line-id #(if (= id %) nil %)))
              (clear-options [line]
                (update line :options #(mapv clear-line %)))]
        (-> db
          (db/clear-dialogue-state id)
          (update :lines dissoc id)
          (update :lines #(map-values (fn [line]
                                        (case (:kind line)
                                          :npc (clear-line line)
                                          :player (clear-options line)))
                                      %)))))))

(reg-event-db
  :delete-dialogue-state
  [validate
   record-undo]
  (fn [db [_ id]]
    (db/clear-dialogue-state db id)))

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
    (assoc-in db [:lines id :options index :next-line-id] nil)))
