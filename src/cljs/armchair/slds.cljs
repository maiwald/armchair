(ns armchair.slds
  (:require [reagent.core :as r]
            [armchair.util :as u]))

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
