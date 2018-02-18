(ns armchair.position)

(defn position-delta [[start-x start-y] [current-x current-y]]
  [(- current-x start-x)
   (- current-y start-y)])

(defn apply-delta [position delta]
  (mapv + position delta))

(defn translate-position [item delta]
  (update item :position apply-delta delta))

(defn translate-positions [items ids delta]
  (reduce #(update %1 %2 translate-position delta) items ids))
