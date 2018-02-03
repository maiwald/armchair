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

(defn update-line-handler [id field]
  #(dispatch [:update-line id field (-> % .-target .-value)]))

(defn update-character-handler [id field]
  #(dispatch [:update-character id field (-> % .-target .-value)]))

(defn line-form []
  (if-let [{:keys [id text character-id]} @(subscribe [:selected-line])]
    (let [characters @(subscribe [:characters])]
      [:div {:className "slds-grid slds-grid_align-center"}
       [:div {:className "slds-col slds-size_6-of-12"}
        [slds/form {:title (str "Line #" id)}
         [slds/input-select {:label "Character"
                             :on-change (update-line-handler id :character-id)
                             :options (map (fn [[k c]] [k (:display-name c)]) characters)
                             :value character-id}]
         [slds/input-textarea {:label "Text"
                               :on-change (update-line-handler id :text)
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

(defn character-form [character]
  (let [{:keys [id display-name color]} character]
    [slds/form {:title display-name}
     [slds/input-text {:label "Name"
                       :on-change (update-character-handler id :display-name)
                       :value display-name}]
     [slds/input-text {:label "Color"
                       :on-change (update-character-handler id :color)
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
                                         [character-form selected-character]
                                         [:div "hello detail!"])}]]))

(defn main-panel []
  (let [current-page @(subscribe [:current-page])]
    [:div {:id "page"}
     [:div {:id "navigation"}
      [slds/global-navigation
       {"Home" #(dispatch [:show-page "dialogue"])
        "Characters" #(dispatch [:show-page "characters"])
        "Locations" #(dispatch [:show-page "master-detail"])}]]
     [:div {:id "content"}
      (case current-page
        "dialogue" [dialogue-component]
        "characters" [character-management]
        "master-detail" [:div "Master/Detail"]
        [:div "Nothing here"])]]))
