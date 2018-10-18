(ns armchair.textures
  (:require [clojure.core.async :refer [go chan take! put! <!]]))

(def background-textures [:wall
                          :stairs
                          :red_wall-top-left
                          :red_wall-top
                          :red_wall_top-right
                          :red_wall-left
                          :red_wall-center
                          :red_wall-right
                          :red_wall-bottom-left
                          :red_wall-bottom
                          :red_wall-bottom-right
                          :house_arch_top
                          :house_bottom_left
                          :house_bottom_right
                          :house_door_bottom
                          :house_roof_middle
                          :house_roof_top
                          :house_roof_bottom-left
                          :house_roof_bottom-right
                          :house_roof_bottom
                          :house_roof_bottom2
                          :house_roof_middle-left
                          :house_roof_middle-right
                          :house_roof_top-left
                          :house_roof_top-right
                          :dirt
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
                          :stone])

(def character-textures [:agent
                         :dead_squirrel_idle
                         :droog_idle
                         :girl_idle
                         :goth_idle
                         :hugo
                         :gustav
                         :hi-tops_idle
                         :punker
                         :rourke
                         :yakuza_idle])

(def texture-set (set (-> [:player
                           :enemy
                           :arrow
                           :exit
                           :missing_texture]
                          (into background-textures)
                          (into character-textures))))

(defn texture-path [texture-name]
  (if (contains? texture-set texture-name)
    (str "images/" (name texture-name) ".png")
    (str "images/missing_texture.png")))

(defn load-textures [callback]
  (let [atlas (atom {})
        loaded (chan)]
    (run! (fn [texture-name]
            (let [image (js/Image.)]
              (set! (.-onload image) #(put! loaded [texture-name image]))
              (set! (.-src image) (texture-path texture-name))))
          texture-set)
    (take! (go
             (while (not= (count @atlas) (count texture-set))
               (let [[texture-name texture-image] (<! loaded)]
                 (swap! atlas assoc texture-name texture-image)))
             @atlas)
           callback)))
