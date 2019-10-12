(ns armchair.modals.case-node-creation
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [armchair.config :as config]
            [armchair.slds :as slds]
            [armchair.input :as input]
            [armchair.modals.events :refer [assert-no-open-modal
                                            build-modal-assertion]]
            [armchair.util :as u :refer [<sub >evt e->val]]
            [armchair.events :refer [reg-event-data reg-event-meta]]))

;; Events

(def assert-trigger-modal
  (build-modal-assertion :case-node-creation))

(reg-event-meta
  ::open
  (fn [db [_ dialogue-id]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :case-node-creation]
              {:dialogue-id dialogue-id})))

(reg-event-meta
  ::update-switch-id
  (fn [db [_ id]]
    (assert-trigger-modal db)
    (assoc-in db [:modal :case-node-creation :switch-id] id)))

(reg-event-data
  ::save
  (fn [db]
    (assert-trigger-modal db)
    (let [node-id (random-uuid)
          {:keys [dialogue-id switch-id]} (get-in db [:modal :case-node-creation])
          case-node {:entity/id node-id
                     :entity/type :line
                     :kind :case
                     :dialogue-id dialogue-id
                     :switch-id switch-id
                     :clauses {}}]
      (-> db
          (assoc-in [:ui/positions node-id] config/default-ui-position)
          (assoc-in [:lines node-id] case-node)
          (dissoc :modal)))))

;; Subscriptions

(reg-sub
  ::switch-options
  :<- [:db-switches]
  (fn [switches]
    (->> switches
         (u/map-values :display-name)
         (sort-by second))))

;; Views

(defn modal [{:keys [switch-id]}]
  (let [switch-options (<sub [::switch-options])]
    [slds/modal {:title "Add Switch Node"
                 :close-handler #(>evt [:close-modal])
                 :confirm-handler #(>evt [::save])}
     [input/select {:label "Switch"
                    :options switch-options
                    :value switch-id
                    :on-change #(>evt [::update-switch-id (uuid (e->val %))])}]]))
