(ns armchair.game.core
  (:require [clojure.core.async :refer [<!
                                        chan
                                        close!
                                        go-loop
                                        put!
                                        sliding-buffer]]
            [clojure.spec.alpha :as s]
            [clojure.set :refer [subset? union]]
            [armchair.game.canvas :as c]
            [armchair.game.pathfinding :as path]
            [armchair.config :refer [tile-size]]
            [armchair.textures :refer [texture-set load-textures]]
            [armchair.util :as u]))

;; Definitions

(def time-factor 1)
(def tile-move-time 150) ; milliseconds
(def direction-map {:up [0 -1]
                    :down [0 1]
                    :left [-1 0]
                    :right [1 0]})


(s/def ::state (s/and (s/keys :req-un [::player ::dialogue-states ::switches]
                              :opt-un [::interaction ::animation])))

(s/def ::player (s/keys :req-un [:player/position
                                 :player/direction]))
(s/def :player/position :type/point)
(s/def :player/direction #{:up :down :left :right})

(s/def ::dialogue-states (s/map-of :entity/id :entity/id))
(s/def ::switches (s/map-of :entity/id (s/nilable :entity/id)))

(s/def ::interaction (s/keys :req-un [::line-id ::selected-option]))
(s/def ::animation (s/keys :req-un [::start ::destination]))

(s/def ::line-id :entity/id)
(s/def ::selected-option int?)

;; State

(def state (atom nil))
(def data (atom nil))

(defn walkable? [tile]
  (let [{l :location-id} (:player @state)
        {:keys [walk-set npcs]} (get-in @data [:locations l])]
    (and (contains? walk-set tile)
         (not (contains? npcs tile)))))

(defn interaction-tile [{{:keys [position direction]} :player}]
  (u/translate-point
    position
    (direction-map direction)))

(defn dialogue-data [{{:keys [line-id selected-option]} :interaction}]
  (let [line (get-in @data [:lines line-id])]
    {:text (:text line)
     :options (->> (:options line)
                   (map :text))
     :selected-option selected-option}))

(defn interacting? [state]
  (contains? state :interaction))

(defn interaction-option-count [{{:keys [line-id]} :interaction}]
  (count (get-in @data [:lines line-id :options])))

(defn interaction-option [{{:keys [line-id selected-option]} :interaction}]
  (get-in @data [:lines line-id :options selected-option]))

;; Rendering

(def ctx (atom nil))
(def texture-atlas (atom nil))

(defn tile-visible? [camera [x y]]
  (u/rect-intersects?
    camera
    [(u/tile->coord [x y])
     (u/tile->coord [(inc x) (inc y)])]))

(defn draw-texture [ctx texture coord]
  (when (some? @texture-atlas)
    (c/draw-image! ctx (get @texture-atlas texture (@texture-atlas :missing_texture)) coord)))

(defn draw-texture-rotated [ctx texture coord deg]
  (when (some? @texture-atlas)
    (c/draw-image-rotated! ctx (@texture-atlas texture) coord deg)))

(defn draw-background [ctx [[left top] [right bottom]] background camera]
  (doseq [x (range left (inc right))
          y (range top (inc bottom))
          :when (tile-visible? camera [x y])
          :let [texture (get background [x y])]]
    (draw-texture ctx texture (u/tile->coord [x y]))))

(defn draw-player [ctx player]
  (draw-texture ctx :player player))

(defn draw-npcs [ctx npcs camera]
  (doseq [[tile {texture :texture}] npcs]
    (when (tile-visible? camera tile)
      (draw-texture ctx texture (u/tile->coord tile)))))

; (defn draw-highlight [ctx highlight-coord]
;   (c/save! ctx)
;   (c/set-stroke-style! ctx "rgb(255, 255, 0)")
;   (c/set-line-width! ctx "2")
;   (c/stroke-rect! ctx highlight-coord tile-size tile-size)
;   (c/restore! ctx))

; (defn draw-path [ctx {:keys [highlight] {:keys [position]} :player}]
;   (if highlight
;     (doseq [path-tile (path/a-star
;                         walkable?
;                         (u/coord->tile position)
;                         (u/coord->tile highlight))]
;       (c/save! ctx)
;       (c/set-fill-style! ctx "rgba(255, 255, 0, .2)")
;       (c/fill-rect! ctx (u/tile->coord path-tile) tile-size tile-size)
;       (c/restore! ctx))))

(defn draw-direction-indicator [ctx {{:keys [position direction]} :player}]
  (let [rotation (direction {:up 0 :right 90 :down 180 :left 270})]
    (draw-texture-rotated ctx :arrow position rotation)))

(defn draw-dialogue-box [ctx {:keys [text options selected-option]}]
  (let [w 600
        h 300
        x (/ (- (c/width ctx) w) 2)
        y (/ (- (c/height ctx) h) 2)]
    (c/save! ctx)
    (c/set-fill-style! ctx "rgba(230, 230, 230, .9)")
    (c/fill-rect! ctx [x y] w h)
    (c/set-stroke-style! ctx "rgb(0, 0, 0)")
    (c/stroke-rect! ctx [x y] w h)

    (c/set-fill-style! ctx "rgb(0, 0, 0)")
    (c/set-baseline! ctx "top")
    (c/set-font! ctx "23px serif")
    (let [offset (c/draw-textbox! ctx text (u/translate-point [x y] [20 20]) (- w 40))]
      (c/set-font! ctx "18px serif")
      (let [w (- w 40)
            padding 6
            spacing 6]
        (loop [idx 0
               y (+ y 40 offset)
               [option & options] options]
          (when (some? option)
            (c/set-fill-style! ctx "rgb(0, 0, 0)")
            (if (= selected-option idx)
              (c/set-stroke-style! ctx "rgb(255, 0, 0)")
              (c/set-stroke-style! ctx "rgb(0, 0, 0)"))
            (let [coord (u/translate-point [x y] [20 0])
                  height (+ padding (c/draw-textbox! ctx option (u/translate-point coord [7 padding]) w))]
              (if (= selected-option idx)
                (c/set-fill-style! ctx "rgba(0, 0, 0, .1)")
                (c/set-fill-style! ctx "rgba(0, 0, 0, 0)"))
              (c/fill-rect! ctx coord w height)

              (c/set-line-width! ctx "1")
              (c/stroke-rect! ctx coord w height)

              (recur (inc idx)
                     (+ y height spacing)
                     options))))))
    (c/restore! ctx)))

(defn draw-camera [[left-top right-bottom]]
  (when (some? @ctx)
    (c/stroke-rect! @ctx
                    left-top
                    right-bottom)))

(defn render [view-state]
  (when (some? @ctx)
    (let [l (get-in view-state [:player :location-id])
          [[left top] :as camera] (:camera view-state)
          {:keys [npcs dimension background]} (get-in @data [:locations l])]
      (c/clear! @ctx)
      (c/set-fill-style! @ctx "rgb(0, 0, 0)")
      (c/fill-rect! @ctx [0 0] (c/width @ctx) (c/height @ctx))
      ;; NOTE: try copying drawn tiles to prevent gaps
      (let [a (/ (c/width @ctx) (u/rect-width camera))
            d (/ (c/height @ctx) (u/rect-height camera))
            e (* a (- left))
            f (* d (- top))]
        (c/set-transform! @ctx a 0 0 d e f))
      (draw-background @ctx dimension background camera)
      (draw-player @ctx (get-in view-state [:player :position]))
      (draw-npcs @ctx npcs camera)
      (draw-direction-indicator @ctx view-state)
      (c/reset-transform! @ctx)
      (when (interacting? view-state)
        (draw-dialogue-box @ctx (dialogue-data view-state))))))


;; Input Handlers

(def move-q (atom #queue []))

(defn handle-move [direction]
  (if (interacting? @state)
    (if-let [next-index-fn (get {:up dec :down inc} direction)]
      (swap! state
             update-in [:interaction :selected-option]
             (fn [index] (mod (next-index-fn index)
                              (interaction-option-count @state)))))
    (swap! move-q conj direction)))

(defn handle-interact []
  (if (interacting? @state)
    (let [current-line-id (get-in @state [:interaction :line-id])
          current-triggers (get-in @data [:lines current-line-id :triggers])
          {option-triggers :triggers :keys [next-line-id]} (interaction-option @state)]
      (swap! state
             #(-> %
                  (update :dialogue-states merge
                          (:dialogue-states current-triggers)
                          (:dialogue-states option-triggers))
                  (update :switches merge
                          (:switches current-triggers)
                          (:switches option-triggers))))
      (if (some? next-line-id)
        (swap! state assoc :interaction {:line-id next-line-id
                                         :selected-option 0})
        (swap! state dissoc :interaction)))
    (let [l (get-in @state [:player :location-id])
          npcs (get-in @data [:locations l :npcs])]
      (if-let [dialogue-id (get-in npcs [(interaction-tile @state) :dialogue-id])]
        (let [line-id (get-in @state [:dialogue-states dialogue-id])]
          (swap! state assoc :interaction {:line-id line-id
                                           :selected-option 0}))))))

(defn start-input-loop [channel]
  (go-loop [[command payload :as message] (<! channel)]
           (when message
             (let [handler (case command
                             :move handle-move
                             :interact handle-interact)]
               (handler payload)
               (recur (<! channel))))))

;; Animations

(defn round [x] (.round js/Math x))

(defn animated-position [start [fx fy] [tx ty] now]
  (let [passed (- now start)
        pct (/ passed tile-move-time)
        transform (fn [f t] (+ f (round (* pct (- t f)))))]
    (if (< pct 1)
      [(transform fx tx) (transform fy ty)]
      [tx ty])))

(defn animation-done? [start now]
  (< (+ start tile-move-time) now))

;; state updates and view state

(defn camera-rect [player-coord]
  (let [h 9
        w 15
        camera-delta [(* (quot w -2) tile-size)
                      (* (quot h -2) tile-size)]
        left-top (u/translate-point
                   player-coord
                   camera-delta)
        right-bottom (u/translate-point
                       player-coord
                       (mapv - camera-delta)
                       [tile-size tile-size])]
    [left-top right-bottom]))

(defn update-state-animation [state now]
  (if-let [{:keys [start destination]} (:animation state)]
    (if (animation-done? start now)
      (let [new-state (dissoc state :animation)
            location-id (get-in state [:player :location-id])]
        (if-let [new-location-id (get-in @data [:locations
                                                location-id
                                                :outbound-connections
                                                destination])]
          (let [new-position (get-in @data [:locations
                                            new-location-id
                                            :inbound-connections
                                            location-id])]
            (reset! move-q #queue [])
            (update new-state :player merge {:location-id new-location-id
                                             :position new-position}))
          (assoc-in new-state [:player :position] destination)))
      state)
    state))

(defn update-state-movement [state now]
  (if (not (contains? state :animation))
    (if-let [direction (peek @move-q)]
      (let [new-position (u/translate-point (get-in state [:player :position])
                                            (direction-map direction))]
        (swap! move-q pop)
        (cond-> (assoc-in state [:player :direction] direction)
          (walkable? new-position)
          (assoc :animation {:start now
                             :destination new-position})))
      state)
    state))

(defn update-state [state now]
  (-> state
      (update-state-animation now)
      (update-state-movement now)))

(defn view-state [state now]
  (as-> state s
    (update-in s [:player :position]
               (fn [player-tile]
                 (let [player-coord (u/tile->coord player-tile)]
                   (if-let [{:keys [start destination]} (:animation state)]
                     (animated-position start
                                        player-coord
                                        (u/tile->coord destination)
                                        now)
                     player-coord))))
    (assoc s :camera (camera-rect (get-in s [:player :position])))))

;; Game Loop

(defn start-game [context game-data]
  (reset! state (:initial-state game-data))
  (reset! data game-data)
  (reset! ctx context)
  (reset! move-q #queue [])
  (let [input-chan (chan)
        quit (atom false)
        prev-view-state (atom nil)]
    (load-textures
      (fn [loaded-atlas]
        (reset! texture-atlas loaded-atlas)
        (start-input-loop input-chan)
        (js/requestAnimationFrame
          (fn game-loop []
            (let [now (* time-factor (.now js/performance))
                  new-state (swap! state #(update-state % now))
                  view-state (view-state new-state now)]
              (when-not (s/valid? ::state new-state)
                (s/explain ::state new-state))
              (when-not (= @prev-view-state view-state)
                (reset! prev-view-state view-state)
                (render view-state))
              (if-not @quit
                (js/requestAnimationFrame game-loop)))))))
    {:input input-chan
     :quit quit}))

(defn end-game [{:keys [input quit]}]
  (reset! quit true)
  (close! input)
  (reset! state nil)
  (reset! data nil)
  (reset! move-q nil)
  (reset! ctx nil))
