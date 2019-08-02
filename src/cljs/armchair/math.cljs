(ns armchair.math)

(defrecord Point [x y])
(defrecord Rect [x y w h])

(defn round [x] (.round js/Math x))
(defn abs [x] (.abs js/Math x))

(defn clip [u-bound value]
  (min u-bound (max 0 value)))

(defn rect-top-left [rect]
  (Point. (:x rect) (:y rect)))

(defn rect-bottom-right [rect]
  (Point. (+ -1 (:x rect) (:w rect))
          (+ -1 (:y rect) (:h rect))))

(defn rect-contains? [rect point]
  (let [bottom-right (rect-bottom-right rect)]
    (and (<= (:x rect) (:x point) (:x bottom-right))
         (<= (:y rect) (:y point) (:y bottom-right)))))

(defn rect-intersects? [a b]
  (and (< (:x a) (+ (:x b) (:w b)))
       (< (:x b) (+ (:x a) (:w a)))
       (< (:y a) (+ (:y b) (:h b)))
       (< (:y b) (+ (:y a) (:h a)))))

(defn rect-resize [{:keys [x y w h]}
                   {:keys [top left right bottom]
                    :or [top 0 left 0 right 0 bottom 0]}]
  (Rect. (- x left) (- y top)
         (+ w left right) (+ h top bottom)))

(defn rect-scale [rect factor]
  (Rect. (* factor (:x rect))
         (* factor (:y rect))
         (* factor (:w rect))
         (* factor (:h rect))))

(defn point-delta [start end]
  [(- (:x end) (:x start))
   (- (:y end) (:y start))])

(defn rect-point [{:keys [x y]}]
  (Point. x y))

(defn translate-point [{:keys [x y] :as point} dx dy]
  (assoc point
         :x (+ x dx)
         :y (+ y dy)))

(defn relative-point
  "Convert *global* point to a Point relative to rect"
  [point rect]
  (let [[dx dy] (point-delta (rect-top-left rect) point)]
    (Point. dx dy)))
