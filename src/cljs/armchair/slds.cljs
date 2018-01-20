(ns armchair.slds)

(defn form [{:keys [title]} & children]
  [:div {:className "slds-form slds-form_stacked"}
   [:div {:className "slds-text-heading_small"} title]
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

(defn global-navigation [links]
  [:div {:className "slds-context-bar"}
   [:div {:className "slds-context-bar__primary"}
    [:div {:className "slds-context-bar__item slds-no-hover"}
     [:span {:className "slds-context-bar__label-action slds-context-bar__app-name"} "Armchair"]]
    [:div {:className "slds-context-bar__item"}
     [:a {:className "slds-context-bar__label-action"} "Home"]]
    [:nav {:className "slds-context-bar__secondary" :role "navigation"}
     [:ul {:className "slds-grid"}
      (for [[label handler] links]
        [:li {:key (str "nav-" label)
              :className "slds-context-bar__item slds-context-bar__dropdown-trigger slds-dropdown-trigger slds-dropdown-trigger_hover"}
         [:a {:className "slds-context-bar__label-action" :on-click handler} label]

         [:div {:className "slds-context-bar__icon-action slds-p-left_none"}
          [:button {:className "slds-button slds-button_icon slds-button_icon slds-context-bar__button" :title "Open menu item submenu"}
           [:svg {:className "slds-button__icon" :aria-hidden "true"}
            [:use {:xlinkHref "/assets/icons/utility-sprite/svg/symbols.svg#chevrondown" :xmlnsXlink "http://www.w3.org/1999/xlink"}]
            [:span {:className "slds-assistive-text"} "Open menu item submenu"]]]

          [:div {:className "slds-dropdown slds-dropdown_right"}
           [:ul {:className "slds-dropdown__list" :role "menu"}
            [:li {:className "slds-dropdown__item" :role "presentation"}
             [:a {:role "menuitem"} "Foo"]]

            [:li {:className "slds-dropdown__item" :role "presentation"}
             [:a {:role "menuitem"} "Bar"]]]]]])]]]])
