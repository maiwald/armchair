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
    [:nav {:className "slds-context-bar__secondary" :role "navigation"}
     [:ul {:className "slds-grid"}
      (for [[label handler] links]
        [:li {:key (str "nav-" label)
              :className "slds-context-bar__item slds-context-bar__dropdown-trigger slds-dropdown-trigger slds-dropdown-trigger_hover"}
         [:a {:className "slds-context-bar__label-action" :on-click handler} label]])
      ]]]])

(defn master-detail [{:keys [title collection item-view-fn detail-view]}]
  [:div {:className "slds-grid"}
   [:div {:className "slds-col slds-size_1-of-4"}
    [:ul (for [item collection]
           ^{:key (:id item)} [:li [item-view-fn item]])]]
   [:div {:className "slds-col slds-size_3-of-4"}
    detail-view]])
