(ns armchair.modals.connection-trigger-creation
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [armchair.slds :as slds]
            [armchair.input :as input]
            [armchair.util :as u :refer [<sub >evt e->val]]
            [armchair.events :refer [reg-event-data reg-event-meta]]
            [armchair.location-editor.views :refer [position-select]]))

;; Events

(reg-event-meta
  ::update-target-id
  (fn [db [_ target-id]]
    (assoc-in db [:modal :connection-trigger-creation :target-id] target-id)))

(reg-event-meta
  ::update-target-position
  (fn [db [_ position]]
    (assoc-in db [:modal :connection-trigger-creation :target-position] position)))

(reg-event-data
  ::save
  (fn [db]
    (let [{:keys [location-id
                  location-position
                  target-id
                  target-position]} (get-in db [:modal :connection-trigger-creation])]
      (-> db
          (dissoc :modal)
          (assoc-in [:locations location-id :connection-triggers location-position]
                    [target-id target-position])))))

;; Subscriptions

(reg-sub
  ::location-options
  :<- [:modal]
  :<- [:db-locations]
  (fn [[modal locations]]
    (if-let [{:keys [location-id]} (:connection-trigger-creation modal)]
      (->> (dissoc locations location-id)
           (u/map-values :display-name)))))

;; Views

(defn modal [{:keys [target-id target-position]}]
  (letfn [(close-modal [e] (>evt [:close-modal]))
          (update-target [e] (>evt [::update-target-id (uuid (e->val e))]))
          (update-position [position] (>evt [::update-target-position position]))
          (save [] (>evt [::save]))]
    (fn [{:keys [target-id target-position]}]
      (let [location-options (<sub [::location-options])]
        [slds/modal {:title "New Exit"
                     :close-handler close-modal
                     :confirm-handler save}
         [input/select {:on-change update-target
                        :options location-options
                        :value target-id}]
         (when (some? target-id)
           [position-select target-id update-position target-position])]))))
