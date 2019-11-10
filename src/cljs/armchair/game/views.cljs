(ns armchair.game.views
  (:require [clojure.core.async :refer [put!]]
            [reagent.core :as r]
            [armchair.util :as u :refer [<sub prevent-e! px]]
            [armchair.config :refer [tile-size
                                     camera-tile-width
                                     camera-tile-height
                                     camera-scale]]
            [armchair.components :refer [icon]]
            [armchair.game.core :refer [start-game end-game]]))

(defn game-canvas [game-data]
  (let [game-handle (atom nil)]
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
                  (when-not (.-repeat e)
                    (put! (:input @game-handle) action)))))
            (on-click [e]
              (let [point (u/relative-cursor e (.-currentTarget e))]
                (put! (:input @game-handle) [:click point])))]
      (r/create-class
        {:display-name "game-canvas"
         :component-did-mount
         (fn [this]
           (reset! game-handle (start-game (.getContext (r/dom-node this) "2d")
                                           (r/props this)))
           (.addEventListener js/document "keydown" on-key-down))

         :component-will-unmount
         (fn [this]
           (.removeEventListener js/document "keydown" on-key-down)
           (end-game @game-handle))

         :component-did-update
         (fn [this]
           (end-game @game-handle)
           (reset! game-handle (start-game (.getContext (r/dom-node this) "2d")
                                           (r/props this))))

         :reagent-render
         (fn []
           (let [w (* tile-size camera-tile-width camera-scale)
                 h (* tile-size camera-tile-height camera-scale)]
             [:canvas {:id "game"
                       :on-click on-click
                       :width w
                       :height h
                       :style {:width (px w)
                               :height (px h)}}]))}))))

(defn game-view []
  [:div {:class "content-wrapper"}
   [game-canvas (<sub [:game/data])]
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
