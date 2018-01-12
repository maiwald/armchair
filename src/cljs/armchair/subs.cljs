(ns armchair.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [clojure.set :refer [difference]]
            [armchair.position :refer [translate-positions]]))

(reg-sub :db-lines #(:lines %))
(reg-sub :characters #(:characters %))
(reg-sub :db-connections #(:connections %))
(reg-sub :db-dragging #(:dragging %))
(reg-sub :db-pointer #(:pointer %))
(reg-sub :db-selected-line-id #(:selected-line-id %))

(reg-sub
  :selected-line
  :<- [:db-lines]
  :<- [:db-selected-line-id]
  (fn [[lines id]]
    (get lines id)))

(reg-sub
  :lines-with-drag
  :<- [:db-lines]
  :<- [:db-dragging]
  (fn [[lines dragging] _]
    (if-let [{:keys [line-ids delta]} dragging]
      (translate-positions lines line-ids delta)
      lines)))

(reg-sub
  :lines
  :<- [:lines-with-drag]
  (fn [lines-with-drag] lines-with-drag))

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
