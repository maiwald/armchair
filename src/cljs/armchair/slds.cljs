(ns armchair.slds)

(defn form [& children]
  [:div {:class "slds-form slds-form_stacked"}
   (map-indexed #(with-meta %2 {:key %1}) children)])

(defn form-title [title]
  [:div {:class "slds-text-heading_small"} title])

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
                 :value (or value "nil")}
        [:option {:key (str id "nil") :value "nil" :disabled "disabled"}]
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

(defn add-button [label click-handler]
  [:button {:class "slds-button slds-button_neutral"
            :on-click click-handler}
   [:i {:class "slds-button__icon slds-button__icon_left fas fa-plus"}] label])

(defn symbol-button [sym options]
  [:button (merge
             {:class "slds-button slds-button_icon-small slds-button_icon-border-filled"}
             options)
   [:i {:class (str "slds-button__icon fas fa-" sym)} ]])

(defn data-table [{:keys [table-id columns cell-views title collection]}]
  [:div {:class "slds-grid slds-gutters"}
   [:table {:class "slds-table slds-table_bordered slds-table_cell-buffer"}
    [:thead {:class "slds-text-title_caps"}
     [:tr
      (for [column columns]
        [:th {:key (str table-id column)
              :scope "col"
              :title column}
         column])]]
      [:tbody
       (for [item collection]
         [:tr {:key (str table-id (:id item))}
          (for [column columns]
            [:td {:key (str table-id (:id item) column)}
             (if-let [cell-view (get cell-views column)]
               [cell-view item column]
               (get item column))])])]]])

(defn resource-page [title content-options]
  [:div {:class "slds-page-header slds-m-around_medium"}
   [:div {:class "slds-grid"}
    [:div {:class "slds-col slds-has-flexi-truncate"}
     [:h1 {:class "slds-page-header__title"} title]]
    [:div {:class "slds-col slds-no-flex"}
     [add-button "New" (:new-resource content-options)]]]
   [:div {:class "slds-page-header__detail-row"}
    [data-table
     (assoc content-options :table-id title)]]])

(defn modal [{:keys [title close-handler content]}]
  [:div
   [:section {:class "slds-modal slds-fade-in-open"}
    [:div {:class "slds-modal__container"}
     [:header {:class "slds-modal__header"}
      [:button {:class "slds-button slds-button_icon slds-modal__close slds-button_icon-inverse"
                :on-click close-handler
                :title "Close"}
       [:svg {:class "slds-button__icon slds-button__icon_large"}
        [:use {:xlinkHref "/assets/icons/utility-sprite/svg/symbols.svg#close", :xmlnsXlink "http://www.w3.org/1999/xlink"}]]]
      [:h2 {:class "slds-text-heading_medium slds-hyphenate"} title]]
     [:div {:class "slds-modal__content slds-p-around_medium"}
      content]
     [:footer {:class "slds-modal__footer"}
      [:button {:class "slds-button slds-button_brand"
                :on-click close-handler} "Ok"]]]]
   [:div {:class "slds-backdrop slds-backdrop_open"}]])
