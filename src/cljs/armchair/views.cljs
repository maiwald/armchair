(ns armchair.views
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [armchair.slds :as slds]
            [armchair.config :as config]))

;; Helpers

(defn cursor-position [e]
  [(.. e -pageX) (.. e -pageY)])

(defn record-update-handler [record-type id field]
  (let [record-event (keyword (str "update-" (name record-type)))]
    (fn [event]
      (dispatch [record-event id field (-> event .-target .-value)]))))

;; Components

(defn draggable [{:keys [id position]} component]
  [:div {:class "draggable"
         :on-mouse-down (fn [e]
                          (.preventDefault e)
                          (.stopPropagation e)
                          (dispatch [:start-drag id (cursor-position e)]))
         :on-mouse-up (fn [e]
                        (.preventDefault e)
                        (.stopPropagation e)
                        (dispatch [:end-drag id]))
         :style {:left (first position)
                 :top (second position)}}
   component])

(defn line-component [{:keys [id text position character-color]}]
  [draggable {:id id :position position}
   [:div {:class "line"
          :style {:border-color character-color
                  :width (str config/line-width "px")}}
    [:p text]
    [:div {:class "connection-handle fas fa-link"
           :on-mouse-down (fn [e]
                            (.preventDefault e)
                            (.stopPropagation e)
                            (dispatch [:start-connection id (cursor-position e)]))}]]])

(defn line-form []
  (if-let [{:keys [id text character-id]} @(subscribe [:selected-line])]
    (let [characters @(subscribe [:characters])
          update-handler (partial record-update-handler :line id)]
      [:div {:class "slds-grid slds-grid_align-center"}
       [:div {:class "slds-col slds-size_6-of-12"}
        [slds/form
         [slds/form-title (str "Line #" id)]
         [slds/input-select {:label "Character"
                             :on-change (update-handler :character-id)
                             :options (map (fn [[k c]] [k (:display-name c)]) characters)
                             :value character-id}]
         [slds/input-textarea {:label "Text"
                               :on-change (update-handler :text)
                               :value text}]]]])))

(defn dialogue-graph []
  (let [lines @(subscribe [:lines])
        connections @(subscribe [:connections])]
    [:div {:class "graph"
           :on-mouse-move #(dispatch [:move-pointer (cursor-position %)])
           :on-mouse-down (fn [e] (.preventDefault e)
                            (dispatch [:start-drag-all (cursor-position e)]))
           :on-mouse-up #(dispatch [:end-drag-all])}
     [:button {:class "new-line-button slds-button slds-button_neutral"
               :on-click #(dispatch [:create-new-line])}
      [:i {:class "slds-button__icon slds-button__icon_left fas fa-plus"}]
      "New"]
     [:div {:class "graph__items"}
      (for [[id line] lines]
          ^{:key (str "line" id)} [line-component line])]
     [:svg {:className "graph__connections"
            :version "1.1"
            :baseProfile "full"
            :xmlns "http://www.w3.org/2000/svg"}
      (for [{:keys [kind start end]} connections]
        [:line {:class kind
                :stroke-dasharray (when (= kind :drag-connection) "3, 3")
                :key (str "connection" kind start "-" end)
                :x1 (first start)
                :y1 (second start)
                :x2 (first end)
                :y2 (second end)}])]]))

(defn dialogue-component []
  [:div {:class "container"}
   [dialogue-graph]
   [:div {:class "panel"} [line-form]]])

(defn character-form-modal []
  (let [{:keys [character-id]} @(subscribe [:modal])
        update-handler (partial record-update-handler :character character-id)]
    (if-let [character (get @(subscribe [:characters]) character-id)]
      (let [{:keys [display-name color]} character]
        [slds/modal {:title display-name
                     :close-handler #(dispatch [:close-modal])
                     :content [slds/form
                               [slds/input-text {:label "Name"
                                                 :on-change (update-handler :display-name)
                                                 :value display-name}]
                               [slds/input-text {:label "Color"
                                                 :on-change (update-handler :color)
                                                 :value color}]]}]))))

(defn character-management []
  (let [characters @(subscribe [:characters])]
    [slds/resource-page "Characters"
     {:columns [:id :display-name :color :lines :actions]
      :collection (vals characters)
      :cell-views {:color slds/color-cell
                   :actions (fn [{:keys [id lines]} _]
                              [:div {:class "slds-text-align_right"}
                               (when (zero? lines)
                                 [slds/symbol-button "trash-alt" {:on-click #(dispatch [:delete-character id])}])
                               [slds/symbol-button "edit" {:on-click #(dispatch [:open-character-modal id])}]])}
      :new-resource #(dispatch [:create-new-character])}]))

(defn location-form-modal []
  (let [{:keys [location-id]} @(subscribe [:modal])
        update-handler (partial record-update-handler :location location-id)]
    (if-let [location (get @(subscribe [:locations]) location-id)]
      (let [display-name (:display-name location)]
        [slds/modal {:title display-name
                     :close-handler #(dispatch [:close-modal])
                     :content [slds/form
                               [slds/input-text {:label "Name"
                                                 :on-change (update-handler :display-name)
                                                 :value display-name}]]}]))))

(defn location-management []
  (let [locations @(subscribe [:locations])]
    [slds/resource-page "Locations"
     {:columns [:id :display-name :actions]
      :collection (vals locations)
      :cell-views {:actions (fn [{:keys [id lines]} _]
                              [:div {:class "slds-text-align_right"}
                               [slds/symbol-button "trash-alt"
                                {:on-click #(dispatch [:delete-location id])}]
                               [slds/symbol-button "edit"
                                {:on-click #(dispatch [:open-location-modal id])}]])}
      :new-resource #(dispatch [:create-new-location])}]))

(defn main-panel []
  (let [current-page @(subscribe [:current-page])
        pages (array-map
                "Dialogue" [dialogue-component]
                "Characters" [character-management]
                "Locations" [location-management])
        link-map (map
                   (fn [name] [name #(dispatch [:show-page name])])
                   (keys pages))]
    [:div {:id "page"}
     [character-form-modal]
     [location-form-modal]
     [:a {:id "reset"
          :on-click #(dispatch [:reset-db])} "reset"]
     [:div {:id "navigation"}
      [slds/global-navigation link-map current-page]]
     [:div {:id "content"}
      (get pages current-page [:div "Nothing"])]]))
