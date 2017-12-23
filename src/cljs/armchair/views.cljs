(ns armchair.views
  (:require [re-frame.core :as re-frame :refer [dispatch]]))

(defn svg [width height & children]
  [:svg {:version "1.1"
         :baseProfile "full"
         :style { :height "100%" :width "100%" }
         :xmlns "http://www.w3.org/2000/svg"}
   children])

(defn line-component [{:keys [id text x y]}]
  [:div {:on-mouse-down #(dispatch [:start-drag {:id id :mouse-x (.. % -pageX) :mouse-y (.. % -pageY)}])
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
                 :top y
                 :left x
                 }}
   text])

(defn lines-component []
  (let [lines @(re-frame/subscribe [:lines])]
    [:div {:className "lines"}
     (for [line lines]
       ^{:key (:id line)} [line-component line])]))

(defn connections-component []
  (let [connections @(re-frame/subscribe [:connections])]
    [svg 800 800
     (for [{:keys [id start end]} connections]
       ^{:key id} [:line {:stroke "black"
                          :x1 (+ 200 (:x start))
                          :y1 (+ 15 (:y start))
                          :x2 (:x end)
                          :y2 (+ 15 (:y end))}])
     ]))

(defn main-panel []
  [:div {:className "container"
         :on-mouse-move #(dispatch [:drag {:mouse-x (.. % -pageX)
                                           :mouse-y (.. % -pageY)}])
         :style {:position "absolute"
                 :top 0
                 :left 0
                 :height "100%"
                 :width "100%"
                 :overflow "hidden"
                 }}
   [lines-component]
   [connections-component]])
