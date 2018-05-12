(ns armchair.game
  (:require [clojure.core.async :refer [chan put! take! go go-loop <! >!]]
            [armchair.canvas :as c]
            [armchair.pathfinding :as path]))


;; Conversion Helpers

(defn tile->coord [[tx ty]]
  [(* 32 tx) (* 32 ty)])

(defn coord->tile [[cx cy]]
  [(quot cx 32) (quot cy 32)])

(defn normalize-to-tile [coord]
  (-> coord coord->tile tile->coord))

;; State

(def initial-game-state
  {:highlight nil
   :player (tile->coord [1 13])
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

;; Textures

(def textures ["grass"
               "wall"
               "player"])

(def texture-atlas (atom nil))

(defn get-texture-atlas-chan []
  (let [atlas (atom {})
        loaded (chan)]
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

;; Rendering

(def ctx (atom nil))

(defn draw-texture [texture coord]
  (when @texture-atlas
    (c/draw-image! @ctx (@texture-atlas texture) coord)))

(defn draw-level [level]
  (let [cols (count (first level))
        rows (count level)]
    (doseq [x (range 0 rows)
            y (range 0 cols)
            :let [value (get-in level [x y])]]
      (draw-texture ({0 :wall 1 :grass} value)
                    (tile->coord [x y])))))

(defn draw-player [player]
  (draw-texture :player player))

(defn draw-highlight [highlight-coord]
  (when highlight-coord
    (doto @ctx
      c/save!
      (c/set-stroke-style! "rgba(255, 255, 0, .7)")
      (c/set-line-width! "2")
      (c/stroke-rect! highlight-coord 32 32)
      c/restore!)))

(defn draw-path [{:keys [level player highlight]}]
  (if highlight
    (doseq [path-tile (path/a-star
                        level
                        (coord->tile player)
                        (coord->tile highlight))]
      (doto @ctx
        c/save!
        (c/set-fill-style! "rgba(255, 255, 0, .2)")
        (c/set-line-width! "2")
        (c/fill-rect! (tile->coord path-tile) 32 32)
        c/restore!))))

(defn render [state]
  (.debug js/console "render")
  (js/requestAnimationFrame
    #(do
       (c/clear! @ctx)
       (draw-level (:level state))
       (draw-player (:player state))
       (draw-path state)
       (draw-highlight (:highlight state)))))


;; Input Handlers

(defn handle-cursor-position [state coord]
  (assoc state :highlight (when coord (normalize-to-tile coord))))

(defn start-input-loop [state input-chan]
  (go-loop [[cmd payload] (<! input-chan)]
           (let [handler (case cmd
                           :cursor-position handle-cursor-position
                           (fn [state _] state))]
             (reset! state (handler @state payload)))
           (recur (<! input-chan))))

;; Game Loop

(def state (atom nil))

(defn start-game [context]
  (.debug js/console "start-game")
  (reset! state initial-game-state)
  (reset! ctx context)
  (add-watch state
             :state-update
             (fn [_ _ old-state new-state]
               (if-not (= old-state new-state)
                 (render new-state))))
  (let [input-chan (chan)]
    (take! (get-texture-atlas-chan)
           #(do (reset! texture-atlas %)
                (start-input-loop state input-chan)
                (render @state)))
    input-chan))

(defn end-game []
  (.debug js/console "end-game")
  (remove-watch state :state-update)
  (reset! state nil)
  (reset! ctx nil))
