(ns armchair.util
  (:require [clojure.set :refer [intersection]]))

(defn position-delta [[start-x start-y] [current-x current-y]]
  [(- current-x start-x)
   (- current-y start-y)])

(defn translate-position [position delta]
  (mapv + position delta))

(defn translate-positions [positions ids delta]
  (let [relevant-ids (intersection (-> positions keys set) ids)]
    (reduce #(update %1 %2 translate-position delta) positions relevant-ids)))

(defn map-values [f m]
  (into {} (for [[k v] m] [k (f v)])))

