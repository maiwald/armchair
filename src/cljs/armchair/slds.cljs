(ns armchair.slds)

(defn form [& children]
  [:div {:className "slds-form slds-form_horizontal"}
   (map-indexed #(with-meta %2 {:key %1}) children)])

(defn input-select [{:keys [label on-change value options]}]
  (let [id (gensym "input-select")]
    [:div {:className "slds-form-element"}
     [:label {:className "slds-form-element__label" :for id} label]
     [:div {:className "slds-form-element__control"}
      [:div {:className "slds-select_container"}
       [:select {:className "slds-select"
                 :id id
                 :on-change on-change
                 :value value}
        (for [[k v] options] [:option {:key (str id k) :value k} v])]]]]))

(defn input-textarea [{:keys [label on-change value]}]
  (let [id (gensym "input-textarea")]
    [:div {:className "slds-form-element"}
     [:label {:className "slds-form-element__label" :for id} label]
     [:div {:className "slds-form-element__control"}
      [:textarea {:className "slds-textarea"
                  :id id
                  :on-change on-change
                  :value value}]]]))
