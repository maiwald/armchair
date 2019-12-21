(ns armchair.modals.connection-trigger-creation
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [armchair.slds :as slds]
            [armchair.input :as input]
            [armchair.util :as u :refer [<sub >evt e->val]]
            [armchair.events :refer [reg-event-data reg-event-meta]]
            [armchair.modals.events :refer [build-modal-assertion]]
            [armchair.location-editor.views :refer [position-select]]))

;; Events

(def assert-connection-trigger-creation-modal
  (build-modal-assertion :connection-trigger-creation))

(reg-event-meta
  ::update-target-id
  (fn [db [_ target-id]]
    (assert-connection-trigger-creation-modal db)
    (assoc-in db [:modal :connection-trigger-creation :target-id] target-id)))

(reg-event-meta
  ::update-target-position
  (fn [db [_ position]]
    (assert-connection-trigger-creation-modal db)
    (assoc-in db [:modal :connection-trigger-creation :target-position] position)))

(reg-event-meta
  ::update-symmetric
  (fn [db [_ value]]
    (assert-connection-trigger-creation-modal db)
    (assoc-in db [:modal :connection-trigger-creation :symmetric?] value)))

(reg-event-data
  ::save
  (fn [db]
    (assert-connection-trigger-creation-modal db)
    (let [{:keys [location-id
                  location-position
                  target-id
                  target-position
                  symmetric?]} (get-in db [:modal :connection-trigger-creation])]
      (if (and (some? target-id)
               (some? target-position))
        (-> db
            (dissoc :modal)
            (assoc-in [:locations location-id :connection-triggers location-position]
                      [target-id target-position])
            (cond->
              symmetric?
              (assoc-in [:locations target-id :connection-triggers target-position]
                        [location-id location-position])))
        db))))

;; Subscriptions

(reg-sub
  ::location-options
  :<- [:modal]
  :<- [:db-locations]
  (fn [[modal locations]]
    (if-let [{:keys [location-id]} (:connection-trigger-creation modal)]
      (->> (dissoc locations location-id)
           (u/map-values :display-name)
           (sort-by second)))))

;; Views

(defn modal [{:keys [target-id target-position symmetric?]}]
  (letfn [(close-modal [e] (>evt [:close-modal]))
          (update-target [e] (>evt [::update-target-id (uuid (e->val e))]))
          (update-symmetric [e] (>evt [::update-symmetric (e->val e)]))
          (update-position [position] (>evt [::update-target-position position]))
          (save [] (>evt [::save]))]
    (fn [{:keys [target-id target-position symmetric?]}]
      (let [location-options (<sub [::location-options])]
        [slds/modal {:title "New Exit"
                     :close-handler close-modal
                     :confirm-handler save}
         [input/select {:label "Location"
                        :on-change update-target
                        :options location-options
                        :value target-id}]
         (if (some? target-id)
           [:<>
            [input/label "Position"]
            [position-select target-id update-position target-position]])
         [input/checkbox {:label "Also add reverse exit"
                          :on-change update-symmetric
                          :checked? symmetric?}]]))))
