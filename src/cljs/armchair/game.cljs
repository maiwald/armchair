(ns armchair.game
  (:require [clojure.core.async :refer [chan sliding-buffer put! take! go go-loop <! >!]]
            [clojure.spec.alpha :as s]
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

(s/def ::position (s/tuple number? number?))
(s/def ::direction #{:up :down :left :right})

(s/def ::character-id int?)
(s/def ::line-id int?)
(s/def ::selected-option-index int?)

(s/def ::interaction (s/keys :req-un [::character-id
                                      ::line-id
                                      ::selected-option-index]))

(s/def ::player ::position)
(s/def ::player-direction ::direction)

(s/def ::uniform-level (fn [level] (apply = (map count level))))
(s/def ::level (s/and (s/coll-of vector? :kind vector?)
                      ::uniform-level))
(s/def ::enemies (s/map-of int? ::position))
(s/def ::highlight ::position)

(s/def ::all-enemies-have-dialogue (fn [{:keys [enemies dialogues]}]
                                     (= (keys enemies) (keys dialogues))))
(s/def ::state (s/and (s/keys :req-un [::player ::player-direction ::level ::enemies]
                              :opt-un [::highlight ::interaction])
                      ::all-enemies-have-dialogue))

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
  {:player (tile->coord [0 12])
   :player-direction :right})

(defn ^boolean walkable? [tile]
  (let [level (:level @state)
        enemy-tiles (->> @state :enemies vals (map coord->tile) set)]
    (and (= (get-in level tile) 1)
         (not (contains? enemy-tiles tile)))))

(defn interaction-tile [state]
  (translate-position
    (coord->tile (:player state))
    (direction-map (:player-direction state))))

(defn ^boolean interacting? [state]
  (contains? state :interaction))

(defn interacting-line [state]
  (let [{:keys [character-id line-id]} (:interaction state)]
    (get-in state [:dialogues character-id :lines line-id])))

(defn interaction-options [state]
  (let [{:keys [character-id line-id]} (:interaction state)
        next-line-id (get-in state [:dialogues character-id :lines line-id :next-line-id])]
    (if (nil? next-line-id)
      (list {:text "Yeah..., whatever. Farewell."})
      (let [next-line (get-in state [:dialogues character-id :lines next-line-id])]
        (case (:kind next-line)
          :player (:options next-line)
          :npc (list {:text "Continue..."}))))))

(defn next-interaction [state]
  (let [{:keys [character-id line-id selected-option-index]} (:interaction state)
        next-line-id (get-in state [:dialogues character-id :lines line-id :next-line-id])]
    (if-let [next-line (get-in state [:dialogues character-id :lines next-line-id])]
      (case (:kind next-line)
        :player (get-in next-line [:options selected-option-index :next-line-id])
        :npc next-line-id)
      next-line-id)))

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
  (c/save! @ctx)
  (c/set-stroke-style! @ctx "rgb(255, 255, 0)")
  (c/set-line-width! @ctx "2")
  (c/stroke-rect! @ctx highlight-coord tile-size tile-size)
  (c/restore! @ctx))

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

(defn draw-dialogue-box [text options selected-option]
  (let [w 600
        h 360
        x (/ (- (c/width @ctx) w) 2)
        y (/ (- (c/height @ctx) h) 2)]
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
    (c/restore! @ctx)))

(defn render [view-state]
  (when @ctx
    (c/clear! @ctx)
    (draw-level (:level @state))
    (when-not (interacting? @state)
      (draw-path @state))
    (draw-player (:player view-state))
    (draw-enemies (-> view-state :enemies vals))
    (when (and (not (interacting? @state))
               (contains? @state :highlight))
      (draw-highlight (:highlight @state)))
    (draw-direction-indicator view-state)
    (when (interacting? @state)
      (draw-dialogue-box
        (:text (interacting-line @state))
        (map :text (interaction-options @state))
        (get-in @state [:interaction :selected-option-index])))))

;; Input Handlers

(def move-q (atom #queue []))

(defn handle-move [direction]
  (if (interacting? @state)
    (let [option-count (count (interaction-options @state))]
      (case direction
        :up (swap! state update-in [:interaction :selected-option-index] #(mod (dec %) option-count))
        :down (swap! state update-in [:interaction :selected-option-index] #(mod (inc %) option-count))
        :else))
    (swap! move-q conj direction)))

(defn handle-cursor-position [coord]
  (if coord
    (swap! state assoc :highlight (normalize-to-tile coord))
    (swap! state dissoc :highlight)))

(defn handle-interact []
  (if (interacting? @state)
    (let [next-interaction (next-interaction @state)]
      (if (nil? next-interaction)
        (swap! state dissoc :interaction)
        (swap! state update :interaction merge {:line-id next-interaction
                                                :selected-option-index 0})))
    (let [tile-to-enemy (into {} (map (fn [[k v]] [(coord->tile v) k]) (:enemies @state)))]
      (if-let [enemy-id (tile-to-enemy (interaction-tile @state))]
        (let [dialogue (get-in @state [:dialogues enemy-id])]
          (swap! state assoc :interaction {:character-id enemy-id
                                           :line-id (:initial-line-id dialogue)
                                           :selected-option-index 0}))))))

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
                 (when-not (s/valid? ::state new-state)
                   (.log js/console (s/explain ::state new-state)))
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
