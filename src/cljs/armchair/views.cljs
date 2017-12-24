(ns armchair.views
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]))

(defn svg [width height & children]
  [:svg {:version "1.1"
         :baseProfile "full"
         :style { :height "100%" :width "100%" }
         :xmlns "http://www.w3.org/2000/svg"}
   children])

(defn line-component [{:keys [id text position]}]
  [:div {:on-mouse-down #(dispatch [:start-drag id])
         :on-mouse-up #(dispatch [:end-drag])
         :style {:position "absolute"
                 :-webkit-user-select "none"
                 :box-sizing "border-box"
                 :user-select "none"
                 :width 200
                 :min-height 30
                 :border "1px solid black"
                 :border-radius 15
                 :background "white"
                 :padding "5px 10px"
                 :left (first position)
                 :top (second position)
                 }}
   text])

(defn lines-component []
  (let [lines @(subscribe [:lines])]
    [:div {:className "lines"}
     (for [line lines]
       ^{:key (:id line)} [line-component line])]))

(defn connections-component []
  (let [connections @(subscribe [:connections])]
    [svg 800 800
     (for [{:keys [id start end]} connections]
       ^{:key id} [:line {:stroke "black"
                          :x1 (+ 200 (first start))
                          :y1 (+ 15 (second start))
                          :x2 (first end)
                          :y2 (+ 15 (second end))}])
     ]))

(defn main-panel []
  [:div {:className "container"
         :on-mouse-move #(dispatch [:move-pointer [(.. % -pageX) (.. % -pageY)]])
         :style {:position "absolute"
                 :top 0
                 :left 0
                 :height "100%"
                 :width "100%"
                 :overflow "hidden"
                 }}
   [lines-component]
   [connections-component]])
