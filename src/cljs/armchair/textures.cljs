(ns armchair.textures
  (:require [clojure.core.async :refer [go chan take! put! <!]]))

(def background-textures [:dirt
                          :grass_dirt_bottom-left
                          :grass_dirt_bottom
                          :grass_dirt_bottom-right
                          :grass_dirt_left
                          :grass_dirt_right
                          :grass_dirt_top-left
                          :grass_dirt_top
                          :grass_dirt_top-right
                          :grass
                          :grass_stone_bottom-left
                          :grass_stone_bottom
                          :grass_stone_bottom-right
                          :grass_stone_left
                          :grass_stone_right
                          :grass_stone_top-left
                          :grass_stone_top
                          :grass_stone_top-right
                          :stone_2
                          :stone_bush
                          :stone_grass_bottom-left
                          :stone_grass_bottom-right
                          :stone_grass_top-left
                          :stone_grass_top-right
                          :stone
                          :wall])

(def textures (into background-textures [:player
                                         :hugo
                                         :gustav
                                         :enemy
                                         :arrow]))

(defn texture-path [texture-name]
  (str "/images/" (name texture-name) ".png"))

(defn load-textures [callback]
  (let [atlas (atom {})
        loaded (chan)]
    (run! (fn [texture-name]
            (let [image (js/Image.)]
              (set! (.-onload image) #(put! loaded [texture-name image]))
              (set! (.-src image) (texture-path texture-name))))
          textures)
    (take! (go
             (while (not= (count @atlas) (count textures))
               (let [[texture-name texture-image] (<! loaded)]
                 (swap! atlas assoc texture-name texture-image)))
             @atlas)
           callback)))
