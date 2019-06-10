(ns armchair.modals.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [armchair.config :as config]
            [armchair.util :as u]))

(reg-sub
  :dialogue/modal-line
  :<- [:db-lines]
  :<- [:db-dialogues]
  (fn [[lines dialogues] [_ line-id]]
    (when-let [{id :entity/id :keys [dialogue-id] :as line} (get lines line-id)]
      (-> line
          (assoc :initial-line? (= id (get-in dialogues [dialogue-id :initial-line-id])))
          (assoc :option-count (count (:options line)))))))

(reg-sub
  :trigger-creation/switch-options
  :<- [:modal]
  :<- [:db-triggers]
  :<- [:db-lines]
  :<- [:db-switches]
  :<- [:db-switch-values]
  (fn [[{{:keys [trigger-node-id switch-id]} :trigger-creation} triggers lines switches switch-values]]
    (let [used-switches (->> (get-in lines [trigger-node-id :trigger-ids])
                             (map #(get-in triggers [% :switch-id]))
                             set)]
      {:switch-options (->> switches
                            (u/filter-map (fn [{id :entity/id}]
                                            (not (contains? used-switches id))))
                            (u/map-values :display-name))
       :value-options (when switch-id
                        (map
                          (fn [id]
                            [id (get-in switch-values [id :display-name])])
                          (get-in switches [switch-id :value-ids])))})))

(reg-sub
  :trigger-creation/dialogue-state-options
  :<- [:modal]
  :<- [:db-triggers]
  :<- [:db-lines]
  :<- [:db-dialogues]
  (fn [[{{:keys [trigger-node-id switch-id]} :trigger-creation} triggers lines dialogues]]
    (let [used-switches (->> (get-in lines [trigger-node-id :trigger-ids])
                             (map #(get-in triggers [% :switch-id]))
                             set)]
      {:switch-options (->> dialogues
                            (u/filter-map (fn [{states :states id :entity/id}]
                                            (and (not (contains? used-switches id))
                                                 (seq states))))
                            (u/map-values :synopsis))
       :value-options (when switch-id
                        (let [{:keys [states initital-line-id]} (dialogues switch-id)]
                          (conj (seq states)
                                [initital-line-id "Initial Line"])))})))

