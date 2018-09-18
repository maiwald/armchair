(ns armchair.game
  (:require [clojure.core.async :refer [chan sliding-buffer put! go-loop <!]]
            [clojure.spec.alpha :as s]
            [clojure.set :refer [subset? union]]
            [armchair.canvas :as c]
            [armchair.config :refer [tile-size]]
            [armchair.textures :refer [load-textures]]
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

(s/def ::line-id pos-int?)
(s/def ::selected-option int?)

(s/def ::interaction (s/keys :req-un [::line-id
                                      ::selected-option]))

(s/def ::infos (s/coll-of pos-int? :kind set?))
(s/def ::player (s/keys :req-un [::position ::direction ::infos]))

(s/def ::uniform-level (fn [level] (apply = (map count level))))
(s/def ::level (s/and (s/coll-of vector? :kind vector?)
                      ::uniform-level))
(s/def ::enemies (s/map-of ::position pos-int?))
(s/def ::highlight ::position)

(s/def ::all-enemies-have-dialogue (fn [{:keys [enemies dialogues]}]
                                     (= (vals enemies) (keys dialogues))))
(s/def ::state (s/and (s/keys :req-un [::player ::level ::enemies]
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
  {:player {:position (tile->coord [0 12])
            :direction :right
            :infos #{}}})

(defn ^boolean walkable? [tile]
  (let [{:keys [level enemies]} @state]
    (and (= (get-in level tile) 1)
         (not (contains? enemies tile)))))

(defn interaction-tile [{{:keys [position direction]} :player}]
  (translate-position
    (coord->tile position)
    (direction-map direction)))

(defn dialogue-data [{{:keys [line-id selected-option]} :interaction
                      {player-infos :infos} :player
                      lines :lines}]
  (if-let [line (lines line-id)]
    {:text (:text line)
     :options (if (nil? (:next-line-id line))
                (list "Yeah..., whatever. Farewell.")
                (let [next-line (lines (:next-line-id line))]
                  (case (:kind next-line)
                    :npc (list "Continue...")
                    :player (->> (:options next-line)
                                 (filter #(subset? (:required-info-ids %) player-infos))
                                 (map :text)))))
     :selected-option selected-option}))

(defn ^boolean interacting? [state]
  (contains? state :interaction))

(defn interaction-option-count [state]
  (count (:options (dialogue-data state))))

(defn next-interaction [{{:keys [line-id selected-option]} :interaction
                         lines :lines}]
  (let [next-line-id (get-in lines [line-id :next-line-id])]
    (if-let [next-line (lines next-line-id)]
      (case (:kind next-line)
        :player (get-in next-line [:options selected-option :next-line-id])
        :npc next-line-id)
      next-line-id)))

;; Rendering

(def ctx (atom nil))
(def texture-atlas (atom nil))

(defn draw-texture [ctx texture coord]
  (when @texture-atlas
    (c/draw-image! ctx (@texture-atlas texture) coord)))

(defn draw-texture-rotated [ctx texture coord deg]
  (when @texture-atlas
    (c/draw-image-rotated! ctx (@texture-atlas texture) coord deg)))

(defn draw-level [ctx level]
  (let [cols (count (first level))
        rows (count level)]
    (doseq [x (range 0 rows)
            y (range 0 cols)
            :let [value (get-in level [x y])]]
      (draw-texture ctx
                    (case value
                      0 :wall
                      1 :grass)
                    (tile->coord [x y])))))

(defn draw-player [ctx player]
  (draw-texture ctx :player player))

(defn draw-enemies [ctx coords]
  (doseq [coord coords]
    (draw-texture ctx :enemy coord)))

(defn draw-highlight [ctx highlight-coord]
  (c/save! ctx)
  (c/set-stroke-style! ctx "rgb(255, 255, 0)")
  (c/set-line-width! ctx "2")
  (c/stroke-rect! ctx highlight-coord tile-size tile-size)
  (c/restore! ctx))

(defn draw-path [ctx {:keys [level highlight] {:keys [position]} :player}]
  (if highlight
    (doseq [path-tile (path/a-star
                        walkable?
                        (coord->tile position)
                        (coord->tile highlight))]
      (c/save! ctx)
      (c/set-fill-style! ctx "rgba(255, 255, 0, .2)")
      (c/fill-rect! ctx (tile->coord path-tile) tile-size tile-size)
      (c/restore! ctx))))

(defn draw-direction-indicator [ctx {{:keys [position direction]} :player}]
  (let [rotation (direction {:up 0 :right 90 :down 180 :left 270})]
    (draw-texture-rotated ctx :arrow position rotation)))

(defn draw-dialogue-box [ctx {:keys [text options selected-option]}]
  (let [w 600
        h 360
        x (/ (- (c/width ctx) w) 2)
        y (/ (- (c/height ctx) h) 2)]
    (c/save! ctx)
    (c/set-fill-style! ctx "rgba(237, 224, 142, .8)")
    (c/fill-rect! ctx [x y] w h)
    (c/set-stroke-style! ctx "rgb(200, 200, 0)")
    (c/stroke-rect! ctx [x y] w h)

    (c/set-fill-style! ctx "rgb(0, 0, 0)")
    (c/set-font! ctx "40px serif")
    (c/set-baseline! ctx "top")
    (c/draw-text! ctx "Dialogue!" (translate-position [x y] [20 20]))
    (c/set-font! ctx "18px serif")
    (c/draw-textbox! ctx text (translate-position [x y] [20 70]) (- w 40) 230)

    (c/set-baseline! ctx "middle")
    (doseq [[idx option] (map-indexed vector options)]
      (let [w (- w 40)
            h 24
            offset 6
            coord (translate-position [x y] [20 (+ 220 (* idx (+ offset h)))])]
        (c/set-fill-style! ctx "rgba(0, 0, 0, .2)")
        (c/fill-rect! ctx coord w h)

        (if (= selected-option idx)
          (c/set-stroke-style! ctx "rgb(255, 0, 0)")
          (c/set-stroke-style! ctx "rgb(0, 0, 0)"))
        (c/set-line-width! ctx "1")
        (c/stroke-rect! ctx coord w h)

        (c/set-fill-style! ctx "rgb(0, 0, 0)")
        (c/draw-text! ctx option (translate-position coord [2 (/ h 2)]))))
    (c/restore! ctx)))

(defn render [view-state]
  (when @ctx
    (c/clear! @ctx)
    (when-not (interacting? @state)
      (draw-path @ctx @state))
    (draw-player @ctx (get-in view-state [:player :position]))
    (draw-enemies @ctx (->> view-state :enemies keys (map tile->coord)))
    (when (and (not (interacting? @state))
               (contains? @state :highlight))
      (draw-highlight @ctx (:highlight @state)))
    (draw-direction-indicator @ctx view-state)
    (when (interacting? @state)
      (draw-dialogue-box @ctx (dialogue-data @state)))))

;; Input Handlers

(def move-q (atom #queue []))

(defn handle-move [direction]
  (if (interacting? @state)
    (let [option-count (interaction-option-count @state)]
      (case direction
        :up (swap! state update-in [:interaction :selected-option] #(mod (dec %) option-count))
        :down (swap! state update-in [:interaction :selected-option] #(mod (inc %) option-count))
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
        (swap! state #(let [info-ids (get-in % [:lines (get-in % [:interaction :line-id]) :info-ids])]
                        (cond-> (merge % {:interaction {:line-id next-interaction
                                                        :selected-option 0}})
                          (not (empty? info-ids)) (update-in [:player :infos] union info-ids))))))
    (if-let [enemy-id ((:enemies @state) (interaction-tile @state))]
      (swap! state assoc :interaction {:line-id (get-in @state [:dialogues enemy-id])
                                       :selected-option 0}))))

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

(defn animated-position [start duration [fx fy] [tx ty] now]
  (let [passed (- now start)
        pct (/ passed duration)
        transform (fn [f t] (+ f (round (* pct (- t f)))))]
    (if (<= pct 1)
      [(transform fx tx) (transform fy ty)]
      [tx ty])))

(defn animation-done? [start duration now]
  (< (+ start duration) now))

(defn animate-move [destination-tile move-chan]
  (let [anim-c (chan 1)
        destination (tile->coord destination-tile)
        start (* time-factor (.now js/performance))
        from (get-in @state [:player :position])
        duration tile-move-time]
    (go-loop [_ (<! anim-c)]
             (js/requestAnimationFrame
               (fn []
                 (let [now (* time-factor (.now js/performance))]
                   (if-not (animation-done? start duration now)
                     (do
                       (render (update-in @state [:player :position] #(animated-position start duration from destination now)))
                       (put! anim-c true))
                     (do
                       (swap! state assoc-in [:player :position] destination)
                       (swap! move-q pop)
                       (put! move-chan true))))))
             (recur (<! anim-c)))
    (put! anim-c true)))

(defn start-animation-loop [channel]
  (go-loop [_ (<! channel)]
           (when-let [direction (first @move-q)]
             (let [position-delta (direction-map direction)
                   new-position (translate-position (coord->tile (get-in @state [:player :position])) position-delta)]
               (swap! state assoc-in [:player :direction] direction)
               (if (walkable? new-position)
                 (animate-move new-position channel)
                 (when-not (empty? (swap! move-q pop)) (put! channel true)))))
           (recur (<! channel))))

;; Game Loop

(defn start-game [level-context entity-context data]
  (reset! state (merge initial-game-state data))
  (reset! move-q #queue [])
  (reset! ctx entity-context)
  (let [input-chan (chan)
        animation-chan (chan (sliding-buffer 1))]
    (add-watch state
               :state-update
               (fn [_ _ old-state new-state]
                 (when (not= old-state new-state)
                   (when-not (s/valid? ::state new-state)
                     (js/console.log (s/explain ::state new-state)))
                   (js/requestAnimationFrame #(render new-state)))))
    (add-watch move-q
               :animation-update
               (fn [_ _ old-state new-state]
                 (when (and (empty? old-state)
                            (some? new-state))
                   (put! animation-chan true))))
    (load-textures (fn [loaded-atlas]
                     (reset! texture-atlas loaded-atlas)

                     (c/clear! level-context)
                     (draw-level level-context (:level @state))

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
