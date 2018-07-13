(ns armchair.game
  (:require [clojure.core.async :refer [chan sliding-buffer put! take! go go-loop <! >!]]
            [armchair.canvas :as c]
            [armchair.config :refer [tile-size]]
            [armchair.util :refer [map-values translate-position]]
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
   :interacting-with nil
   :selected-option nil})

(defn ^boolean walkable? [tile]
  (let [level (:level @state)
        enemy-tiles (->> @state :enemies vals (map coord->tile) set)]
    (and (= (get-in level tile) 1)
         (not (contains? enemy-tiles tile)))))

(defn interaction-tile []
  (-> @state
      :player
      coord->tile
      (translate-position (direction-map (:player-direction @state)))))

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

(defn draw-dialogue-box [{:keys [interacting-with selected-option dialogues]}]
  (when (some? interacting-with)
    (let [w 600
          h 360
          x (/ (- (c/width @ctx) w) 2)
          y (/ (- (c/height @ctx) h) 2)
          text (get dialogues interacting-with)
          options '("My name does not matter!" "I could ask you the same!" "We have met before. In the land far beyond.")]
      (c/save! @ctx)
      (c/set-fill-style! @ctx "rgba(237, 224, 142, .8)")
      (c/fill-rect! @ctx [x y] w h)
      (c/set-stroke-style! @ctx "rgb(200, 200, 0)")
      (c/stroke-rect! @ctx [x y] w h)

      (c/set-fill-style! @ctx "rgb(0, 0, 0)")
      (c/set-font! @ctx "40px serif")
      (c/set-baseline! @ctx "top")
      (c/draw-text! @ctx "Dialogue!" (translate-position [x y] [20 20]))
      (c/set-font! @ctx "18px serif")
      (c/draw-textbox! @ctx text (translate-position [x y] [20 70]) (- w 40) 230)

      (c/set-baseline! @ctx "middle")
      (doseq [[idx option] (map-indexed vector options)]
        (let [w (- w 40)
              h 24
              offset 6
              coord (translate-position [x y] [20 (+ 220 (* idx (+ offset h)))])]
          (c/set-fill-style! @ctx "rgba(0, 0, 0, .2)")
          (c/fill-rect! @ctx coord w h)

          (if (= selected-option idx)
            (c/set-stroke-style! @ctx "rgb(255, 0, 0)")
            (c/set-stroke-style! @ctx "rgb(0, 0, 0)"))
          (c/set-line-width! @ctx "1")
          (c/stroke-rect! @ctx coord w h)

          (c/set-fill-style! @ctx "rgb(0, 0, 0)")
          (c/draw-text! @ctx option (translate-position coord [2 (/ h 2)]))))
      (c/restore! @ctx))))

(defn render [view-state]
  (when @ctx
    (c/clear! @ctx)
    (draw-level (:level @state))
    (draw-path @state)
    (draw-player (:player view-state))
    (draw-enemies (-> view-state :enemies vals))
    (draw-highlight (:highlight @state))
    (draw-direction-indicator view-state)
    (draw-dialogue-box @state)))

;; Input Handlers

(def move-q (atom #queue []))

(defn handle-move [direction]
  (if (nil? (:interacting-with @state))
    (swap! move-q conj direction)
    (case direction
      :up (swap! state update :selected-option #(mod (dec %) 3))
      :down (swap! state update :selected-option #(mod (inc %) 3))
      :else)))

(defn handle-cursor-position [coord]
  (swap! state assoc :highlight (when coord (normalize-to-tile coord))))

(defn handle-interact []
  (if (nil? (:interacting-with @state))
    (let [tile-to-enemy (into {} (map (fn [[k v]] [(coord->tile v) k]) (:enemies @state)))]
      (if-let [enemy-id (tile-to-enemy (interaction-tile))]
        (swap! state merge {:interacting-with enemy-id
                            :selected-option 0})))
    (swap! state merge {:interacting-with nil
                        :selected-option nil})))

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
                   new-position (translate-position (coord->tile (:player @state)) position-delta)]
               (swap! state assoc :player-direction direction)
               (if (walkable? new-position)
                 (animate-move new-position channel)
                 (when-not (empty? (swap! move-q pop)) (put! channel true)))))
           (recur (<! channel))))

;; Game Loop

(defn start-game [context data]
  (reset! state (merge initial-game-state
                       (update data :enemies #(map-values tile->coord %))))
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
