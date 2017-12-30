(ns armchair.position)

(defn position-delta [[start-x start-y] [current-x current-y]]
  [(- start-x current-x)
   (- start-y current-y)])

(defn translate-position [item delta]
  (update item :position #(mapv - % delta)))
