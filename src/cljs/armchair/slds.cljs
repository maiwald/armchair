(ns armchair.slds)

(defn form [{:keys [title]} & children]
  [:div {:class "slds-form slds-form_stacked"}
   [:div {:class "slds-text-heading_small"} title]
   (map-indexed #(with-meta %2 {:key %1}) children)])

(defn input-text [{:keys [label on-change value]}]
  (let [id (gensym "input-text")]
    [:div {:class "slds-form-element"}
     [:label {:class "slds-form-element__label" :for id} label]
     [:div {:class "slds-form-element__control"}
      [:input {:class "slds-input"
               :id id
               :on-change on-change
               :value value}]]]))

(defn input-select [{:keys [label on-change value options]}]
  (let [id (gensym "input-select")]
    [:div {:class "slds-form-element"}
     [:label {:class "slds-form-element__label" :for id} label]
     [:div {:class "slds-form-element__control"}
      [:div {:class "slds-select_container"}
       [:select {:class "slds-select"
                 :id id
                 :on-change on-change
                 :value value}
        (for [[k v] options] [:option {:key (str id k) :value k} v])]]]]))

(defn input-textarea [{:keys [label on-change value]}]
  (let [id (gensym "input-textarea")]
    [:div {:class "slds-form-element"}
     [:label {:class "slds-form-element__label" :for id} label]
     [:div {:class "slds-form-element__control"}
      [:textarea {:class "slds-textarea"
                  :id id
                  :on-change on-change
                  :value value}]]]))

(defn global-navigation [links current-page]
  [:div {:class "slds-context-bar"}
   [:div {:class "slds-context-bar__primary"}
    [:div {:class "slds-context-bar__item slds-no-hover"}
     [:span {:class "slds-context-bar__label-action slds-context-bar__app-name"} "Armchair"]]
    [:nav {:class "slds-context-bar__secondary"}
     [:ul {:class "slds-grid"}
      (for [[label handler] links]
        [:li {:key (str "nav-" label)
              :class (str "slds-context-bar__item"
                          (when (= label current-page) " slds-is-active"))}
         [:a {:class "slds-context-bar__label-action" :on-click handler} label]])]]]])

(defn master-detail [{:keys [title collection item-view-fn detail-view]}]
  [:div {:class "slds-grid slds-gutters"}
   [:div {:class "slds-col slds-size_1-of-4"}
    [:ul (for [item collection]
           ^{:key (:id item)} [:li [item-view-fn item]])]]
   [:div {:class "slds-col slds-size_3-of-4"}
    detail-view]])

(defn page-header [title]
  [:div {:class "slds-page-header slds-m-around_medium"}
   [:div {:class "slds-grid"}
    [:div {:class "slds-col slds-has-flexi-truncate"}
     [:h1 {:class "slds-page-header__title"} title]]
    [:div {:class "slds-col slds-no-flex"}
     [:button {:class "slds-button slds-button_neutral"} "New"]]]])
