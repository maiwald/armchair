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

;; Input Handlers

(defn handle-cursor-position [state coord]
  (assoc state :highlight (when coord (normalize-to-tile coord))))

(def animation (atom nil))

(defn move-to [state to]
  {:started (.now js/performance)
   :from (:player state)
   :to to})

(defn handle-animate [state coord channel]
  (reset! animation (move-to state (normalize-to-tile coord)))
  (put! channel true)
  state)

(defn start-input-loop [state-atom channel animation-chan]
  (go-loop [[cmd payload] (<! channel)]
           (let [handler (case cmd
                           :cursor-position handle-cursor-position
                           :animate #(handle-animate %1 %2 animation-chan)
                           (fn [state _] state))]
             (reset! state-atom (handler @state-atom payload)))
           (recur (<! channel))))

;; Animations

(defn abs [x] (.abs js/Math x))
(defn round [x] (.round js/Math x))

(defn start-animation-loop [state-atom channel]
  (go-loop [_ (<! channel)]
           (if-let [{s :started [fx fy] :from [tx ty] :to} @animation]
             (let [passed (- (.now js/performance) s)
                   pct (/ passed tile-move-time)
                   dx (- tx fx)
                   dy (- ty fy)]
               (if (< passed tile-move-time)
                 (swap! state-atom assoc :player [(+ fx (round (* pct dx)))
                                                  (+ fy (round (* pct dy)))])
                 (do (swap! state-atom assoc :player [tx ty])
                     (reset! animation nil)))
               (js/setTimeout #(put! channel true), 0)))
           (recur (<! channel))))

;; Game Loop

(def state (atom nil))

(defn start-game [context]
  (reset! state initial-game-state)
  (reset! ctx context)
  (let [input-chan (chan)
        animation-chan (chan)]
    (add-watch state
               :state-update
               (fn [_ _ old-state new-state]
                 (when (not= old-state new-state)
                   (render new-state))))
    (take! (get-texture-atlas-chan)
           #(do (reset! texture-atlas %)
                (start-input-loop state input-chan animation-chan)
                (start-animation-loop state animation-chan)
                (render @state)))
    input-chan))

(defn end-game []
  (remove-watch state :state-update)
  (reset! state nil)
  (reset! ctx nil))
