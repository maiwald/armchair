(ns armchair.modals.unlock-conditions-form
  (:require [clojure.spec.alpha :as s]
            [re-frame.core :as re-frame :refer [reg-sub]]
            [armchair.config :as config]
            [armchair.modals.events :refer [assert-no-open-modal
                                            build-modal-assertion]]
            [armchair.input :as input]
            [armchair.components :as c]
            [armchair.util :as u :refer [<sub >evt e->val]]
            [armchair.events :refer [reg-event-data reg-event-meta]]))


;; Events

(def assert-conditions-modal
  (build-modal-assertion :unlock-conditions-form))

(reg-event-meta
  ::open
  (fn [db [_ line-id index]]
    (assert-no-open-modal db)
    (let [option-id (get-in db [:lines line-id :options index])
          {:keys [terms conjunction]
           :or {terms (vector {})
                conjunction :and}} (get-in db [:player-options option-id :condition])]
      (assoc-in db [:modal :unlock-conditions-form]
                {:player-option-id option-id
                 :conjunction conjunction
                 :terms terms}))))

(reg-event-meta
  ::update-conjunction
  (fn [db [e value]]
    (assert-conditions-modal db e)
    (assoc-in db [:modal :unlock-conditions-form :conjunction] value)))

(reg-event-meta
  ::add-term
  (fn [db [e]]
    (assert-conditions-modal db e)
    (update-in db [:modal :unlock-conditions-form :terms]
               conj {})))

(reg-event-meta
  ::remove-term
  (fn [db [e index]]
    (assert-conditions-modal db e)
    (update-in db [:modal :unlock-conditions-form :terms]
               u/removev index)))

(reg-event-meta
  ::update-term-switch
  (fn [db [e index value]]
    (assert-conditions-modal db e)
    (-> db
        (assoc-in [:modal :unlock-conditions-form :terms index :switch-id] value)
        (update-in [:modal :unlock-conditions-form :terms index] dissoc :switch-value-id))))

(reg-event-meta
  ::update-term-operator
  (fn [db [e index value]]
    (assert-conditions-modal db e)
    (assoc-in db [:modal :unlock-conditions-form :terms index :operator] value)))

(reg-event-meta
  ::update-term-value
  (fn [db [e index value]]
    (assert-conditions-modal db e)
    (assoc-in db [:modal :unlock-conditions-form :terms index :switch-value-id] value)))

(reg-event-data
  ::save
  (fn [db [e]]
    (assert-conditions-modal db e)
    (let [{:keys [player-option-id conjunction terms]} (get-in db [:modal :unlock-conditions-form])
          condition {:conjunction conjunction
                     :terms terms}]
      (cond-> db
        (s/valid? :player-option/condition condition)
        (-> (dissoc :modal)
            (assoc-in [:player-options player-option-id :condition] condition))))))

;; Subscriptions

(reg-sub
  ::modal-data
  :<- [:modal]
  :<- [:db-switches]
  :<- [:db-switch-values]
  (fn [[modal switches switch-values]]
    (if-let [{:keys [terms conjunction]} (:unlock-conditions-form modal)]
      (let [switch-options (u/map-values :display-name switches)
            used-switches (set (map :switch-id terms))]
        {:conjunction conjunction
         :terms
         (map-indexed
           (fn [index {:keys [switch-id] :as c}]
             (let [value-options (->> switch-id
                                      switches
                                      :value-ids
                                      (map (fn [id]
                                             [id (-> id
                                                     switch-values
                                                     :display-name)])))]
               [index (assoc c
                             :switch-options (apply dissoc switch-options
                                                    (disj used-switches switch-id))
                             :switch-value-options value-options)]))
           terms)}))))


;; Views

(def operator-options
  (u/map-values :display-name config/condition-operators))

(def conjunction-options
  (u/map-values :display-name config/condition-conjunctions))

(defn term-component [index {:keys [switch-id switch-options
                                    operator
                                    switch-value-id switch-value-options]}]
  (letfn [(update-switch [e] (>evt [::update-term-switch index (uuid (e->val e))]))
          (update-operator [e] (>evt [::update-term-operator index (keyword (e->val e))]))
          (update-value [e] (>evt [::update-term-value index (uuid (e->val e))]))
          (remove-term [] (>evt [::remove-term index]))]
    [:li {:class "condition-select"}
     [:div {:class "condition-select__switch"}
      [input/select {:on-change update-switch
                     :options switch-options
                     :value switch-id}]]
     [:div {:class "condition-select__operator"}
      [input/select {:on-change update-operator
                     :options operator-options
                     :value operator}]]
     [:div {:class "condition-select__value"}
      [input/select {:on-change update-value
                     :disabled (empty? switch-value-options)
                     :options switch-value-options
                     :value switch-value-id}]]
     [:div {:class "condition-select__delete"}
      [:a {:on-click remove-term}
       [c/icon "trash" "Delete condition"]]]]))

(defn modal []
  (letfn [(close-modal [] (>evt [:close-modal]))
          (save-conditions [] (>evt [::save]))
          (add-condition [] (>evt [::add-term]))
          (update-conjunction [e] (>evt [::update-conjunction (keyword (e->val e))]))]
    (fn []
      (let [{:keys [terms conjunction]} (<sub [::modal-data])]
        [c/modal {:title "Unlock Conditions"
                  :close-handler close-modal
                  :confirm-handler save-conditions}
         [input/select {:label "Conjunction"
                        :on-change update-conjunction
                        :options conjunction-options
                        :value conjunction}]
         [input/label "Conditions"]
         [:ul
          (for [[index term] terms]
            ^{:key (str "condition-select" index)}
            [term-component index term])]
         [c/button {:title "Add Condition"
                    :icon "plus"
                    :on-click add-condition}]]))))
