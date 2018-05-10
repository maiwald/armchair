(ns armchair.game
  (:require [clojure.core.async :refer [chan put! go <! >!]]))

(def level
  [[ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 ]
   [ 0 1 0 1 1 1 1 1 1 1 1 0 1 1 ]
   [ 0 1 1 1 0 0 0 0 0 0 1 1 1 0 ]
   [ 0 1 1 1 0 0 0 0 0 0 1 1 1 0 ]
   [ 0 1 0 1 0 0 1 1 1 1 1 0 1 0 ]
   [ 0 0 0 1 1 1 1 0 0 1 0 0 1 1 ]
   [ 0 0 1 1 0 0 1 0 0 1 0 1 1 0 ]
   [ 0 1 1 1 1 0 1 0 0 1 1 1 1 1 ]
   [ 0 1 0 1 1 0 1 1 1 1 0 0 1 1 ]
   [ 0 1 0 1 1 0 1 1 1 1 0 0 1 1 ]
   [ 0 1 0 1 1 0 1 1 1 1 0 0 1 1 ]
   [ 0 0 0 0 1 0 1 0 0 1 0 0 0 0 ]
   [ 0 1 0 1 1 1 1 1 1 1 1 0 1 1 ]
   [ 0 1 1 1 0 0 0 0 0 0 1 1 1 0 ]
   [ 0 1 1 1 0 0 0 0 0 0 1 1 1 0 ]
   [ 1 1 0 1 0 0 1 1 1 1 1 0 1 0 ]
   [ 1 0 0 1 1 1 1 0 0 1 0 0 1 1 ]
   [ 1 0 1 1 0 0 1 0 0 1 0 1 1 0 ]
   [ 1 0 1 1 1 0 1 0 0 1 0 1 1 1 ]
   [ 1 0 1 1 1 0 1 0 0 1 0 1 1 1 ]
   [ 1 0 1 1 1 0 1 1 0 1 0 0 0 1 ]
   [ 1 0 1 1 1 0 1 1 0 1 0 0 0 1 ]
   [ 1 0 0 1 1 0 1 0 0 1 0 0 0 1 ]
   [ 1 0 0 1 1 1 1 1 1 1 0 0 1 1 ]
   [ 1 0 0 0 0 0 0 0 0 0 0 0 0 0 ]
   ])

(def textures ["grass"
               "wall"])

(def context (atom nil))
(def texture-atlas (atom nil))

(defn get-texture-atlas-chan []
  (let [atlas (atom {})
        loaded (chan (count textures))]
    (run! (fn [texture-name]
            (let [image (js/Image.)]
              (set! (.-onload image) #(put! loaded [texture-name image]))
              (set! (.-src image) (str "/images/" texture-name ".png"))))
          textures)
    (go
      (while (not= (count @atlas) (count textures))
        (let [[texture-name texture-image] (<! loaded)]
          (swap! atlas assoc (keyword texture-name) texture-image)))
      @atlas)))

(defn draw-texture [texture x y]
  (.drawImage @context (@texture-atlas texture) x y))

(defn draw-level []
  (let [cols (count (first level))
        rows (count level)]
    (doseq [x (range 0 rows)
            y (range 0 cols)
            :let [value (get-in level [x y])]]
      (draw-texture ({0 :wall 1 :grass} value)
                    (* 32 x)
                    (* 32 y)))))

(defn start-game [c]
  (.log js/console "start-game")
  (reset! context c)
  (go
    (reset! texture-atlas (<! (get-texture-atlas-chan)))
    (draw-level)))

(defn end-game []
  (.log js/console "end-game"))
