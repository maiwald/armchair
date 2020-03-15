(ns armchair.math)

(defrecord Point [x y])
(defrecord Rect [x y w h])

(defn round [x] (.round js/Math x))
(defn abs [x] (.abs js/Math x))

(defn clip [u-bound value]
  (min u-bound (max 0 value)))

(defn rect-top-left [{:keys [x y]}]
  (Point. x y))

(defn rect-bottom-right [{:keys [x y w h]}]
  (Point. (+ -1 x w) (+ -1 y h)))

(defn rect->point-seq [{:keys [x y w h]}]
  (for [x (range x (+ x w))
        y (range y (+ y h))]
    (Point. x y)))

(defn rect-contains? [rect point]
  (let [bottom-right (rect-bottom-right rect)]
    (and (<= (:x rect) (:x point) (:x bottom-right))
         (<= (:y rect) (:y point) (:y bottom-right)))))

(defn containing-rect [points]
  (let [[min-p max-p] (reduce
                        (fn [[{min-x :x min-y :y}
                              {max-x :x max-y :y}]
                             {:keys [x y]}]
                          [(Point. (min min-x x) (min min-y y))
                           (Point. (max max-x x) (max max-y y))])
                        [(Point. ##Inf ##Inf)
                         (Point. ##-Inf ##-Inf)]
                        points)]
    (Rect. (:x min-p)
           (:y min-p)
           (abs (- (:x max-p) (:x min-p)))
           (abs (- (:y max-p) (:y min-p))))))

(defn rect-intersects? [a b]
  (and (< (:x a) (+ (:x b) (:w b)))
       (< (:x b) (+ (:x a) (:w a)))
       (< (:y a) (+ (:y b) (:h b)))
       (< (:y b) (+ (:y a) (:h a)))))

(defn rect-resize [{:keys [x y w h]}
                   {:keys [top left right bottom]}]
  (Rect. (- x left) (- y top)
         (+ w left right) (+ h top bottom)))

(defn rect-scale [rect factor]
  (Rect. (* factor (:x rect))
         (* factor (:y rect))
         (* factor (:w rect))
         (* factor (:h rect))))

(defn point-scale [point factor]
  (Point. (* factor (:x point))
          (* factor (:y point))))

(defn point-delta [start end]
  [(- (:x end) (:x start))
   (- (:y end) (:y start))])

(defn rect-point [{:keys [x y]}]
  (Point. x y))

(defn translate-point [{:keys [x y] :as point} dx dy]
  (Point. (+ x dx) (+ y dy)))

(defn global-point
  "Convert rect-relative point to a global point"
  [point rect]
  (let [[dx dy] (point-delta (rect-top-left rect) point)]
    (Point. dx dy)))

(defn relative-point
  "Convert global point to a rect-relative point"
  [point rect]
  (let [{:keys [x y]} (rect-top-left rect)]
    (translate-point point x y)))
