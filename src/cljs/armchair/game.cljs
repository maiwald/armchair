(ns armchair.game
  (:require [clojure.core.async :refer [chan sliding-buffer put! take! go go-loop <! >!]]
            [armchair.canvas :as c]
            [armchair.config :refer [tile-size]]
            [armchair.position :refer [apply-delta]]
            [armchair.pathfinding :as path]))

;; Definitions

(def time-factor 1)
(def tile-move-time 150) ; milliseconds
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
   :player (tile->coord [0 12])
   :player-direction :right
   :enemies {(tile->coord [6 6]) #(js/alert "Wow, you interacted with guy 1!")
             (tile->coord [5 12]) #(js/alert "Cool, you interacted with guy 2!") }
   :level [[ 0 0 0 0 0 0 0 0 0 0 0 0 1 0 ]
           [ 0 1 0 1 1 1 1 1 1 1 1 1 1 0 ]
           [ 0 1 1 1 0 0 0 0 0 0 1 1 0 0 ]
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

(defn ^boolean walkable? [tile]
  (let [level (:level @state)
        enemy-tiles (->> @state :enemies keys (map coord->tile) set)]
    (and (= (get-in level tile) 1)
         (not (contains? enemy-tiles tile)))))

;; Textures

(def textures ["grass"
               "wall"
               "player"
               "enemy"
               "arrow"])

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

(defn draw-texture-rotated [texture coord deg]
  (when @texture-atlas
    (c/draw-image-rotated! @ctx (@texture-atlas texture) coord deg)))

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

(defn draw-enemies [coords]
  (doseq [coord coords]
    (draw-texture :enemy coord)))

(defn draw-highlight [highlight-coord]
  (when highlight-coord
    (c/save! @ctx)
    (c/set-stroke-style! @ctx "rgb(255, 255, 0)")
    (c/set-line-width! @ctx "2")
    (c/stroke-rect! @ctx highlight-coord tile-size tile-size)
    (c/restore! @ctx)))

(defn draw-path [{:keys [level player highlight]}]
  (if highlight
    (doseq [path-tile (path/a-star
                        walkable?
                        (coord->tile player)
                        (coord->tile highlight))]
      (c/save! @ctx)
      (c/set-fill-style! @ctx "rgba(255, 255, 0, .2)")
      (c/fill-rect! @ctx (tile->coord path-tile) tile-size tile-size)
      (c/restore! @ctx))))

(defn draw-direction-indicator [{:keys [player player-direction]}]
  (let [rotation (player-direction {:up 0
                                    :right 90
                                    :down 180
                                    :left 270})]
    (draw-texture-rotated :arrow player rotation)))

(defn render [view-state]
  (when @ctx
    (c/clear! @ctx)
    (draw-level (:level @state))
    (draw-path @state)
    (draw-player (:player view-state))
    (draw-enemies (-> view-state :enemies keys))
    (draw-highlight (:highlight @state))
    (draw-direction-indicator view-state)))

;; Input Handlers

(def move-q (atom #queue []))

(defn handle-move [direction]
  (swap! move-q conj direction))

(defn handle-cursor-position [coord]
  (swap! state assoc :highlight (when coord (normalize-to-tile coord))))

(defn handle-interact []
  (let [player-tile (coord->tile (:player @state))
        interaction-tile (apply-delta player-tile (direction-map (:player-direction @state)))]
    (when-let [interaction-fn (get-in @state [:enemies (tile->coord interaction-tile)])]
      (interaction-fn))))

(defn start-input-loop [channel]
  (go-loop [[cmd payload] (<! channel)]
           (let [handler (case cmd
                           :cursor-position handle-cursor-position
                           :move handle-move
                           :interact handle-interact
                           identity)]
             (handler payload)
             (recur (<! channel)))))

;; Animations

(defn round [x] (.round js/Math x))

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

(defn animation-done? [{start :start duration :duration} now]
  (< (+ start duration) now))

(defn animate-move [destination-tile move-chan]
  (let [anim-c (chan 1)
        destination (tile->coord destination-tile)
        animation {:start (* time-factor (.now js/performance))
                   :from (:player @state)
                   :to destination
                   :duration tile-move-time}]
    (go-loop [_ (<! anim-c)]
             (js/requestAnimationFrame
               (fn []
                 (let [now (* time-factor (.now js/performance))]
                   (if-not (animation-done? animation now)
                     (do
                       (render (update @state :player #(animated-position animation now)))
                       (put! anim-c true))
                     (do
                       (swap! state assoc :player destination)
                       (swap! move-q pop)
                       (put! move-chan true))))))
             (recur (<! anim-c)))
    (put! anim-c true)))

(defn start-animation-loop [channel]
  (go-loop [_ (<! channel)]
           (when-let [direction (first @move-q)]
             (let [position-delta (direction-map direction)
                   new-position (apply-delta (coord->tile (:player @state)) position-delta)]
               (swap! state assoc :player-direction direction)
               (if (walkable? new-position)
                 (animate-move new-position channel)
                 (when-not (empty? (swap! move-q pop)) (put! channel true)))))
           (recur (<! channel))))

;; Game Loop

(defn start-game [context]
  (reset! state initial-game-state)
  (reset! move-q #queue [])
  (reset! ctx context)
  (let [input-chan (chan)
        animation-chan (chan (sliding-buffer 1))]
    (add-watch state
               :state-update
               (fn [_ _ old-state new-state]
                 (when (not= old-state new-state)
                   (js/requestAnimationFrame #(render new-state)))))
    (add-watch move-q
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
  (remove-watch move-q :animation-update)
  (reset! state nil)
  (reset! move-q nil)
  (reset! ctx nil))
