(ns armchair.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]
            [clojure.set :refer [difference]]))

(re-frame/reg-sub
  :lines
  (fn [db]
    (map (fn [[id line]] (assoc line :id id)) (:lines db))))

(re-frame/reg-sub
  :connections
  (fn [{:keys [lines connections]}]
    (map (fn [[start end]]
           {:id (str start end)
            :start (select-keys (get lines start) [:x :y])
            :end (select-keys (get lines end) [:x :y])})
         connections)))
