(ns armchair.position
  (:require [clojure.set :refer [intersection]]))

(defn position-delta [[start-x start-y] [current-x current-y]]
  [(- current-x start-x)
   (- current-y start-y)])

(defn apply-delta [position delta]
  (mapv + position delta))

(defn translate-positions [positions ids delta]
  (let [relevant-ids (intersection (-> positions keys set) ids)]
    (reduce #(update %1 %2 apply-delta delta) positions relevant-ids)))
