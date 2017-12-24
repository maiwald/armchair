(ns armchair.position)

(defn position-delta [[start-x start-y] [current-x current-y]]
  [(- start-x current-x)
   (- start-y current-y)])

(defn update-drag [item start current]
  (update item :position #(mapv - % (position-delta start current))))
