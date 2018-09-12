(ns armchair.canvas
  (:require [armchair.config :refer [tile-size]]))

(defn save! [ctx] (.save ctx))
(defn restore! [ctx] (.restore ctx))

(defn height [ctx] (.-height (.-canvas ctx)))
(defn width [ctx] (.-width (.-canvas ctx)))

(defn draw-image! [ctx image [x y]]
  (.drawImage ctx image 0 0 tile-size tile-size x y tile-size tile-size))

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

(defn set-font! [ctx value]
  (set! (.-font ctx) value))

(defn set-baseline! [ctx value]
  (set! (.-textBaseline ctx) value))

(defn draw-text! [ctx text [x y]]
  (.fillText ctx text x y))

(defn text-width [ctx text]
  (.-width (.measureText ctx text)))

(defn draw-textbox! [ctx text [x y] w h]
  (let [line-height (* 1.2 (js/parseInt (.-font ctx)))
        words (clojure.string/split text " ")]
    (loop [index 0
           line (first words)
           remaining (rest words)]
      (if (empty? remaining)
        (draw-text! ctx line [x (+ y (* index line-height))])
        (let [currentWord (first remaining)
              nextLine (str line " " currentWord)
              line-y (+ y (* index line-height))]
          (assert (> w (text-width ctx line))
                  "Text is too wide for given container!")
          (when (>= h (+ line-height (* index line-height)))
            (if (>= (text-width ctx nextLine) w)
              (do (draw-text! ctx line [x line-y])
                  (recur (inc index) currentWord (rest remaining)))
              (recur index (str line " " currentWord) (rest remaining)))))))))

(defn clear! [ctx]
  (.clearRect ctx 0 0 (width ctx) (height ctx)))
