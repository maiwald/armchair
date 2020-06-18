(ns armchair.slds
  (:require [reagent.core :as r]
            [armchair.util :as u]))

(defn radio-button-group [{:keys [label options active on-change]}]
  (let [id (gensym "radio-button-group")]
    [:fieldset {:class "slds-form-element"}
     (when label
       [:legend {:class ["slds-form-element__legend"
                         "slds-form-element__label"]} label])
     [:div {:class "slds-form-element__control"}
      [:div {:class "slds-radio_button-group"}
       (for [[option option-label] options]
         [:span {:key (str id option)
                 :class "slds-button slds-radio_button"}
          [:input {:type "radio"
                   :name id
                   :id (str id option)
                   :checked (= active option)
                   :on-change #(on-change option)}]
          [:label {:class "slds-radio_button__label"
                   :for (str id option)}
           [:span {:class "slds-radio_faux"}
            option-label]]])]]]))

(defn modal [{:keys [title close-handler confirm-handler]}]
  [:div
   [:section {:class ["slds-modal"
                      "slds-fade-in-open"]}
    [:div {:class "slds-modal__container"}
     [:form {:on-submit (u/e-> confirm-handler)}
      [:header {:class "slds-modal__header"}
       [:button {:class "slds-button slds-button_icon slds-modal__close slds-button_icon-inverse"
                 :on-click close-handler
                 :type "button"
                 :title "Close"}
        [:svg {:class "slds-button__icon slds-button__icon_large"}
         [:use {:xlinkHref "assets/icons/utility-sprite/svg/symbols.svg#close", :xmlnsXlink "http://www.w3.org/1999/xlink"}]]]
       [:h2 {:class "slds-text-heading_medium slds-hyphenate"} title]]
      (into [:div {:class "slds-modal__content slds-p-around_medium"}]
            (r/children (r/current-component)))
      [:footer {:class "slds-modal__footer"}
       [:button {:class "slds-button slds-button_brand"
                 :type "submit"}
        "Ok"]]]]]
   [:div {:class "slds-backdrop slds-backdrop_open"}]])
