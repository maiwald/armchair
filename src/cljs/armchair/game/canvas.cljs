(ns armchair.game.canvas
  (:require [clojure.string :refer [split]]
            [armchair.config :refer [tile-size]]
            [armchair.math :refer [Point]]))

(defn save! [ctx] (.save ctx))
(defn restore! [ctx] (.restore ctx))

(defn set-transform! [ctx a b c d e f]
  (.setTransform ctx
                 a b c
                 d e f))

(defn reset-transform! [ctx]
  (.setTransform ctx
                 1 0 0
                 1 0 0))

(defn height [ctx] (.-height (.-canvas ctx)))
(defn width [ctx] (.-width (.-canvas ctx)))

(defn draw-image!
  ([ctx image {:keys [x y]}]
   (.drawImage ctx image
               0 0 tile-size tile-size
               x y tile-size tile-size))
  ([ctx image s d]
   (.drawImage ctx image
               (:x s) (:y s) tile-size tile-size
               (:x d) (:y d) tile-size tile-size))
  ([ctx image s [sw sh] d [dw dh]]
   (.drawImage ctx image
               (:x s) (:y s) sw sh
               (:x d) (:y d) dw dh)))

(defn draw-image-rotated! [ctx image {:keys [x y]} deg]
  (let [offset (/ tile-size 2)]
    (save! ctx)
    (.translate ctx (+ x offset) (+ y offset))
    (.rotate ctx (/ (* deg (.-PI js/Math)) 180))
    (draw-image! ctx image [(- offset) (- offset)])
    (restore! ctx)))

(defn stroke-rect! [ctx {:keys [x y w h]}]
  (.strokeRect ctx x y w h))

(defn set-stroke-style! [ctx value]
  (set! (.-strokeStyle ctx) value))

(defn fill-rect! [ctx {:keys [x y w h]}]
  (.fillRect ctx x y w h))

(defn set-fill-style! [ctx value]
  (set! (.-fillStyle ctx) value))

(defn set-line-width! [ctx value]
  (set! (.-lineWidth ctx) value))

(defn set-font! [ctx value]
  (set! (.-font ctx) value))

(defn set-baseline! [ctx value]
  (set! (.-textBaseline ctx) value))

(defn draw-text! [ctx text {:keys [x y]}]
  (.fillText ctx text x y))

(defn text-width [ctx text]
  (.-width (.measureText ctx text)))

(defn draw-textbox! [ctx text {:keys [x y]} w]
  (let [line-height (* 1.2 (js/parseInt (.-font ctx)))
        words (split text " ")]
    (loop [index 0
           line (first words)
           remaining (rest words)]
      (if (empty? remaining)
        (do
          (draw-text! ctx line (Point. x (+ y (* index line-height))))
          (* (inc index) line-height))
        (let [currentWord (first remaining)
              nextLine (str line " " currentWord)
              line-y (+ y (* index line-height))]
          (assert (> w (text-width ctx line))
                  "Text is too wide for given container!")
          (if (>= (text-width ctx nextLine) w)
            (do (draw-text! ctx line (Point. x line-y))
                (recur (inc index) currentWord (rest remaining)))
            (recur index (str line " " currentWord) (rest remaining))))))))

(defn clear! [ctx]
  (.clearRect ctx 0 0 (width ctx) (height ctx)))
