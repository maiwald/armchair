(ns armchair.canvas)

(defn draw-image! [ctx image x y]
  (.drawImage ctx image x y))

(defn draw-rect! [ctx x y w h]
  (.strokeRect ctx x y w h))

(defn set-stroke-style! [ctx value]
  (set! (.-strokeStyle ctx) value))

(defn set-line-width! [ctx value]
  (set! (.-lineWidth ctx) value))

(defn clear! [ctx]
  (let [canvas (.-canvas ctx)]
    (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))))

(defn save! [ctx] (.save ctx))
(defn restore! [ctx] (.restore ctx))
