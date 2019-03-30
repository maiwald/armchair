(ns armchair.input
  (:require [cljsjs.react-select]))

(defn text [{:keys [label on-change value]}]
  (let [id (gensym "input-text")]
    [:div {:class "input input-text"}
     (when label
       [:label {:class "input-text__label input__label" :for id} label])
     [:input {:class "input-text__input"
              :id id
              :on-change on-change
              :value value}]]))

(defn select [{:keys [label disabled on-change value options]}]
  (let [id (gensym "input-select")]
    [:div {:class "input input-select"}
     (when label
       [:label {:class "input-select__label input__label" :for id} label])
     [:select {:class "input-select__select"
               :id id
               :on-change on-change
               :disabled disabled
               :value (or value "nil")}
      [:option {:key (str id "nil") :value "nil" :disabled "disabled"}]
      (for [[option option-label] options]
        [:option {:key (str id ":" option) :value option} option-label])]]))

(defn textarea [{:keys [label on-change value]}]
  (let [id (gensym "input-textarea")]
    [:div {:class "input input-textarea"}
     (when label
       [:label {:class "input-textarea__label input__label" :for id} label])
     [:textarea {:class "input-textarea__textarea"
                 :id id
                 :on-change on-change
                 :value value}]]))

(defn multiselect [{:keys [label on-change values options]}]
  (let [id (gensym "input-multiselect")]
    [:div {:class "input input-multiselect"}
     (when label
       [:label {:class "input-multiselect__label input__label" :for id} label])
     [:> js/Select {:id id
                    :options options
                    :multi true
                    :complete true
                    :onChange #(on-change (map :value (js->clj % :keywordize-keys true)))
                    :matchProp "label"
                    :ignoreCase true
                    :value values}]]))

