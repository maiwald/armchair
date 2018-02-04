(ns armchair.views
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [armchair.slds :as slds]))

(defn cursor-position [e]
  [(.. e -pageX) (.. e -pageY)])

(defn line-component [{:keys [id text position]} character-color]
  [:div {:className "line"
         :on-mouse-down (fn [e] (.stopPropagation e))
         :on-click (fn [e] (.stopPropagation e)
                     (dispatch [:select-line id]))
         :style {:border-color character-color
                 :left (first position)
                 :top (second position)}}
   [:p text]
   [:div {:className "drag-handle fas fa-bars"
          :on-click #(.stopPropagation %)
          :on-mouse-down (fn [e]
                           (.stopPropagation e)
                           (dispatch [:start-drag id (cursor-position e)]))}]])

(defn record-update-handler [record-event]
  (fn [id field]
    (fn [event]
      (dispatch [record-event id field (-> event .-target .-value)]))))

(def update-line-handler (record-update-handler :update-line))
(def update-character-handler (record-update-handler :update-character))

(defn line-form []
  (if-let [{:keys [id text character-id]} @(subscribe [:selected-line])]
    (let [characters @(subscribe [:characters])
          update-handler (partial update-line-handler id)]
      [:div {:className "slds-grid slds-grid_align-center"}
       [:div {:className "slds-col slds-size_6-of-12"}
        [slds/form {:title (str "Line #" id)}
         [slds/input-select {:label "Character"
                             :on-change (update-handler :character-id)
                             :options (map (fn [[k c]] [k (:display-name c)]) characters)
                             :value character-id}]
         [slds/input-textarea {:label "Text"
                               :on-change (update-handler :text)
                               :value text}]]]])))

(defn dialogue-graph []
  (let [lines @(subscribe [:lines])
        connections @(subscribe [:connections])
        characters @(subscribe [:characters])]
    [:div {:className "canvas"
           :on-click #(dispatch [:deselect-line])
           :on-mouse-move #(dispatch [:move-pointer (cursor-position %)])
           :on-mouse-down #(dispatch [:start-drag-all (cursor-position %)])
           :on-mouse-up #(dispatch [:end-drag])}
     [:div {:className "lines"}
      (for [[id line] lines]
        (let [character-id (:character-id line)
              character-color (get-in characters [character-id :color])]
          ^{:key id} [line-component line character-color]))]
     [:svg {:version "1.1"
            :baseProfile "full"
            :xmlns "http://www.w3.org/2000/svg"}
      (for [{:keys [id start end]} connections]
        ^{:key id} [:line {:className "connection"
                           :x1 (+ 200 (first start))
                           :y1 (+ 15 (second start))
                           :x2 (first end)
                           :y2 (+ 15 (second end))}])]]))

(defn dialogue-component []
  [:div {:className "container"}
   [dialogue-graph]
   [:div {:className "panel"} [line-form]]])

(defn character-form [character update-handler]
  (let [{:keys [display-name color]} character]
    [slds/form {:title display-name}
     [slds/input-text {:label "Name"
                       :on-change (update-handler :display-name)
                       :value display-name}]
     [slds/input-text {:label "Color"
                       :on-change (update-handler :color)
                       :value color}]]))

(defn character-management []
  (let [characters @(subscribe [:characters])
        selected-character @(subscribe [:selected-character])]
    [:div
     [slds/page-header "Characters"]
     [slds/master-detail {:collection (vals characters)
                          :item-view-fn (fn [{:keys [id display-name]}]
                                          [:div
                                           {:on-click #(dispatch [:select-character id])}
                                           display-name])
                          :detail-view (if selected-character
                                         [character-form selected-character (partial update-character-handler (:id selected-character))]
                                         [:div "hello detail!"])}]]))

(defn main-panel []
  (let [current-page @(subscribe [:current-page])
        pages (array-map
                "Dialogue" [dialogue-component]
                "Characters" [character-management]
                "Locations" [:div "Master/Detail"])
        link-map (map
                   (fn [name] [name #(dispatch [:show-page name])])
                   (keys pages))]
    [:div {:id "page"}
     [:div {:id "navigation"}
      [slds/global-navigation link-map current-page]]
     [:div {:id "content"}
      (get pages current-page [:div "Nothing"])
      ]]))
