(ns armchair.game
  (:require [clojure.core.async :refer [chan put! take! go go-loop <! >!]]
            [armchair.canvas :as c]
            [armchair.position :refer [apply-delta]]
            [armchair.pathfinding :as path]))

;; Definitions

(def tile-size 32)
(def tile-move-time 200) ; miliseconds
(def direction-map {:up [0 -1]
                    :down [0 1]
                    :left [-1 0]
                    :right [1 0]})

;; Conversion Helpers

(defn tile->coord [[tx ty]]
  [(* tile-size tx) (* tile-size ty)])

(defn coord->tile [[cx cy]]
  [(quot cx tile-size) (quot cy tile-size)])

(defn normalize-to-tile [coord]
  (-> coord coord->tile tile->coord))

;; State

(def state (atom nil))

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
      (c/stroke-rect! highlight-coord tile-size tile-size)
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
        (c/fill-rect! (tile->coord path-tile) tile-size tile-size)
        c/restore!))))

(defn render [state]
  (when @ctx
    (c/clear! @ctx)
    (draw-level (:level state))
    (draw-player (:player state))
    (draw-path state)
    (draw-highlight (:highlight state))))

;; Animations

(def animations (atom (list)))

(defn abs [x] (.abs js/Math x))
(defn round [x] (.round js/Math x))

(defn move [from to duration]
  {:start (.now js/performance)
   :from from
   :to to
   :duration duration})

(defn move-sequence [coords duration-per-tile]
  (let [start (.now js/performance)
        segments (partition-all 2 (interleave coords (rest coords)))]
    (map-indexed (fn [idx [from to]]
                   {:start (+ start (* idx duration-per-tile))
                    :from from
                    :to to
                    :duration duration-per-tile})
                 segments)))

(defn animated-position [animation now]
  (let [{start :start
         [fx fy] :from
         [tx ty] :to
         duration :duration} animation
        passed (- now start)
        pct (/ passed duration)]
    (if (<= pct 1)
      (let [dx (- tx fx)
            dy (- ty fy)]
        [(+ fx (round (* pct dx)))
         (+ fy (round (* pct dy)))])
      [tx ty])))

(defn animation-done? [animation now]
  (let [{start :start
         duration :duration} animation]
    (< (+ start duration) now)))

(defn start-animation-loop [channel]
  (go-loop [_ (<! channel)]
           (if-let [animation (first @animations)]
             (let [now (.now js/performance)]
               (swap! state assoc :player (animated-position animation now))
               (if (animation-done? animation now)
                 (swap! animations rest))
               (js/requestAnimationFrame #(put! channel true))))
           (recur (<! channel))))

;; Input Handlers

(defn handle-cursor-position [coord]
  (swap! state assoc :highlight (when coord (normalize-to-tile coord))))

(defn handle-animate [coord]
  (let [path-tiles (path/a-star (:level @state)
                                (coord->tile (:player @state))
                                (coord->tile (normalize-to-tile coord)))]
    (reset! animations (move-sequence (map tile->coord path-tiles) tile-move-time))))

(defn handle-move [direction]
  (let [position-delta (direction-map direction)
        new-position (apply-delta (coord->tile (:player @state)) position-delta)]
    (if (path/walkable? (:level @state) new-position)
      (swap! state assoc :player (tile->coord new-position)))))

(defn start-input-loop [channel]
  (go-loop [[cmd payload] (<! channel)]
           (let [handler (case cmd
                           :cursor-position handle-cursor-position
                           :move handle-move
                           :animate handle-animate
                           identity)]
             (handler payload)
             (recur (<! channel)))))

;; Game Loop

(defn start-game [context]
  (reset! state initial-game-state)
  (reset! animations nil)
  (reset! ctx context)
  (let [input-chan (chan)
        animation-chan (chan)]
    (add-watch state
               :state-update
               (fn [_ _ old-state new-state]
                 (when (not= old-state new-state)
                   (js/requestAnimationFrame #(render new-state)))))
    (add-watch animations
               :animation-update
               (fn [_ _ old-state new-state]
                 (when (and (empty? old-state)
                            (some? new-state))
                   (put! animation-chan true))))
    (take! (get-texture-atlas-chan)
           (fn [loaded-atlas]
             (reset! texture-atlas loaded-atlas)
             (start-input-loop input-chan)
             (start-animation-loop animation-chan)
             (js/requestAnimationFrame #(render @state))))
    input-chan))

(defn end-game []
  (remove-watch state :state-update)
  (remove-watch animations :animation-update)
  (reset! state nil)
  (reset! animations nil)
  (reset! ctx nil))
