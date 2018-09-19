(ns armchair.textures
  (:require [clojure.core.async :refer [go chan take! put! <!]]))

(def background-textures [:grass
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
