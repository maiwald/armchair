(ns armchair.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [clojure.set :refer [difference]]
            [armchair.position :refer [update-drag]]))

(reg-sub :db-lines #(:lines %))
(reg-sub :db-connections #(:connections %))
(reg-sub :db-dragging #(:dragging %))
(reg-sub :db-pointer #(:pointer %))

(reg-sub
  :lines-with-drag
  :<- [:db-lines]
  :<- [:db-dragging]
  :<- [:db-pointer]
  (fn [[lines dragging pointer] _]
    (if-let [{:keys [line-id start]} dragging]
      (update lines line-id update-drag start pointer)
      lines)))

(reg-sub
  :lines
  :<- [:lines-with-drag]
  (fn [lines-with-drag] (vals lines-with-drag)))

(reg-sub
  :connections
  :<- [:lines-with-drag]
  :<- [:db-connections]
  (fn [[lines connections]]
    (map (fn [[start end]]
           {:id (str start end)
            :start (get-in lines [start :position])
            :end (get-in lines [end :position])})
         connections)))
