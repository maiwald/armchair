(ns armchair.modals.trigger-creation
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [clojure.spec.alpha :as s]
            [armchair.components :as c]
            [armchair.input :as input]
            [armchair.modals.events :refer [assert-no-open-modal
                                            build-modal-assertion]]
            [armchair.util :as u :refer [<sub >evt e->val]]
            [armchair.events :refer [reg-event-data reg-event-meta]]))

;; Events

(def assert-trigger-modal (build-modal-assertion :trigger-creation))

(reg-event-meta
  ::open
  (fn [db [_ node-id]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :trigger-creation]
              {:trigger-node-id node-id})))

(reg-event-meta
  ::update-switch-id
  (fn [db [_ id]]
    (assert-trigger-modal db)
    (update-in db [:modal :trigger-creation]
               (fn [t] (-> t
                           (assoc :switch-id id)
                           (dissoc :switch-value))))))

(reg-event-meta
  ::update-swtich-value
  (fn [db [_ value]]
    (assert-trigger-modal db)
    (assoc-in db [:modal :trigger-creation :switch-value] value)))

(reg-event-data
  ::save
  (fn [db]
    (assert-trigger-modal db)
    (let [trigger-id (random-uuid)
          {:keys [trigger-node-id switch-id switch-value]} (get-in db [:modal :trigger-creation])
          trigger {:entity/id trigger-id
                   :entity/type :trigger
                   :switch-id switch-id
                   :switch-value switch-value}]
      (cond-> db
        (s/valid? :trigger/trigger trigger)
        (-> (update-in [:lines trigger-node-id :trigger-ids]
                       (fn [ts] (conj (vec ts) trigger-id)))
            (assoc-in [:triggers trigger-id] trigger)
            (dissoc :modal))))))

;; Subscriptions

(reg-sub
  ::switch-options
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
  ::dialogue-state-options
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

;; Views

(defn modal [{:keys [switch-id switch-value]}]
  (let [{:keys [switch-options value-options]} (<sub [::switch-options])]
    [c/modal {:title "Add Trigger Node"
              :close-handler #(>evt [:close-modal])
              :confirm-handler #(>evt [::save])}
     [input/select {:label "Switch"
                    :options switch-options
                    :value switch-id
                    :on-change #(>evt [::update-switch-id (uuid (e->val %))])}]
     [input/select {:label "Value"
                    :options value-options
                    :value switch-value
                    :disabled (nil? switch-id)
                    :on-change #(>evt [::update-swtich-value (uuid (e->val %))])}]]))
