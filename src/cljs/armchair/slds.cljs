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

(defn color-cell [item column]
  [:span {:class "slds-badge"
          :style {:color "rgba(255, 255, 255, .8)"
                  :background-color (get item column)}}
   (get item column)])

(defn symbol-button [sym]
  [:button {:class "slds-button slds-button_icon-small slds-button_icon-border-filled"}
   [:svg {:class "slds-button__icon"}
    [:use {:xlinkHref (str "/assets/icons/utility-sprite/svg/symbols.svg#" sym)
           :xmlnsXlink "http://www.w3.org/1999/xlink"}]]])

(defn data-table [{:keys [columns cell-views title collection]}]
  [:div {:class "slds-grid slds-gutters"}
   [:table {:class "slds-table slds-table_bordered slds-table_cell-buffer"}
    [:thead {:class "slds-text-title_caps"}
     [:tr
      (for [column columns]
            [:th {:key column
                  :scope "col"
                  :title column}
             column])
      [:th {:title "actions"} "actions"]]]
    [:tbody
     (for [item collection]
       [:tr {:key (:id item)}
        (for [column columns]
          [:td {:key (str column (:id item))}
           (if-let [cell-view (get cell-views column)]
             [cell-view item column]
             (get item column))])
        [:td [symbol-button "delete"]]])]]])

(defn resource-page [title content-options]
  [:div {:class "slds-page-header slds-m-around_medium"}
   [:div {:class "slds-grid"}
    [:div {:class "slds-col slds-has-flexi-truncate"}
     [:h1 {:class "slds-page-header__title"} title]]
    [:div {:class "slds-col slds-no-flex"}
     [:button {:class "slds-button slds-button_neutral"} "New"]]]
   [:div {:class "slds-page-header__detail-row"}
    [data-table
     content-options]]])
