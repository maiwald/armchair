(ns armchair.canvas
  (:require [armchair.config :refer [tile-size]]))

(defn save! [ctx] (.save ctx))
(defn restore! [ctx] (.restore ctx))

(defn height [ctx] (.-height (.-canvas ctx)))
(defn width [ctx] (.-width (.-canvas ctx)))

(defn draw-image! [ctx image [x y]]
  (.drawImage ctx image x y))

(defn draw-image-rotated! [ctx image [x y] deg]
  (let [offset (/ tile-size 2)]
    (save! ctx)
    (.translate ctx (+ x offset) (+ y offset))
    (.rotate ctx (/ (* deg (.-PI js/Math)) 180))
    (draw-image! ctx image [(- offset) (- offset)])
    (restore! ctx)))

(defn stroke-rect! [ctx [x y] w h]
  (.strokeRect ctx x y w h))

(defn set-stroke-style! [ctx value]
  (set! (.-strokeStyle ctx) value))

(defn fill-rect! [ctx [x y] w h]
  (.fillRect ctx x y w h))

(defn set-fill-style! [ctx value]
  (set! (.-fillStyle ctx) value))

(defn set-line-width! [ctx value]
  (set! (.-lineWidth ctx) value))

(defn clear! [ctx]
  (.clearRect ctx 0 0 (width ctx) (height ctx)))
