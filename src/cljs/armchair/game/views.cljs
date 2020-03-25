(ns armchair.game.views
  (:require [clojure.core.async :refer [put!]]
            [reagent.core :as r]
            [reagent.dom :as rd]
            [armchair.util :as u :refer [<sub prevent-e! px]]
            [armchair.config :refer [tile-size
                                     camera-tile-width
                                     camera-tile-height
                                     camera-scale]]
            [armchair.components :refer [icon]]
            [armchair.game.core :refer [start-game end-game]]))

(def allowed-keys
  #{"ArrowUp" "KeyW" "KeyK"
    "ArrowRight" "KeyD" "KeyL"
    "ArrowDown" "KeyS" "KeyJ"
    "ArrowLeft" "KeyA" "KeyH"
    "Space" "Enter"})

(defn game-canvas []
  (let [game-handle (atom nil)]
    (letfn [(on-key-down [e]
              (when-not (or (.-altKey e)
                            (.-ctrlKey e)
                            (.-metaKey e))
                (when-let [keycode (allowed-keys (.-code e))]
                  (prevent-e! e)
                  (when-not (.-repeat e)
                    (put! (:input @game-handle) [:key-state [keycode :down]])))))
            (on-key-up [e]
              (when-let [keycode (allowed-keys (.-code e))]
                (prevent-e! e)
                (put! (:input @game-handle) [:key-state [keycode :up]])))
            (on-pointer-down []
              (put! (:input @game-handle) [:mouse-state :down]))
            (on-pointer-up []
              (put! (:input @game-handle) [:mouse-state :up]))
            (on-pointer-move [e]
              (let [point (u/relative-cursor e (.-currentTarget e))]
                (put! (:input @game-handle) [:mouse-position point])))]
      (r/create-class
        {:display-name "game-canvas"
         :component-did-mount
         (fn [this]
           (reset! game-handle (start-game (.getContext (rd/dom-node this) "2d")
                                           (r/props this)))
           (.addEventListener js/document "keydown" on-key-down)
           (.addEventListener js/document "keyup" on-key-up))

         :component-will-unmount
         (fn []
           (.removeEventListener js/document "keydown" on-key-down)
           (.removeEventListener js/document "keyup" on-key-up)
           (end-game @game-handle))

         :component-did-update
         (fn [this]
           (end-game @game-handle)
           (reset! game-handle (start-game (.getContext (rd/dom-node this) "2d")
                                           (r/props this))))

         :reagent-render
         (fn []
           (let [w (* tile-size camera-tile-width camera-scale)
                 h (* tile-size camera-tile-height camera-scale)]
             [:canvas#game__view {:on-mouse-down on-pointer-down
                                  :on-mouse-up on-pointer-up
                                  :on-mouse-move on-pointer-move
                                  :on-touch-start on-pointer-down
                                  :on-touch-end on-pointer-down
                                  :on-touch-move on-pointer-move
                                  :width w
                                  :height h
                                  :style {:width (px w)
                                          :height (px h)}}]))}))))

(defn game-view []
  [:div#game
   [game-canvas (<sub [:game/data])]
   [:div#game__help
    [:p "Use mouse for movement, selection and interaction."]
    [:p
     "On keyboard use "
     [:span [icon "arrow-left" "Arrow Left"]] " "
     [:span [icon "arrow-up" "Arrow Up"]] " "
     [:span [icon "arrow-down" "Arrow Down"]] " "
     [:span [icon "arrow-right" "Arrow Right"]] " "
     "or "
     [:span "w"] " "
     [:span "a"] " "
     [:span "s"] " "
     [:span "d"] " "
     "for movement and selection"]
    [:p
     "and "
     [:span "Space"]
     " or "
     [:span "Enter"]
     " to interact."]]])
