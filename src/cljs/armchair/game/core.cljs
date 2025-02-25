(ns armchair.game.core
  (:require [clojure.core.async :refer [<!
                                        chan
                                        close!
                                        go-loop]]
            [clojure.spec.alpha :as s]
            [clojure.set :refer [intersection difference]]
            [armchair.game.canvas :as c]
            [armchair.game.pathfinding :as path]
            [armchair.config :refer [tile-size
                                     camera-tile-width
                                     camera-tile-height
                                     camera-scale]]
            [armchair.sprites :refer [sprite-sheets image-files load-images]]
            [armchair.math :as m]
            [armchair.util :as u]
            [com.rpl.specter
             :refer [nthpath ALL MAP-VALS FIRST]
             :refer-macros [select transform]]))

;; Definitions

(def time-factor 1)
(defn get-time []
  (* time-factor (.now js/performance)))

(def tile-move-time 200) ; milliseconds

(def direction->delta {:up [0 -1]
                       :down [0 1]
                       :left [-1 0]
                       :right [1 0]})

(def delta->direction (u/reverse-map direction->delta))

(s/def ::state (s/and (s/keys :req-un [::player ::switches]
                              :opt-un [::interaction])))

(s/def ::player (s/keys :req-un [:player/position
                                 :player/direction]
                        :opt-un [:player/sprite
                                 :player/animation]))
(s/def :player/position :type/point)
(s/def :player/direction #{:up :down :left :right})
(s/def :player/animation (s/keys :req-un [::start ::origin ::destination]))

(s/def ::switches (s/map-of :entity/id (s/nilable :entity/id)))

(s/def ::interaction (s/keys :req-un [:interaction/text
                                      :interaction/options
                                      :interaction/selected-option]))

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
(def input-map (atom {:pressed-keys #{}}))
(def data (atom nil))

(defn walkable? [state tile]
  (let [{l :location-id} (:player state)
        {:keys [bounds blocked characters]} (get-in @data [:locations l])]
    (and
      (m/rect-contains? bounds tile)
      (not (or (contains? blocked tile)
               (contains? characters tile))))))

(defn interactable? [state tile]
  (let [{l :location-id} (:player state)
        characters (get-in @data [:locations l :characters])]
    (contains? characters tile)))

(defn interaction-tile [{{:keys [position direction]} :player}]
  (apply
    m/translate-point
    position
    (direction->delta direction)))

(defn dialogue-data [{:keys [interaction]}]
  (-> interaction
      (select-keys [:text :options :selected-option])
      (update :options #(mapv :text %))))

(defn interacting? [state]
  (contains? state :interaction))

;; Rendering

(def ctx (atom nil))
(def loaded-images (atom nil))

(defn tile-visible? [camera {:keys [x y]}]
  (m/rect-intersects? camera
                      (m/Rect. (* x tile-size) (* y tile-size)
                               tile-size tile-size)))

(defn draw-sprite [sprite dest-coord]
  (let [[file-name {:keys [x y]}] sprite]
    (when (some? @loaded-images)
      (if-let [sprite-sheet (get @loaded-images file-name)]
        (let [{sheet-tile-size :tile-size :keys [gutter offset]} (sprite-sheets file-name)]
          (c/draw-image! @ctx sprite-sheet
                         (m/Point. (+ offset (* (+ gutter sheet-tile-size) x))
                                   (+ offset (* (+ gutter sheet-tile-size) y)))
                         [sheet-tile-size sheet-tile-size]
                         dest-coord
                         [tile-size tile-size]))
        (c/draw-image! @ctx (@loaded-images "missing_texture.png") dest-coord)))))

(defn draw-sprite-rotated [file-name coord deg]
  (when (some? @loaded-images)
    (c/draw-image-rotated! @ctx (@loaded-images file-name) coord deg)))

(defn draw-tile-layer [tile-layer camera]
  (when (some? tile-layer)
    (doseq [[_ tile-data] (group-by (fn [[_ [file-name]]] file-name) tile-layer)
            [tile sprite] tile-data
            :when (tile-visible? camera tile)]
      (draw-sprite sprite (u/tile->coord tile)))))

(defn draw-player [{:keys [coord sprite]}]
  (draw-sprite sprite coord))

(defn draw-characters [characters camera]
  (doseq [[tile {:keys [sprite]}] characters]
    (when (tile-visible? camera tile)
      (draw-sprite sprite (u/tile->coord tile)))))

(defn draw-direction-indicator [{{:keys [coord direction]} :player}]
  (let [rotation (direction {:up 0 :right 90 :down 180 :left 270})]
    (draw-sprite-rotated "arrow.png" coord rotation)))

(defn button [rect
              {:keys [mouse-clicked?
                      mouse-position]}
              {:keys [on-click on-enter]}]
  (when (m/rect-contains? rect mouse-position)
    (if mouse-clicked?
      (on-click)
      (on-enter))))

(defn draw-dialogue-box [{:keys [text options selected-option]} input]
  (let [w 600
        h 300
        x (/ (- (c/width @ctx) w) 2)
        y (/ (- (c/height @ctx) h) 2)
        dialogue-box-rect (m/Rect. x y w h)]
    (c/save! @ctx)
    (c/set-fill-style! @ctx "rgba(230, 230, 230, .9)")
    (c/fill-rect! @ctx dialogue-box-rect)
    (c/set-stroke-style! @ctx "rgb(0, 0, 0)")
    (c/stroke-rect! @ctx dialogue-box-rect)

    (c/set-fill-style! @ctx "rgb(0, 0, 0)")
    (c/set-baseline! @ctx "top")
    (c/set-font! @ctx "23px serif")
    (let [offset (c/draw-textbox! @ctx text (m/translate-point (m/Point. x y) 20 20) (- w 40))]
      (c/set-font! @ctx "18px serif")
      (let [w (- w 40)
            padding 6
            spacing 6]
        (loop [idx 0
               y (+ y 40 offset)
               [option & options] options]
          (when (some? option)
            (c/set-fill-style! @ctx "rgb(0, 0, 0)")
            (if (= selected-option idx)
              (c/set-stroke-style! @ctx "rgb(255, 0, 0)")
              (c/set-stroke-style! @ctx "rgb(0, 0, 0)"))
            (let [coord (m/translate-point (m/Point. x y) 20 0)
                  height (+ padding (c/draw-textbox! @ctx option (m/translate-point coord 7 padding) w))
                  option-rect (m/Rect. (:x coord) (:y coord) w height)]
              (if (= selected-option idx)
                (c/set-fill-style! @ctx "rgba(0, 0, 0, .1)")
                (c/set-fill-style! @ctx "rgba(0, 0, 0, 0)"))
              (c/fill-rect! @ctx option-rect)

              (c/set-line-width! @ctx "1")
              (c/stroke-rect! @ctx option-rect)

              (button option-rect input
                      {:on-click #(swap! state (fn [s] (-> s
                                                           (update :action-q conj :interact)
                                                           (assoc-in [:interaction :selected-option] idx))))
                       :on-enter #(swap! state assoc-in [:interaction :selected-option] idx)})

              (recur (inc idx)
                     (+ y height spacing)
                     options))))))
    (c/restore! @ctx)))

(defn draw-camera [{:keys [x y w h] :as camera}]
  (c/set-stroke-style! @ctx "rgb(255, 0, 0)")
  (c/stroke-rect! @ctx camera)
  (c/set-stroke-style! @ctx "rgb(255, 255, 0)")
  (c/stroke-rect! @ctx
                  (m/Rect. (- x tile-size) (- y tile-size)
                           (+ w (* 2 tile-size)) (+ h (* 2 tile-size)))))

(defn draw-highlight [{:keys [position completion]}]
  (when (and (some? position) (<= completion 1))
    (c/set-fill-style! @ctx (str "rgba(255, 255, 0, " (* 0.5 (- 1 completion)) ")"))
    (let [{:keys [x y]} (u/tile->coord position)
          offset (* 8 completion)]
      (c/fill-rect! @ctx (m/Rect. (- x offset) (- y offset)
                                  (+ (* 2 offset) tile-size)
                                  (+ (* 2 offset) tile-size))))))

(defn scale-to-fill [{cam-w :w cam-h :h}]
  (let [ctx-w (c/width @ctx)
        ctx-h (c/height @ctx)
        w-offset (/ (- ctx-w cam-w) 2)
        h-offset (/ (- ctx-h cam-h) 2)]
    (c/draw-image! @ctx
                   (.-canvas @ctx)
                   (m/Point. w-offset h-offset)
                   [cam-w cam-h]
                   (m/Point. 0 0)
                   [ctx-w ctx-h])))

(defn render [state]
  (let [camera (:camera state)]
    (c/clear! @ctx)
    (c/set-fill-style! @ctx "rgb(0, 0, 0)")
    (c/fill-rect! @ctx (m/Rect. 0 0 (c/width @ctx) (c/height @ctx)))
    (let [w-offset (- (/ (- (c/width @ctx) (:w camera)) 2) (:x camera))
          h-offset (- (/ (- (c/height @ctx) (:h camera)) 2) (:y camera))]
      (c/set-transform! @ctx 1 0 0 1 w-offset h-offset))
    (let [l (get-in state [:player :location-id])
          {:keys [characters
                  background1
                  background2
                  foreground1
                  foreground2]} (get-in @data [:locations l])]
      (draw-tile-layer background1 camera)
      (draw-tile-layer background2 camera)
      (draw-highlight (:highlight state))
      (draw-player (:player state))
      (draw-characters characters camera)
      (draw-tile-layer foreground1 camera)
      (draw-tile-layer foreground2 camera))
      ; (draw-direction-indicator @ctx state)
      ; (draw-camera camera))
    (c/reset-transform! @ctx)
    (scale-to-fill camera)
    (when (interacting? state)
      (draw-dialogue-box (dialogue-data state)
                         (:input state)))))

;; Dialogue

(defn advance-meta-nodes [state resolver line trigger-changes]
  (case (:kind line)
    :trigger (resolver state
               (:next-line-id line)
               (merge trigger-changes (:trigger-changes line)))
    :case (resolver state
            ((:next-line-id-fn line) (:switches state))
            trigger-changes)
    line))

(defn resolve-next-line-id [state line-id trigger-changes]
  (let [line (get-in @data [:lines line-id])]
    (case (:kind line)
      :npc {:next-line-id line-id
            :trigger-changes trigger-changes}
      (:trigger :case) (advance-meta-nodes state
                         resolve-next-line-id line trigger-changes)
      {:next-line-id nil
       :trigger-changes trigger-changes})))

(defn available-option? [state {:keys [condition]}]
  (or (not (fn? condition))
      (condition (:switches state))))

(defn resolve-options [state line-id trigger-changes]
  (let [line (get-in @data [:lines line-id])
        options (case (:kind line)
                  :npc (vector {:text "Continue..."
                                :next-line-id line-id
                                :trigger-changes trigger-changes})
                  :player (->> (:options line)
                               (filterv #(available-option? state %))
                               (mapv (fn [option]
                                       (merge option (resolve-next-line-id
                                                       state
                                                       (:next-line-id option)
                                                       trigger-changes)))))
                  (:trigger :case) (advance-meta-nodes state
                                     resolve-options line trigger-changes)
                  nil)]
    (if-not (empty? options)
      options
      (vector {:text "Ok, farewell."
               :trigger-changes trigger-changes}))))

(defn resolve-interaction
  ([state line-id] (resolve-interaction state line-id {}))
  ([state line-id trigger-changes]
   (let [line (get-in @data [:lines line-id])]
     (case (:kind line)
       :npc {:text (:text line)
             :options (resolve-options state (:next-line-id line) trigger-changes)
             :selected-option 0}
       (:trigger :case) (advance-meta-nodes state
                          resolve-interaction line trigger-changes)))))

(defn interaction-line-id [state]
  (let [l (get-in state [:player :location-id])
        tile (interaction-tile state)]
    (when-let [dialogue-id (get-in @data [:locations l :characters tile :dialogue-id])]
      (get-in @data [:initial-dialogue-lines dialogue-id]))))

;; Animations

(defn add-animation-sprites [animation-map]
  (transform [MAP-VALS MAP-VALS ALL FIRST]
             {:hare_down_idle1 ["hare.png" (m/Point. 6 0)]
              :hare_down_idle2 ["hare.png" (m/Point. 7 0)]
              :hare_down_walking1 ["hare.png" (m/Point. 2 0)]
              :hare_down_walking2 ["hare.png" (m/Point. 3 0)]
              :hare_left_idle ["hare.png" (m/Point. 13 0)]
              :hare_left_walking1 ["hare.png" (m/Point. 8 0)]
              :hare_left_walking2 ["hare.png" (m/Point. 9 0)]
              :hare_right_idle ["hare.png" (m/Point. 12 0)]
              :hare_right_walking1 ["hare.png" (m/Point. 10 0)]
              :hare_right_walking2 ["hare.png" (m/Point. 11 0)]
              :hare_up_idle1 ["hare.png" (m/Point. 4 0)]
              :hare_up_idle2 ["hare.png" (m/Point. 5 0)]
              :hare_up_walking1 ["hare.png" (m/Point. 0 0)]
              :hare_up_walking2 ["hare.png" (m/Point. 1 0)]
              :guy_down_idle ["guy_sprite_sheet.png" (m/Point. 1 0)]
              :guy_down_walking1 ["guy_sprite_sheet.png" (m/Point. 0 0)]
              :guy_down_walking2 ["guy_sprite_sheet.png" (m/Point. 2 0)]
              :guy_left_idle ["guy_sprite_sheet.png" (m/Point. 1 1)]
              :guy_left_walking1 ["guy_sprite_sheet.png" (m/Point. 0 1)]
              :guy_left_walking2 ["guy_sprite_sheet.png" (m/Point. 2 1)]
              :guy_right_idle ["guy_sprite_sheet.png" (m/Point. 1 2)]
              :guy_right_walking1 ["guy_sprite_sheet.png" (m/Point. 0 2)]
              :guy_right_walking2 ["guy_sprite_sheet.png" (m/Point. 2 2)]
              :guy_up_idle ["guy_sprite_sheet.png" (m/Point. 1 3)]
              :guy_up_walking1 ["guy_sprite_sheet.png" (m/Point. 0 3)]
              :guy_up_walking2 ["guy_sprite_sheet.png" (m/Point. 2 3)]}
             animation-map))

(def hare-animations
  (add-animation-sprites
    {:idle {:up
            [[:hare_up_idle1 200]
             [:hare_up_idle2 200]]
            :down
            [[:hare_down_idle1 200]
             [:hare_down_idle2 200]]
            :left
            [[:hare_left_walking2 200]
             [:hare_left_idle 200]]
            :right
            [[:hare_right_walking2 200]
             [:hare_right_idle 200]]}
     :walking {:up
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
                [:hare_right_walking2 (quot tile-move-time 2)]]}}))

(def guy-animations
  (add-animation-sprites
    {:idle {:up [[:guy_up_idle 200]]
            :down [[:guy_down_idle 200]]
            :left [[:guy_left_idle 200]]
            :right [[:guy_right_idle 200]]}
     :walking {:up
               [[:guy_up_walking1 (quot tile-move-time 2)]
                [:guy_up_walking2 (quot tile-move-time 2)]]
               :down
               [[:guy_down_walking1 (quot tile-move-time 2)]
                [:guy_down_walking2 (quot tile-move-time 2)]]
               :left
               [[:guy_left_walking1 (quot tile-move-time 2)]
                [:guy_left_walking2 (quot tile-move-time 2)]]
               :right
               [[:guy_right_walking1 (quot tile-move-time 2)]
                [:guy_right_walking2 (quot tile-move-time 2)]]}}))

(defn anim->data [now frames]
  {:start (m/round now)
   :frames frames
   :total-duration (apply + (select [ALL (nthpath 1)] frames))})

(defn animation-sprite [now {:keys [start frames total-duration]}]
  (let [animation-frame (mod (m/round (- now start)) total-duration)]
    (reduce (fn [duration-sum [sprite duration]]
              (if (<= duration-sum animation-frame (+ duration-sum duration -1))
                (reduced sprite)
                (+ duration-sum duration)))
            0
            frames)))

(defn animated-position [start from to now]
  (let [pct-done (/ (- now start) tile-move-time)
        xform (fn [f t] (+ f (m/round (* pct-done (- t f)))))]
    (if (< pct-done 1)
      (m/Point. (xform (:x from) (:x to))
                (xform (:y from) (:y to)))
      to)))

(defn animation-done? [start now]
  (< (+ start tile-move-time) now))

;; State updates

(defn read-input [state input-map]
  (let [{prev-mouse-state :mouse-state prev-pressed-keys :pressed-keys} (:input state)
        {new-mouse-state :mouse-state new-pressed-keys :pressed-keys} input-map]
    (assoc state
           :input
           (assoc input-map
                  :changed-keys (difference new-pressed-keys prev-pressed-keys)
                  :mouse-clicked? (and (= prev-mouse-state :down)
                                       (= new-mouse-state :up))))))

(defn handle-tile-click [state]
  (if (and (not (interacting? state))
           (get-in state [:input :mouse-clicked?]))
    (let [{:keys [x y]} (get-in state [:input :mouse-position])
          target-tile (-> (m/Point. (quot x camera-scale)
                                    (quot y camera-scale))
                          (m/relative-point (:camera state)) ; previous frame's camera
                          (u/coord->tile))
          target-tile-interactable? (interactable? state target-tile)
          player-pos (get-in state [:player :position])
          path (->> (path/a-star (fn [t]
                                   (or (walkable? state t)
                                       (and (= target-tile t)
                                            target-tile-interactable?)))
                                 player-pos target-tile)
                    (partition 2 1)
                    (map (fn [[p1 p2]] (delta->direction (m/point-delta p1 p2))))
                    (into #queue []))]
      (assoc state
             :action-q (if target-tile-interactable?
                         (conj path :interact)
                         path)
             :move-source :mouse
             :highlight {:position target-tile
                         :start (get-time)}))
    state))

(defn handle-keyboard-input [state]
  (let [{:keys [move-source action-q]
         {:keys [changed-keys]} :input} state
        key? (fn [& k] (seq (intersection changed-keys (set k))))]
    (if-some [action (cond
                       (key? "ArrowUp" "KeyW" "KeyK") :up
                       (key? "ArrowDown" "KeyS" "KeyJ") :down
                       (key? "ArrowLeft" "KeyA" "KeyH") :left
                       (key? "ArrowRight" "KeyD" "KeyL") :right
                       (key? "Space" "Enter") :interact)]
      (assoc state
             :move-source :keys
             :action-q (if (= :keys move-source)
                         (conj action-q action)
                         #queue [action]))
      state)))

(defn player-animation? [state]
  (contains? (:player state) :animation))

(defn player-animation-done?
  "Returns true if a player animation has just finished in this frame."
  [state now]
  (let [animation (get-in state [:player :animation])]
    (and (some? animation)
         (animation-done? (:start animation) now))))

(defn maybe-change-location [state now]
  (if (player-animation-done? state now)
    (let [{location-id :location-id
           {destination :destination} :animation} (:player state)]
      (if-let [[new-location-id new-position]
               (get-in @data [:locations location-id :outbound-connections destination])]
        (-> state
            (assoc :action-q #queue [])
            (dissoc :move-source)
            (update :player merge {:location-id new-location-id
                                   :position new-position}))
        state))
    state))

(defn remove-finished-animations [state now]
  (if (player-animation-done? state now)
    (update state :player dissoc :animation)
    state))

(defn process-action [state now]
  (if-not (player-animation? state)
    (if-let [action (peek (:action-q state))]
      (if (interacting? state)
        (case action
          (:up :down)
          (-> state
              (update :action-q pop)
              (update :interaction
                      (fn [{:keys [options selected-option] :as interaction}]
                        (assoc interaction :selected-option
                               (mod ((case action :up dec :down inc) selected-option)
                                    (count options))))))
          :interact
          (let [{:keys [options selected-option]} (:interaction state)
                {:keys [trigger-changes next-line-id]} (get options selected-option)
                new-state (-> state
                              (update :action-q pop)
                              (update :switches merge trigger-changes))]
            (if (some? next-line-id)
              (assoc new-state :interaction (resolve-interaction state next-line-id))
              (dissoc new-state :interaction)))
          (update state :action-q pop))
        (case action
          (:up :down :left :right)
          (let [old-position (get-in state [:player :position])
                new-position (apply
                               m/translate-point
                               old-position
                               (direction->delta action))]
            (cond-> (-> state
                        (update :action-q pop)
                        (assoc-in [:player :direction] action))
              (walkable? state new-position)
              (update :player
                      assoc
                      :position new-position
                      :animation {:start now
                                  :origin old-position
                                  :destination new-position})))
          :interact
          (if-let [line-id (interaction-line-id state)]
            (-> state
                (update :action-q pop)
                (assoc :interaction (resolve-interaction state line-id)))
            (update state :action-q pop))))
      state)
    state))

(defn update-animation [state now]
  (update state :player
          (fn [{:keys [position direction animation] :as player}]
            (merge player
                   (if-some [{:keys [start origin destination]} animation]
                     {:coord (animated-position start (u/tile->coord origin) (u/tile->coord destination) now)
                      :sprite (animation-sprite now (anim->data start (get-in hare-animations [:walking direction])))}
                     {:coord (u/tile->coord position)
                      :sprite (animation-sprite now (anim->data 0 (get-in hare-animations [:idle direction])))})))))

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
        loc-bounds (get-in @data [:locations location-id :bounds])
        bounds (m/rect-scale loc-bounds tile-size)
        left (camera-coord w (:x bounds) (:w bounds) (:x player-coord))
        top (camera-coord h (:y bounds) (:h bounds) (:y player-coord))]
    (m/Rect. left top w h)))

(defn update-camera [state]
  (let [{:keys [coord location-id]} (:player state)]
    (assoc state :camera (camera-rect coord location-id))))

(defn update-highlight [state now]
  (if-let [start (get-in state [:highlight :start])]
    (let [highlight-t 300
          delta-t (- now start)]
      (assoc-in state [:highlight :completion] (/ delta-t highlight-t)))
    state))

(defn update-state [state input-map now]
  (-> state
      (read-input input-map)
      (handle-keyboard-input)
      (handle-tile-click)
      (maybe-change-location now)
      (remove-finished-animations now)
      (process-action now)
      (update-animation now)
      (update-camera)
      (update-highlight now)))

;; Input Handlers

(defn start-input-loop [channel]
  (go-loop [[command payload :as message] (<! channel)]
           (when message
             (let [handler (case command
                             :key-state (fn [[k key-state]]
                                          (if (= key-state :down)
                                            (swap! input-map update :pressed-keys conj k)
                                            (swap! input-map update :pressed-keys disj k)))
                             :mouse-state #(swap! input-map assoc :mouse-state %)
                             :mouse-position #(swap! input-map assoc :mouse-position %))]
               (handler payload)
               (recur (<! channel))))))

;; Game Loop

(defn start-game [context game-data]
  (reset! state (assoc (:initial-state game-data)
                       :action-q #queue []))
  (reset! data game-data)
  (reset! ctx context)
  (let [input-chan (chan)
        quit (atom false)]
    (load-images
      image-files
      (fn [images]
        (reset! loaded-images images)
        (start-input-loop input-chan)
        (js/requestAnimationFrame
          (fn game-loop []
            (when (and (not @quit) @ctx)
              (let [now (get-time)
                    new-state (swap! state update-state @input-map now)]
                (when-not (s/valid? ::state new-state)
                  (s/explain ::state new-state))
                (render new-state)
                (js/requestAnimationFrame game-loop)))))))
    {:input input-chan
     :quit quit}))

(defn end-game [{:keys [input quit]}]
  (reset! quit true)
  (close! input)
  (reset! state nil)
  (reset! data nil)
  (reset! ctx nil))
