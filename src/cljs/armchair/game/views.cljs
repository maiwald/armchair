(ns armchair.game.views
  (:require [clojure.core.async :refer [put!]]
            [reagent.core :as r]
            [armchair.util :refer [<sub relative-cursor]]
            [armchair.components :refer [icon]]
            [armchair.game.core :refer [start-game end-game]]))

(defn game-canvas [game-data]
  (let [game-data (<sub [:game/data])
        background-canvas (atom nil)
        entity-canvas (atom nil)
        game-input (atom nil)
        key-listener (fn [e]
                       (when-let [action (case (.-code e)
                                           ("ArrowUp" "KeyW" "KeyK") (put! @game-input [:move :up])
                                           ("ArrowRight" "KeyD" "KeyL") (put! @game-input [:move :right])
                                           ("ArrowDown" "KeyS" "KeyJ") (put! @game-input [:move :down])
                                           ("ArrowLeft" "KeyA" "KeyH") (put! @game-input [:move :left])
                                           ("Space" "Enter") (put! @game-input [:interact])
                                           nil)]
                         (.preventDefault e)))]
    (r/create-class
      {:display-name "game-canvas"
       :component-did-mount
       (fn []
         (reset! game-input (start-game
                              (.getContext @background-canvas "2d")
                              (.getContext @entity-canvas "2d")
                              game-data))
         (.addEventListener js/document "keydown" key-listener))

       :component-will-unmount
       (fn []
         (.removeEventListener js/document "keydown" key-listener)
         (end-game))

       :reagent-render
       (fn []
         [:div {:class "content-wrapper"}
          [:div {:id "game"
                 :style {:width (str 800 "px")
                         :height (str 448 "px")}}
           [:canvas {:height 448
                     :width 800
                     :ref (fn [el] (reset! background-canvas el))}]
           [:canvas {:on-mouse-move #(let [c (relative-cursor % @entity-canvas)]
                                       (put! @game-input [:cursor-position c]))
                     :on-mouse-out #(put! @game-input [:cursor-position nil])
                     :height 448
                     :width 800
                     :ref (fn [el] (reset! entity-canvas el))}]]
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
            " to interact."]]])})))
