(ns armchair.modals.switch-form
  (:require [clojure.spec.alpha :as s]
            [com.rpl.specter
             :refer [must NONE MAP-VALS MAP-KEYS]
             :refer-macros [setval]]
            [re-frame.core :as re-frame :refer [reg-sub]]
            [armchair.modals.events :refer [assert-no-open-modal
                                            build-modal-assertion]]
            [armchair.slds :as slds]
            [armchair.input :as input]
            [armchair.components :as c]
            [armchair.util :as u :refer [<sub >evt stop-e! e->val]]
            [armchair.events :refer [reg-event-data reg-event-meta]]))

;; Events

(def assert-switch-modal (build-modal-assertion :switch-form))

(reg-event-meta
  ::open
  (fn [db [_ id]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :switch-form]
              (if-let [{:keys [display-name value-ids default]} (get-in db [:switches id])]
                (let [switch-values (mapv (:switch-values db) value-ids)
                      default (reduce (fn [acc vid]
                                        (if (= default vid)
                                          (reduced acc)
                                          (inc acc)))
                                      0
                                      value-ids)]
                  {:switch-id id
                   :display-name display-name
                   :values switch-values
                   :default default})
                {:display-name ""
                 :values (vector {:entity/id (random-uuid)
                                  :entity/type :switch-value
                                  :display-name "ON"}
                                 {:entity/id (random-uuid)
                                  :entity/type :switch-value
                                  :display-name "OFF"})}))))

(reg-event-meta
  ::update-name
  (fn [db [e value]]
    (assert-switch-modal db e)
    (assoc-in db [:modal :switch-form :display-name] value)))

(reg-event-meta
  ::update-value
  (fn [db [e index value]]
    (assert-switch-modal db e)
    (assoc-in db [:modal :switch-form :values index :display-name] value)))

(reg-event-meta
  ::change-default
  (fn [db [e index]]
    (assert-switch-modal db e)
    (if (= index (get-in db [:modal :switch-form :default]))
      (update-in db [:modal :switch-form] dissoc :default)
      (assoc-in db [:modal :switch-form :default] index))))

(reg-event-meta
  ::add-value
  (fn [db e]
    (assert-switch-modal db e)
    (update-in db [:modal :switch-form :values]
               conj {:entity/id (random-uuid)
                     :entity/type :switch-value
                     :display-name ""})))

(reg-event-meta
  ::remove-value
  (fn [db [e index]]
    (assert-switch-modal db e)
    (cond-> (assoc-in db [:modal :switch-form :values index :deleted] true)
      (= index (get-in db [:modal :switch-form :default]))
      (update-in [:modal :switch-form] dissoc :default))))

(reg-event-data
  ::save
  (fn [db _]
    (assert-switch-modal db)
    (let [{:keys [switch-id display-name values default]} (get-in db [:modal :switch-form])
          belongs-to-switch? (fn [i] (= (:switch-id i) switch-id))
          {values nil
           deleted-values true} (group-by :deleted values)
          deleted-value-ids (map :entity/id deleted-values)
          id (or switch-id (random-uuid))
          value-map (into {} (for [v values] [(:entity/id v) v]))]
      (if (s/valid? :modal/switch-form {:switch-id id
                                        :display-name display-name
                                        :values values})
        (-> (setval [:lines MAP-VALS
                     belongs-to-switch? (must :clauses)
                     MAP-KEYS #(contains? (set deleted-value-ids) %)] NONE db)
            (dissoc :modal)
            (assoc-in [:switches id]
                      {:entity/id id
                       :entity/type :switch
                       :display-name display-name
                       :value-ids (mapv :entity/id values)
                       :default (:entity/id (get values default))})
            (update :switch-values merge value-map)
            (update :switch-values #(apply dissoc % deleted-value-ids)))
        db))))

;; Subscriptions

(reg-sub
  ::switch-data
  :<- [:modal]
  (fn [modal]
    (if-let [modal-data (:switch-form modal)]
      (update modal-data :values
              (fn [values]
                (for [index (range (count values))
                      :let [v (get values index)]
                      :when (not (:deleted v))]
                  [index (:display-name v)]))))))

;; Views

(defn modal []
  (letfn [(close-modal [] (>evt [:close-modal]))
          (update-name [e] (>evt [::update-name (e->val e)]))
          (change-default [index] (>evt [::change-default index]))
          (update-value [index e] (>evt [::update-value index (e->val e)]))
          (remove-value [index] (>evt [::remove-value index]))
          (add-value [] (>evt [::add-value]))
          (save [] (>evt [::save]))]
    (fn []
      (let [{:keys [display-name values default]} (<sub [::switch-data])]
        [slds/modal {:title "Switch"
                     :close-handler close-modal
                     :confirm-handler save}
         [:div.switch-form
          [input/text {:label "Name"
                       :on-change update-name
                       :value display-name}]
          [input/label "Values"]
          (for [[index value-name] values]
            ^{:key (str "switch-value" index)}
            [:div.switch-form__value
             [input/text {:on-change (partial update-value index)
                          :value value-name}]
             [input/checkbox {:label "default?"
                              :checked? (= default index)
                              :on-change (partial change-default index)}]
             [:a {:on-click (partial remove-value index)}
              [c/icon "trash-alt"]]])
          [c/button {:title "Add Value"
                     :icon "plus"
                     :on-click add-value}]]]))))
