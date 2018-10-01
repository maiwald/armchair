(ns armchair.util
  (:require [clojure.set :refer [intersection]]))

(defn rect-width [[[x1 _] [x2 _]]]
  (- x2 x1))

(defn rect-height [[[_ y1] [_ y2]]]
  (- y2 y1))

(defn rect-contains? [[[x1 y1] [x2 y2]] [x y]]
  (and (<= x1 x x2)
       (<= y1 y y2)))

(defn position-delta [[start-x start-y] [current-x current-y]]
  [(- current-x start-x)
   (- current-y start-y)])

(defn translate-position
  ([position delta] (translate-position position delta +))
  ([position delta f] (if (= delta [0 0])
                        position
                        (mapv f position delta))))

(defn rect->0 [[top-left _] position]
  "Normalize a position relative to a rect to a 0,0 based rect"
  (translate-position position top-left -))

(defn translate-positions [positions ids delta]
  (let [relevant-ids (intersection (-> positions keys set) ids)]
    (reduce #(update %1 %2 translate-position delta) positions relevant-ids)))

(defn map-values [f m]
  (into {} (for [[k v] m] [k (f v)])))

(defn map-keys [f m]
  (into {} (for [[k v] m] [(f k) v])))

(defn where [property value coll]
  (filter #(= (property %) value) coll))

(defn filter-map [pred? coll]
  (into {} (filter #(pred? (second %)) coll)))

(defn filter-keys [pred? coll]
  (into {} (filter #(pred? (first %)) coll)))

(defn where-map [property value coll]
  (filter-map #(= (property %) value) coll))

(defn once [f]
  (let [called (atom false)]
    (fn [& args]
      (when-not @called
        (reset! called true)
        (apply f args)))))
