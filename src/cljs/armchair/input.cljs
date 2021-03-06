(ns armchair.input)

(defn label [text]
  [:label {:class "input__label"} text])

(defn text [{:keys [label on-change value placeholder]}]
  (let [id (gensym "input-text")]
    [:div {:class "input input-text"}
     (when label
       [:label {:class "input-text__label input__label" :for id} label])
     [:input {:class "input-text__input"
              :id id
              :placeholder placeholder
              :on-change on-change
              :value value}]]))

(defn checkbox [{:keys [label on-change checked?]}]
  (let [id (gensym "input-checkbox")]
    [:div {:class "input input-checkbox"}
     [:input {:class "input-checkbox__input"
              :id id
              :type "checkbox"
              :on-change on-change
              :checked checked?}]
     [:label {:class "input-checkbox__label input__label" :for id} label]]))

(defn select [{:keys [label disabled on-change value options nil-value]}]
  (let [id (gensym "input-select")]
    [:div {:class "input input-select"}
     (when label
       [:label {:class "input-select__label input__label" :for id} label])
     [:select {:class "input-select__select"
               :id id
               :on-change on-change
               :disabled disabled
               :value (or value "nil")}
      [:option {:key (str id "nil")
                :value "nil"
                :disabled (if-not nil-value "disabled")}
       nil-value]
      (for [[option option-label] options]
        [:option {:key (str id ":" option) :value option} option-label])]]))

(defn textarea [{:keys [label on-change value options]}]
  (let [id (gensym "input-textarea")]
    [:div {:class "input input-textarea"}
     (when label
       [:label {:class "input-textarea__label input__label" :for id} label])
     [:textarea (merge options
                       {:class "input-textarea__textarea"
                        :id id
                        :on-change on-change
                        :value value})]]))
