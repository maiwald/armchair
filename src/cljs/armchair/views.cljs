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

(defn lines-component []
  (let [lines @(subscribe [:lines])
        characters @(subscribe [:characters])]
    [:div {:className "lines"}
     (for [[id line] lines]
       (let [character-color (get-in characters [(:character-id line) :color])]
         ^{:key id} [line-component line character-color]))]))

(defn connections-component []
  (let [connections @(subscribe [:connections])]
    [:svg {:version "1.1"
           :baseProfile "full"
           :xmlns "http://www.w3.org/2000/svg"}
     (for [{:keys [id start end]} connections]
       ^{:key id} [:line {:className "connection"
                          :x1 (+ 200 (first start))
                          :y1 (+ 15 (second start))
                          :x2 (first end)
                          :y2 (+ 15 (second end))}])]))

(defn update-line-handler [id field]
  #(dispatch [:update-line id field (-> % .-target .-value)]))

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

(defn dialog-component []
  [:div {:className "container"}
   [:div {:className "canvas"
          :on-click #(dispatch [:deselect-line])
          :on-mouse-move #(dispatch [:move-pointer (cursor-position %)])
          :on-mouse-down #(dispatch [:start-drag-all (cursor-position %)])
          :on-mouse-up #(dispatch [:end-drag])}
    [lines-component]
    [connections-component]]
   [:div {:className "panel"} [line-form]]])

(defn main-panel []
  (let [current-page @(subscribe [:current-page])]
    [:div {:id "page"}
     [:div {:id "navigation"}
      [slds/global-navigation
       {"Home" #(dispatch [:show-page "dialog"])
        "Characters" #(dispatch [:show-page "master-detail"])
        "Locations" #(dispatch [:show-page "master-detail"])}]]
     [:div {:id "content"}
      (case current-page
        "dialog" [dialog-component]
        "master-detail" [:div "Master/Detail"]
        [:div "Nothing here"])]]))
