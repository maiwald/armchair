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
            [armchair.config :refer [tile-size
                                     camera-tile-width
                                     camera-tile-height
                                     camera-scale]]
            [armchair.textures :refer [load-textures sprite-lookup]]
            [armchair.math :as m]
            [armchair.util :as u]
            [com.rpl.specter
             :refer [nthpath ALL]
             :refer-macros [select transform]]))

;; Definitions

(def time-factor 1)
(def tile-move-time 200) ; milliseconds
(def direction-map {:up [0 -1]
                    :down [0 1]
                    :left [-1 0]
                    :right [1 0]})

(s/def ::state (s/and (s/keys :req-un [::player ::switches]
                              :opt-un [::interaction ::animation])))

(s/def ::player (s/keys :req-un [:player/position
                                 :player/direction]
                        :req-opt [:player/texture]))
(s/def :player/position :type/point)
(s/def :player/direction #{:up :down :left :right})

(s/def ::switches (s/map-of :entity/id (s/nilable :entity/id)))

(s/def ::interaction (s/keys :req-un [:interaction/text
                                      :interaction/options
                                      :interaction/selected-option]))
(s/def ::animation (s/keys :req-un [::start ::destination]))

(s/def :interaction/line-id :entity/id)
(s/def :interaction/selected-option int?)
(s/def :interaction/options (s/coll-of
                              :interaction/option
                              :kind vector?))

(s/def :interaction/option
  (s/keys :req-un [:option/text
                   :option/trigger-changes]
          :opt-un [:option/next-line-id]))

;; State

(def state (atom nil))
(def data (atom nil))

(defn walkable? [tile]
  (let [{l :location-id} (:player @state)
        {:keys [dimension blocked npcs]} (get-in @data [:locations l])]
    (and
      (m/rect-contains? dimension tile)
      (not (or (contains? blocked tile)
               (contains? npcs tile))))))

(defn interaction-tile [{{:keys [position direction]} :player}]
  (apply
    m/translate-point
    position
    (direction-map direction)))

(defn dialogue-data [{:keys [interaction]}]
  (-> interaction
      (select-keys [:text :options :selected-option])
      (update :options #(mapv :text %))))

(defn interacting? [state]
  (contains? state :interaction))

;; Rendering

(def ctx (atom nil))
(def texture-atlas (atom nil))

(defn tile-visible? [camera {:keys [x y]}]
  (m/rect-intersects? camera
                      (m/Rect. (* x tile-size) (* y tile-size)
                               tile-size tile-size)))

(defn draw-texture [ctx texture coord]
  (when (some? @texture-atlas)
    (c/draw-image! ctx (get @texture-atlas texture (@texture-atlas :missing_texture)) coord)))

(defn draw-sprite-texture [ctx texture dest-coord]
  (when (some? @texture-atlas)
    (if-let [[file [x-offset y-offset]] (get sprite-lookup texture)]
      (if-let [sprite-sheet (get @texture-atlas file)]
        (c/draw-image! ctx sprite-sheet (m/Point. x-offset y-offset) dest-coord)
        (c/draw-image! ctx (@texture-atlas :missing_texture) dest-coord))
      (c/draw-image! ctx (@texture-atlas :missing_texture) dest-coord))))

(defn draw-texture-rotated [ctx texture coord deg]
  (when (some? @texture-atlas)
    (c/draw-image-rotated! ctx (@texture-atlas texture) coord deg)))

(defn draw-background [ctx rect background camera]
  (when (some? background)
    (doseq [x (range (:x rect) (+ (:x rect) (:w rect)))
            y (range (:y rect) (+ (:y rect) (:h rect)))
            :let [tile (m/Point. x y)
                  texture (get background tile)]
            :when (and (some? texture)
                       (tile-visible? camera tile))]
      (draw-sprite-texture ctx texture (u/tile->coord tile)))))

(defn draw-player [ctx {:keys [position texture]}]
  (draw-sprite-texture ctx texture position))

(defn draw-npcs [ctx npcs camera]
  (doseq [[tile {texture :texture}] npcs]
    (when (tile-visible? camera tile)
      (draw-sprite-texture ctx texture (u/tile->coord tile)))))

(defn draw-direction-indicator [ctx {{:keys [position direction]} :player}]
  (let [rotation (direction {:up 0 :right 90 :down 180 :left 270})]
    (draw-texture-rotated ctx :arrow position rotation)))

(defn draw-dialogue-box [ctx {:keys [text options selected-option]}]
  (let [w 600
        h 300
        x (/ (- (c/width ctx) w) 2)
        y (/ (- (c/height ctx) h) 2)
        dialogue-box-rect (m/Rect. x y w h)]
    (c/save! ctx)
    (c/set-fill-style! ctx "rgba(230, 230, 230, .9)")
    (c/fill-rect! ctx dialogue-box-rect)
    (c/set-stroke-style! ctx "rgb(0, 0, 0)")
    (c/stroke-rect! ctx dialogue-box-rect)

    (c/set-fill-style! ctx "rgb(0, 0, 0)")
    (c/set-baseline! ctx "top")
    (c/set-font! ctx "23px serif")
    (let [offset (c/draw-textbox! ctx text (m/translate-point (m/Point. x y) 20 20) (- w 40))]
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
            (let [coord (m/translate-point (m/Point. x y) 20 0)
                  height (+ padding (c/draw-textbox! ctx option (m/translate-point coord 7 padding) w))
                  option-rect (m/Rect. (:x coord) (:y coord) w height)]
              (if (= selected-option idx)
                (c/set-fill-style! ctx "rgba(0, 0, 0, .1)")
                (c/set-fill-style! ctx "rgba(0, 0, 0, 0)"))
              (c/fill-rect! ctx option-rect)

              (c/set-line-width! ctx "1")
              (c/stroke-rect! ctx option-rect)

              (recur (inc idx)
                     (+ y height spacing)
                     options))))))
    (c/restore! ctx)))

(defn draw-camera [ctx {:keys [x y w h] :as camera}]
  (c/set-stroke-style! ctx "rgb(255, 0, 0)")
  (c/stroke-rect! ctx camera)
  (c/set-stroke-style! ctx "rgb(255, 255, 0)")
  (c/stroke-rect! ctx
                  (m/Rect. (- x tile-size) (- y tile-size)
                           (+ w (* 2 tile-size)) (+ h (* 2 tile-size)))))


(defn render [view-state]
  (when (some? @ctx)
    (let [camera (:camera view-state)]
      (c/clear! @ctx)
      (c/set-fill-style! @ctx "rgb(0, 0, 0)")
      (c/fill-rect! @ctx (m/Rect. 0 0 (c/width @ctx) (c/height @ctx)))
      (let [w-offset (- (/ (- (c/width @ctx) (:w camera)) 2) (:x camera))
            h-offset (- (/ (- (c/height @ctx) (:h camera)) 2) (:y camera))]
        (c/set-transform! @ctx 1 0 0 1 w-offset h-offset))
      (let [l (get-in view-state [:player :location-id])
            player-tile (u/coord->tile (get-in view-state [:player :position]))
            {:keys [npcs
                    dimension
                    background1
                    background2
                    foreground1
                    foreground2]} (get-in @data [:locations l])]
        (draw-background @ctx dimension background1 camera)
        (draw-background @ctx dimension background2 camera)
        (draw-player @ctx (:player view-state))
        (draw-npcs @ctx npcs camera)
        (draw-background @ctx dimension foreground1 camera)
        (draw-background @ctx dimension foreground2 camera))
      ; (draw-direction-indicator @ctx view-state)
      ; (draw-camera @ctx camera)
      (c/reset-transform! @ctx)
      (let [w-offset (/ (- (c/width @ctx) (:w camera)) 2)
            h-offset (/ (- (c/height @ctx) (:h camera)) 2)]
        (c/draw-image! @ctx
                       (.-canvas @ctx)
                       (m/Point. w-offset h-offset)
                       [(:w camera) (:h camera)]
                       (m/Point.
                         (- (/ (c/width @ctx) 2) (* (/ (:w camera) 2) camera-scale))
                         (- (/ (c/height @ctx) 2) (* (/ (:h camera) 2) camera-scale)))
                       [(* camera-scale (:w camera)) (* camera-scale (:h camera))]))
      (when (interacting? view-state)
        (draw-dialogue-box @ctx (dialogue-data view-state))))))

;; Dialogue

(defn advance-meta-nodes [resolver line trigger-changes]
  (case (:kind line)
    :trigger (resolver
               (:next-line-id line)
               (merge trigger-changes (:trigger-changes line)))
    :case (resolver
            ((:next-line-id-fn line) (:switches @state))
            trigger-changes)
    line))

(defn resolve-next-line-id [line-id trigger-changes]
  (let [line (get-in @data [:lines line-id])]
    (case (:kind line)
      :npc {:next-line-id line-id
            :trigger-changes trigger-changes}
      (:trigger :case) (advance-meta-nodes
                         resolve-next-line-id line trigger-changes)
      {:next-line-id nil
       :trigger-changes trigger-changes})))

(defn available-option? [{:keys [condition]}]
  (or (not (fn? condition))
      (condition (:switches @state))))

(defn resolve-options [line-id trigger-changes]
  (let [line (get-in @data [:lines line-id])
        options (case (:kind line)
                  :npc (vector {:text "Continue..."
                                :next-line-id line-id
                                :trigger-changes trigger-changes})
                  :player (->> (:options line)
                               (filterv available-option?)
                               (mapv (fn [option]
                                       (merge option (resolve-next-line-id
                                                       (:next-line-id option)
                                                       trigger-changes)))))
                  (:trigger :case) (advance-meta-nodes
                                     resolve-options line trigger-changes)
                  nil)]
    (if-not (empty? options)
      options
      (vector {:text "Yeah..., whatever. Farewell."
               :trigger-changes trigger-changes}))))

(defn resolve-interaction
  ([line-id] (resolve-interaction line-id {}))
  ([line-id trigger-changes]
   (let [line (get-in @data [:lines line-id])]
     (case (:kind line)
       :npc {:text (:text line)
             :options (resolve-options (:next-line-id line) trigger-changes)
             :selected-option 0}
       (:trigger :case) (advance-meta-nodes
                          resolve-interaction line trigger-changes)))))

(defn interaction-line-id []
  (let [l (get-in @state [:player :location-id])
        npcs (get-in @data [:locations l :npcs])
        tile (interaction-tile @state)]
    (if-let [dialogue-id (get-in npcs [tile :dialogue-id])]
      (get-in @state [:dialogue-states dialogue-id]))))

;; Input Handlers

(def move-q (atom #queue []))

(defn handle-move [direction]
  (if (interacting? @state)
    (if-let [next-index-fn (get {:up dec :down inc} direction)]
      (swap! state update :interaction
             (fn [{:keys [options selected-option] :as interaction}]
               (assoc interaction :selected-option
                      (mod (next-index-fn selected-option)
                           (count options))))))
    (swap! move-q conj direction)))

(defn handle-interact []
  (if (interacting? @state)
    (let [{:keys [options selected-option]} (:interaction @state)
          {:keys [trigger-changes next-line-id]} (get options selected-option)]
      (swap! state update :switches merge trigger-changes)
      (if (some? next-line-id)
        (swap! state assoc :interaction (resolve-interaction next-line-id))
        (swap! state dissoc :interaction)))
    (if-let [line-id (interaction-line-id)]
      (swap! state assoc :interaction (resolve-interaction line-id)))))

(defn start-input-loop [channel]
  (go-loop [[command payload :as message] (<! channel)]
           (when message
             (let [handler (case command
                             :move handle-move
                             :interact handle-interact)]
               (handler payload)
               (recur (<! channel))))))

;; Animations

(def player-animations
  {:idle
   {:up
    [[:hare_up_idle1 200]
     [:hare_up_idle2 200]]
    :down
    [[:hare_down_idle1 200]
     [:hare_down_idle2 200]]
    :left
    [[:hare_left_walking2 200]]
    :right
    [[:hare_right_walking2 200]]}

   :walking
   {:up
    [[:hare_up_walking1 (quot tile-move-time 2)]
     [:hare_up_walking2 (quot tile-move-time 2)]]
    :down
    [[:hare_down_walking1 (quot tile-move-time 2)]
     [:hare_down_walking2 (quot tile-move-time 2)]]
    :left
    [[:hare_left_walking1 (quot tile-move-time 2)]
     [:hare_left_walking2 (quot tile-move-time 2)]]
    :right
    [[:hare_right_walking1 (quot tile-move-time 2)]
     [:hare_right_walking2 (quot tile-move-time 2)]]}})

(defn anim->data [now frames]
  {:start (m/round now)
   :frames frames
   :total-duration (apply + (select [ALL (nthpath 1)] frames))})

(defn animation-texture [now {:keys [start frames total-duration]}]
  (let [animation-frame (mod (m/round (- now start)) total-duration)]
    (reduce (fn [duration-sum [texture duration]]
              (if (<= duration-sum animation-frame (+ duration-sum duration -1))
                (reduced texture)
                (+ duration-sum duration)))
            0
            frames)))

(defn animated-position [start from to now]
  (let [pct-done (/ (- now start) tile-move-time)
        transform (fn [f t] (+ f (m/round (* pct-done (- t f)))))]
    (if (< pct-done 1)
      (m/Point. (transform (:x from) (:x to))
                (transform (:y from) (:y to)))
      to)))

(defn animation-done? [start now]
  (< (+ start tile-move-time) now))

;; state updates and view state

(defn camera-coord [cam-size loc-lower loc-size p-coord]
  (if (<= loc-size cam-size)
    (- loc-lower (/ (- cam-size loc-size) 2))
    (let [cam-half (/ cam-size 2)
          loc-upper (+ loc-lower loc-size)
          center-offset (/ tile-size 2)]
      (cond
        ; close to *lower* (left or top) edge
        (< p-coord (+ loc-lower cam-half (- center-offset)))
        loc-lower

        ; close to *upper* (right or bottom) edge
        (> p-coord (- loc-upper cam-half center-offset))
        (- loc-upper cam-size)

        :else
        (+ (- p-coord cam-half) center-offset)))))

(defn camera-rect [player-coord location-id]
  (let [w (* camera-tile-width tile-size)
        h (* camera-tile-height tile-size)
        loc-dim (get-in @data [:locations location-id :dimension])
        dim (m/rect-scale loc-dim tile-size)
        left (camera-coord w (:x dim) (:w dim) (:x player-coord))
        top (camera-coord h (:y dim) (:h dim) (:y player-coord))]
    (m/Rect. left top w h)))

(defn update-state-movement [state now]
  (if-let [{:keys [start destination]} (:animation state)]
    (if (animation-done? start now)
      (let [new-state (dissoc state :animation)
            location-id (get-in state [:player :location-id])]
        (if-let [[new-location-id new-position]
                 (get-in @data [:locations location-id :outbound-connections destination])]
          (do
            (reset! move-q #queue [])
            (update new-state :player merge {:location-id new-location-id
                                             :position new-position}))
          (assoc-in new-state [:player :position] destination)))
      state)
    (if-let [direction (peek @move-q)]
      (let [new-position (apply
                           m/translate-point
                           (get-in state [:player :position])
                           (direction-map direction))]
        (swap! move-q pop)
        (cond-> (assoc-in state [:player :direction] direction)
          (walkable? new-position)
          (assoc :animation {:start now
                             :destination new-position})))
      state)))

(defn update-state [state now]
  (-> state
      (update-state-movement now)))

(defn view-state [state now]
  (as-> state s
    (update s :player
            (fn [{:keys [position direction] :as player}]
              (merge player
                     (if-let [{:keys [start destination]} (:animation state)]
                       {:position (animated-position start
                                                     (u/tile->coord position)
                                                     (u/tile->coord destination)
                                                     now)
                        :texture (animation-texture now (anim->data start (get-in player-animations [:walking direction])))}
                       {:position (u/tile->coord position)
                        :texture (animation-texture now (anim->data 0 (get-in player-animations [:idle direction])))}))))
    (let [{:keys [position location-id]} (:player s)]
      (assoc s :camera (camera-rect position location-id)))))

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
            (when-not @quit
              (let [now (* time-factor (.now js/performance))
                    new-state (swap! state #(update-state % now))
                    view-state (view-state new-state now)]
                (when-not (s/valid? ::state new-state)
                  (s/explain ::state new-state))
                (when-not (= @prev-view-state view-state)
                  (reset! prev-view-state view-state)
                  (render view-state))
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
