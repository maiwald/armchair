(ns armchair.views
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]))

(defn cursor-position [e]
  [(.. e -pageX) (.. e -pageY)])

(defn line-component [{:keys [id text position]} character-color]
  [:div {:className "line"
         :on-mouse-down (fn [e] (.stopPropagation e))
         :style {:border-color character-color
                 :left (first position)
                 :top (second position)}}
   [:p text]
   [:div {:className "drag-handle fas fa-bars"
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

(defn line-form [line]
  (let [characters (subscribe [:characters])]
    (fn [{:keys [id text character-id]}]
      [:form {:className "line-form"}
       [:div id]
       [:select {:defaultValue character-id}
        (for [[id {:keys [display-name]}] @characters]
          ^{:key (str "c" id)} [:option {:value id} display-name])]
       [:input {:type "text" :defaultValue text}]])))

(defn main-panel []
  (let [dummy-line (get @(subscribe [:lines]) 2)]
    [:div {:className "container"}
     [:div {:className "canvas"
            :on-mouse-move #(dispatch [:move-pointer (cursor-position %)])
            :on-mouse-down #(dispatch [:start-drag-all (cursor-position %)])
            :on-mouse-up #(dispatch [:end-drag])}
      [lines-component]
      [connections-component]]
     [:div {:className "panel"} [line-form dummy-line]]]))
