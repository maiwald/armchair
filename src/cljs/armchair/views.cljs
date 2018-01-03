(ns armchair.views
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]))

(defn cursor-position [e]
  [(.. e -pageX) (.. e -pageY)])

(defn line-component [{:keys [id text position]}]
  [:div {:className "line"
         :on-mouse-down (fn [e] (.stopPropagation e))
         :style {:left (first position)
                 :top (second position)}}
   [:p text]
   [:div {:className "drag-handle fas fa-bars"
          :on-mouse-down (fn [e]
                           (.stopPropagation e)
                           (dispatch [:start-drag id (cursor-position e)]))}]
    ])

(defn lines-component []
  (let [lines @(subscribe [:lines])]
    [:div {:className "lines"}
     (for [line lines]
       ^{:key (:id line)} [line-component line])]))

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
                          :y2 (+ 15 (second end))}])
     ]))

(defn main-panel []
  [:div {:className "container"
         :on-mouse-move #(dispatch [:move-pointer (cursor-position %)])
         :on-mouse-down #(dispatch [:start-drag-all (cursor-position %)])
         :on-mouse-up #(dispatch [:end-drag])}
   [lines-component]
   [connections-component]])
