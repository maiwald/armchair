(ns armchair.game
  (:require [clojure.core.async :refer [chan put! take! go go-loop <! >!]]
            [armchair.canvas :as c]
            [armchair.pathfinding :as path]))

;; Definitions

(def tile-size 32)
(def tile-move-time 1000) ; miliseconds

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
  (js/requestAnimationFrame
    #(do
       (when @ctx
         (c/clear! @ctx)
         (draw-level (:level state))
         (draw-player (:player state))
         (draw-path state)
         (draw-highlight (:highlight state))))))

;; Animations

(def animations (atom (list)))

(defn abs [x] (.abs js/Math x))
(defn round [x] (.round js/Math x))

(defn move [from to duration]
  {:started (.now js/performance)
   :from from
   :to to
   :duration duration})

(defn start-animation-loop [channel]
  (go-loop [_ (<! channel)]
           (.log js/console (first @animations))
           (if-let [{started :started
                     [fx fy] :from
                     [tx ty] :to
                     duration :duration} (first @animations)]
             (let [passed (- (.now js/performance) started)
                   pct (/ passed duration)
                   dx (- tx fx)
                   dy (- ty fy)]
               (if (<= pct 1)
                 (swap! state assoc :player [(+ fx (round (* pct dx)))
                                             (+ fy (round (* pct dy)))])
                 (do (swap! state assoc :player [tx ty])
                     (swap! animations rest)))
               (js/setTimeout #(put! channel true), 0)))
           (recur (<! channel))))

;; Input Handlers

(defn handle-cursor-position [coord]
  (swap! state assoc :highlight (when coord (normalize-to-tile coord))))

(defn handle-animate [coord]
  (let [path-tiles (path/a-star (:level @state)
                                (coord->tile (:player @state))
                                (coord->tile (normalize-to-tile coord)))
        segments (partition-all 2 (interleave path-tiles (rest path-tiles)))]
    (.log js/console (map (fn [[from to]]
                            (move (tile->coord from)
                                  (tile->coord to)
                                  tile-move-time))
                          segments))
    (.log js/console segments)
    (reset! animations (map (fn [[from to]]
                              (move (tile->coord from)
                                    (tile->coord to)
                                    tile-move-time))
                            segments))))

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
                 (when (and (nil? old-state)
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
