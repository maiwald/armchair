(ns armchair.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [clojure.set :refer [difference]]
            [armchair.db :as db]
            [armchair.position :refer [translate-positions]]))

(reg-sub :db-lines #(:lines %))
(reg-sub :db-characters #(:characters %))
(reg-sub :db-connections #(:connections %))
(reg-sub :db-dragging #(:dragging %))
(reg-sub :db-pointer #(:pointer %))
(reg-sub :db-selected-line-id #(:selected-line-id %))
(reg-sub :db-selected-character-id #(:selected-character-id %))

(reg-sub :current-page #(:current-page %))

(reg-sub
  :characters
  :<- [:db-characters]
  :<- [:db-lines]
  (fn [[characters lines]]
    (reduce-kv
      (fn [acc id character]
        (let [line-count (db/line-count-for-character lines id)]
          (assoc acc id (assoc character :lines line-count))))
      {}
      characters)))

(reg-sub
  :selected-line
  :<- [:db-lines]
  :<- [:db-selected-line-id]
  (fn [[lines id]]
    (get lines id)))

(reg-sub
  :selected-character
  :<- [:characters]
  :<- [:db-selected-character-id]
  (fn [[characters id]]
    (get characters id)))

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
