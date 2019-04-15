(ns armchair.game.views
  (:require [clojure.core.async :refer [put!]]
            [reagent.core :as r]
            [armchair.util :refer [<sub prevent-e! px]]
            [armchair.config :refer [tile-size
                                     camera-tile-width
                                     camera-tile-height
                                     camera-scale]]
            [armchair.components :refer [icon]]
            [armchair.game.core :refer [start-game end-game]]))

(defn game-canvas []
  (let [game-data (<sub [:game/data])
        canvas (atom nil)
        game-handle (atom nil)
        keypresses (atom #{})]
    (letfn [(on-key-down [e]
              (let [keycode (.-code e)]
                (when-let [action (case keycode
                                    ("ArrowUp" "KeyW" "KeyK") [:move :up]
                                    ("ArrowRight" "KeyD" "KeyL") [:move :right]
                                    ("ArrowDown" "KeyS" "KeyJ") [:move :down]
                                    ("ArrowLeft" "KeyA" "KeyH") [:move :left]
                                    ("Space" "Enter") [:interact]
                                    nil)]
                  (prevent-e! e)
                  (when (not (contains? @keypresses keycode))
                    (swap! keypresses conj keycode)
                    (put! (:input @game-handle) action)))))
            (on-key-up [e]
              (swap! keypresses disj (.-code e)))]
      (r/create-class
        {:display-name "game-canvas"
         :component-did-mount
         (fn []
           (reset! game-handle (start-game (.getContext @canvas "2d")
                                           game-data))
           (.addEventListener js/document "keydown" on-key-down)
           (.addEventListener js/document "keyup" on-key-up))

         :component-will-unmount
         (fn []
           (.removeEventListener js/document "keydown" on-key-down)
           (.removeEventListener js/document "keyup" on-key-up)
           (end-game @game-handle)
           (reset! canvas nil))

         :reagent-render
         (fn []
           (let [w (* tile-size camera-tile-width camera-scale)
                 h (* tile-size camera-tile-height camera-scale)]
             [:div {:id "game"}
              [:canvas {:width w
                        :height h
                        :style {:width (px w)
                                :height (px h)}
                        :ref (fn [el] (reset! canvas el))}]]))}))))

(defn game-view []
  [:div {:class "content-wrapper"}
   [game-canvas]
   [:div {:id "game-help"}
    [:p
     "Use "
     [:span [icon "arrow-left" "Arrow Left"]] " "
     [:span [icon "arrow-up" "Arrow Up"]] " "
     [:span [icon "arrow-down" "Arrow Down"]] " "
     [:span [icon "arrow-right" "Arrow Right"]] " "
     "or "
     [:span "w"] " "
     [:span "a"] " "
     [:span "s"] " "
     [:span "d"] " "
     "for movement and selection."]
    [:p
     "Use "
     [:span "Space"]
     " or "
     [:span "Enter"]
     " to interact."]]])
