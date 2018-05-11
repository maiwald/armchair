(ns armchair.game
  (:require [clojure.core.async :refer [chan put! take! go go-loop <! >!]]
            [armchair.canvas :as c]))


(def initial-game-state
  {:cursor nil
   :player [(* 32 1) (* 32 13)]
   :level [[ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 ]
           [ 0 1 0 1 1 1 1 1 1 1 1 0 1 1 ]
           [ 0 1 1 1 0 0 0 0 0 0 1 1 1 0 ]
           [ 0 1 1 1 0 0 0 0 0 0 1 1 1 0 ]
           [ 0 1 0 1 0 0 1 1 1 1 1 0 1 0 ]
           [ 0 0 0 1 1 1 1 0 0 1 0 0 1 0 ]
           [ 0 0 1 1 0 0 1 0 0 1 0 1 1 0 ]
           [ 0 1 1 1 1 0 1 0 0 1 1 1 1 0 ]
           [ 0 1 0 1 1 0 1 1 1 1 0 0 1 0 ]
           [ 0 1 0 1 1 0 1 1 1 1 0 0 1 0 ]
           [ 0 1 0 1 1 0 1 1 1 1 0 0 1 0 ]
           [ 0 0 0 0 1 0 1 0 0 1 0 0 0 0 ]
           [ 0 1 0 1 1 1 1 1 1 1 1 0 1 0 ]
           [ 0 1 1 1 0 0 0 0 0 0 1 1 1 0 ]
           [ 0 1 1 1 0 0 0 0 0 0 1 1 1 0 ]
           [ 0 1 0 1 0 0 1 1 1 1 1 0 1 0 ]
           [ 0 1 0 1 1 1 1 0 0 1 0 0 1 0 ]
           [ 0 1 1 1 0 0 1 0 0 1 0 1 1 0 ]
           [ 0 1 1 1 1 0 1 0 0 1 0 1 1 0 ]
           [ 0 0 0 0 0 0 1 0 0 1 0 1 1 0 ]
           [ 1 1 1 1 1 0 1 1 0 1 0 1 0 0 ]
           [ 0 1 1 1 1 0 1 1 0 1 1 1 0 0 ]
           [ 0 1 0 1 1 0 1 0 0 1 0 1 0 0 ]
           [ 0 1 0 1 1 1 1 1 1 1 0 1 1 0 ]
           [ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 ]
           ]
   })

(def textures ["grass"
               "wall"
               "player"])

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
  (when @texture-atlas
    (c/draw-image! @context (@texture-atlas texture) x y)))

(defn draw-level [level]
  (let [cols (count (first level))
        rows (count level)]
    (doseq [x (range 0 rows)
            y (range 0 cols)
            :let [value (get-in level [x y])]]
      (draw-texture ({0 :wall 1 :grass} value)
                    (* 32 x)
                    (* 32 y)))))

(defn draw-player [[x y]]
  (draw-texture :player x y))

(defn draw-highlight [highlight]
  (if-let [[cursor-x cursor-y] highlight]
    (let [
           x (* (quot cursor-x 32) 32)
           y (* (quot cursor-y 32) 32)]
    (doto @context
        c/save!
        (c/set-stroke-style! "rgba(255, 255, 0, .7)")
        (c/set-line-width! "2")
        (c/draw-rect! x y 32 32)
        c/restore!))))

(defn render [state]
  (js/requestAnimationFrame
    #(do
       (c/clear! @context)
       (draw-level (:level state))
       (draw-player (:player state))
       (draw-highlight (:cursor state)))))

(defn game-loop [input-chan]
  (let [state (atom initial-game-state)]
    (go-loop [[cmd payload] (<! input-chan)]
             (case cmd
               :highlight (swap! state assoc :cursor payload))
             (recur (<! input-chan)))
    (add-watch state
               :state-update
               (fn [_ _ old-state new-state]
                 (if-not (= old-state new-state)
                   (render new-state))))
    (render @state))
  input-chan)

(defn start-game [c]
  (.log js/console "start-game")
  (reset! context c)
  (let [input-chan (chan)]
    (take! (get-texture-atlas-chan)
           #(do
              (reset! texture-atlas %)
              (game-loop input-chan)))
    input-chan))

(defn end-game []
  (.log js/console "end-game"))
