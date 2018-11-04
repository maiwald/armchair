(ns armchair.game.core
  (:require [clojure.core.async :refer [chan sliding-buffer put! go-loop <!]]
            [clojure.spec.alpha :as s]
            [clojure.set :refer [subset? union]]
            [armchair.game.canvas :as c]
            [armchair.game.pathfinding :as path]
            [armchair.config :refer [tile-size]]
            [armchair.textures :refer [texture-set load-textures]]
            [armchair.util :refer [map-values
                                   rect-width
                                   rect-height
                                   translate-point]]))

;; Definitions

(def time-factor 1)
(def tile-move-time 150) ; milliseconds
(def direction-map {:up [0 -1]
                    :down [0 1]
                    :left [-1 0]
                    :right [1 0]})


(s/def ::state (s/and (s/keys :req-un [::player]
                              :opt-un [::highlight ::interaction])))

(s/def ::player (s/keys :req-un [:player/position
                                 :player/direction
                                 :player/infos]))
(s/def :player/position :type/point)
(s/def :player/direction #{:up :down :left :right})
(s/def :player/infos (s/coll-of :entity/id :kind set?))

(s/def ::highlight :type/point)

(s/def ::interaction (s/keys :req-un [::line-id ::selected-option]))
(s/def ::line-id :entity/id)
(s/def ::selected-option int?)

;; Conversion Helpers

(defn tile->coord [[tx ty]]
  [(* tile-size tx) (* tile-size ty)])

(defn coord->tile [[cx cy]]
  [(quot cx tile-size) (quot cy tile-size)])

(defn normalize-to-tile [coord]
  (-> coord coord->tile tile->coord))

;; State

(def state (atom nil))
(def data (atom nil))

(def initial-game-state
  {:player {:location-id #uuid "121fb127-fbc8-44b9-ba62-2ca2517b6995"
            :position (tile->coord [8 12])
            :direction :right
            :infos #{}}})

(defn ^boolean walkable? [tile]
  (let [{l :location-id} (:player @state)
        {:keys [walk-set npcs]} (get-in @data [:locations l])]
    (and (contains? walk-set tile)
         (not (contains? npcs tile)))))

(defn interaction-tile [{{:keys [position direction]} :player}]
  (translate-point
    (coord->tile position)
    (direction-map direction)))

(defn dialogue-data [{{:keys [line-id selected-option]} :interaction
                      {player-infos :infos} :player}]
  (let [line (get-in @data [:lines line-id])]
    {:text (:text line)
     :options (->> (:options line)
                   (filterv #(subset? (:required-info-ids %) player-infos))
                   (map :text))
     :selected-option selected-option}))

(defn ^boolean interacting? [state]
  (contains? state :interaction))

(defn interaction-option-count [state]
  (count (:options (dialogue-data state))))

(defn next-interaction [{{:keys [line-id selected-option]} :interaction}]
  (get-in @data [:lines line-id :options selected-option :next-line-id]))

;; Rendering

(def ctx (atom nil))
(def texture-atlas (atom nil))

(defn draw-texture [ctx texture coord]
  (when @texture-atlas
    (c/draw-image! ctx (get @texture-atlas texture (@texture-atlas :missing_texture)) coord)))

(defn draw-texture-rotated [ctx texture coord deg]
  (when @texture-atlas
    (c/draw-image-rotated! ctx (@texture-atlas texture) coord deg)))

(defn draw-background [ctx dimension background]
  (doseq [x (range (rect-width dimension))
          y (range (rect-height dimension))
          :let [texture (get background [x y])]]
    (draw-texture ctx texture (tile->coord [x y]))))

(defn draw-player [ctx player]
  (draw-texture ctx :player player))

(defn draw-npcs [ctx npcs]
  (doseq [[tile {texture :npc-texture}] npcs]
    (draw-texture ctx texture (tile->coord tile))))

(defn draw-highlight [ctx highlight-coord]
  (c/save! ctx)
  (c/set-stroke-style! ctx "rgb(255, 255, 0)")
  (c/set-line-width! ctx "2")
  (c/stroke-rect! ctx highlight-coord tile-size tile-size)
  (c/restore! ctx))

(defn draw-path [ctx {:keys [highlight] {:keys [position]} :player}]
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
    (c/set-fill-style! ctx "rgba(230, 230, 230, .9)")
    (c/fill-rect! ctx [x y] w h)
    (c/set-stroke-style! ctx "rgb(0, 0, 0)")
    (c/stroke-rect! ctx [x y] w h)

    (c/set-fill-style! ctx "rgb(0, 0, 0)")
    (c/set-font! ctx "40px serif")
    (c/set-baseline! ctx "top")
    (c/draw-text! ctx "Dialogue!" (translate-point [x y] [20 20]))
    (c/set-font! ctx "18px serif")
    (c/draw-textbox! ctx text (translate-point [x y] [20 70]) (- w 40) 230)

    (c/set-baseline! ctx "middle")
    (doseq [[idx option] (map-indexed vector options)]
      (let [w (- w 40)
            h 24
            offset 6
            coord (translate-point [x y] [20 (+ 220 (* idx (+ offset h)))])]
        (c/set-fill-style! ctx "rgba(0, 0, 0, .2)")
        (c/fill-rect! ctx coord w h)

        (if (= selected-option idx)
          (c/set-stroke-style! ctx "rgb(255, 0, 0)")
          (c/set-stroke-style! ctx "rgb(0, 0, 0)"))
        (c/set-line-width! ctx "1")
        (c/stroke-rect! ctx coord w h)

        (c/set-fill-style! ctx "rgb(0, 0, 0)")
        (c/draw-text! ctx option (translate-point coord [2 (/ h 2)]))))
    (c/restore! ctx)))

(defn render [view-state]
  (when @ctx
    (let [l (get-in @state [:player :location-id])
          location (get-in @data [:locations l])]
      (c/clear! @ctx)
      (when-not (interacting? @state)
        (draw-path @ctx @state))
      (draw-player @ctx (get-in view-state [:player :position]))
      (draw-npcs @ctx (:npcs location))
      (when (and (not (interacting? @state))
                 (contains? @state :highlight))
        (draw-highlight @ctx (:highlight @state)))
      (draw-direction-indicator @ctx view-state)
      (when (interacting? @state)
        (draw-dialogue-box @ctx (dialogue-data @state))))))

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

(defn handle-cursor [coord]
  (if coord
    (swap! state assoc :highlight (normalize-to-tile coord))
    (swap! state dissoc :highlight)))

(defn handle-interact []
  (if (interacting? @state)
    (let [next-interaction (next-interaction @state)]
      (if (nil? next-interaction)
        (swap! state dissoc :interaction)
        (swap! state #(let [line-id (get-in % [:interaction :line-id])
                            info-ids (get-in @data [:lines line-id :info-ids])]
                        (cond-> (merge % {:interaction {:line-id next-interaction
                                                        :selected-option 0}})
                          (not (empty? info-ids)) (update-in [:player :infos] union info-ids))))))
    (let [l (get-in @state [:player :location-id])
          location (get-in @data [:locations l])]
      (if-let [{line-id :initial-line-id} ((:npcs location) (interaction-tile @state))]
        (swap! state assoc :interaction {:line-id line-id
                                         :selected-option 0})))))

(defn start-input-loop [channel]
  (go-loop [[cmd payload] (<! channel)]
           (let [handler (case cmd
                           :cursor-position handle-cursor
                           :move handle-move
                           :interact handle-interact)]
             (handler payload)
             (recur (<! channel)))))

;; Animations

(defn round [x] (.round js/Math x))

(defn animated-position [start duration [fx fy] [tx ty] now]
  (let [passed (- now start)
        pct (/ passed duration)
        transform (fn [f t] (+ f (round (* pct (- t f)))))]
    (if (< pct 1)
      [(transform fx tx) (transform fy ty)]
      [tx ty])))

(defn animation-done? [start duration now]
  (< (+ start duration) now))

(defn animate-move [destination-tile done-fn]
  (let [destination (tile->coord destination-tile)
        start (* time-factor (.now js/performance))
        from (get-in @state [:player :position])
        duration tile-move-time]
    (js/requestAnimationFrame
      (fn animate []
        (let [now (* time-factor (.now js/performance))]
          (if-not (animation-done? start duration now)
            (do
              (render (update-in @state [:player :position] #(animated-position start duration from destination now)))
              (js/requestAnimationFrame animate))
            (do
              (swap! state assoc-in [:player :position] destination)
              (swap! move-q pop)
              (done-fn))))))))

(defn start-move-loop [channel]
  (go-loop [_ (<! channel)]
           (when-let [direction (first @move-q)]
             (swap! state assoc-in [:player :direction] direction)
             (let [new-position (translate-point (coord->tile (get-in @state [:player :position]))
                                                 (direction-map direction))]
               (if (walkable? new-position)
                 (animate-move
                   new-position
                   (fn []
                     (let [{l :location-id p :position} (:player @state)]
                       (if-let [new-location (get-in @data [:locations l :outbound-connections (coord->tile p)])]
                         (let [new-position (tile->coord (get-in @data [:locations new-location :inbound-connections l]))]
                           (reset! move-q #queue [])
                           (swap! state update :player merge {:location-id new-location
                                                              :position new-position}))
                         (put! channel true)))))
                 (when-not (empty? (swap! move-q pop)) (put! channel true)))))
           (recur (<! channel))))

;; Game Loop

(defn start-game [background-context entity-context game-data]
  (reset! state initial-game-state)
  (reset! data game-data)
  (reset! move-q #queue [])
  (reset! ctx entity-context)
  (let [input-chan (chan)
        move-chan (chan (sliding-buffer 1))]
    (add-watch state
               :state-update
               (fn [_ _ old-state new-state]
                 (when (not= old-state new-state)
                   (when-not (s/valid? ::state new-state)
                     (js/console.log (s/explain ::state new-state)))
                   (when (not= (get-in old-state [:player :location-id])
                               (get-in new-state [:player :location-id]))
                     (c/clear! background-context)
                     (let [l (get-in @state [:player :location-id])
                           {:keys [dimension background]} (get-in @data [:locations l])]
                       (draw-background background-context dimension background)))

                   (js/requestAnimationFrame #(render new-state)))))
    (add-watch move-q
               :animation-update
               (fn [_ _ old-state new-state]
                 (when (and (empty? old-state)
                            (some? new-state))
                   (put! move-chan true))))
    (load-textures (fn [loaded-atlas]
                     (reset! texture-atlas loaded-atlas)

                     (c/clear! background-context)
                     (let [l (get-in @state [:player :location-id])
                           {:keys [dimension background]} (get-in @data [:locations l])]
                       (draw-background background-context dimension background))

                     (start-input-loop input-chan)
                     (start-move-loop move-chan)

                     (js/requestAnimationFrame #(render @state))))
    input-chan))

(defn end-game []
  (remove-watch state :state-update)
  (remove-watch move-q :animation-update)
  (reset! state nil)
  (reset! data nil)
  (reset! move-q nil)
  (reset! ctx nil))
