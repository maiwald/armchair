(ns armchair.game
  (:require [clojure.core.async :refer [chan put! take! go go-loop <! >!]]
            [armchair.canvas :as c]
            [armchair.pathfinding :as path]))

;; Definitions

(def tile-size 32)
(def tile-move-time 200) ; miliseconds

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
   :entities {:player (tile->coord [1 13])
              :enemy (tile->coord [12 4])}
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

(defn draw-path [level start end]
  (if (and start end)
    (doseq [path-tile (path/a-star
                        level
                        (coord->tile start)
                        (coord->tile end))]
      (doto @ctx
        c/save!
        (c/set-fill-style! "rgba(255, 255, 0, .2)")
        (c/set-line-width! "2")
        (c/fill-rect! (tile->coord path-tile) tile-size tile-size)
        c/restore!))))

(defn render [state]
  (js/requestAnimationFrame
    #(let [{level :level
            highlight :highlight
            {player :player} :entities} state]
       (when @ctx
         (c/clear! @ctx)
         (draw-level level)
         (draw-player player)
         (draw-path level player highlight)
         (draw-highlight highlight)))))

;; Animations

(def animations (atom (list)))

(defn abs [x] (.abs js/Math x))
(defn round [x] (.round js/Math x))
(defn *now [] (.now js/performance))

(defn *move [from to duration]
  {:start (*now)
   :from from
   :to to
   :duration duration})

(defn *move-sequence [coords duration-per-tile]
  (let [start (*now)
        segments (partition-all 2 (interleave coords (rest coords)))]
    (map-indexed (fn [idx [from to]]
                   {:start (+ start (* idx duration-per-tile))
                    :from from
                    :to to
                    :duration duration-per-tile})
                 segments)))

(defn *animated-position [animation]
  (let [{start :start
         [fx fy] :from
         [tx ty] :to
         duration :duration} animation
        passed (- (*now) start)
        pct (/ passed duration)]
    (if (<= pct 1)
      (let [dx (- tx fx)
            dy (- ty fy)]
        [(+ fx (round (* pct dx)))
         (+ fy (round (* pct dy)))])
      [tx ty])))

(defn *animation-done? [animation]
  (let [{start :start
         duration :duration} animation]
    (< (+ start duration) (*now))))

(defn start-animation-loop [channel]
  (go-loop [_ (<! channel)]
           (if-let [animation (first @animations)]
             (do
               (swap! state assoc-in [:entities :player] (*animated-position animation))
               (if-not (*animation-done? animation)
                 (js/requestAnimationFrame #(put! channel true))
                 (swap! animations rest))))
           (recur (<! channel))))

;; Input Handlers

(defn handle-cursor-position [coord]
  (swap! state assoc :highlight (when coord (normalize-to-tile coord))))

(defn handle-animate [coord]
  (let [path-tiles (path/a-star (:level @state)
                                (coord->tile (get-in @state [:entities :player]))
                                (coord->tile (normalize-to-tile coord)))]
    (reset! animations (*move-sequence (map tile->coord path-tiles) tile-move-time))))

(defn start-input-loop [channel]
  (go-loop [[cmd payload] (<! channel)]
           (let [handler (case cmd
                           :cursor-position handle-cursor-position
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
                   (render new-state))))
    (add-watch animations
               :animation-update
               (fn [_ _ old-state new-state]
                 (when (and (empty? old-state)
                            (some? new-state))
                   (put! animation-chan true))))
    (take! (get-texture-atlas-chan)
           #(do (reset! texture-atlas %)
                (start-input-loop input-chan)
                (start-animation-loop animation-chan)
                (render @state)))
    input-chan))

(defn end-game []
  (remove-watch state :state-update)
  (remove-watch animations :animation-update)
  (reset! state nil)
  (reset! animations nil)
  (reset! ctx nil))
