(ns armchair.textures
  (:require [clojure.core.async :refer [go chan take! put! <!]]
            [armchair.config :refer [tile-size]]
            [armchair.util :as u]
            [armchair.math :refer [Point Rect]]
            [com.rpl.specter
             :refer [multi-path MAP-VALS]
             :refer-macros [select]]))

(def tile-sprite-sheets
  {"32x32_map_tile_v3.1.png" (Rect. 0 0 736 928)
   "Castle2.png" (Rect. 0 0 512 512)
   "PathAndObjects_0.png" (Rect. 0 0 512 512)
   "adobe.png" (Rect. 0 0 256 384)
   "misc.png" (Rect. 0 0 704 32)
   "mountain_landscape.png" (Rect. 0 0 512 512)})

(def character-sprite-sheets
  {"characters.png" (Rect. 0 0 128 1664)})

(def animation-sprite-sheets
  {"guy_sprite_sheet.png" (Rect. 0 0 96 128)
   "hare.png" (Rect. 0 0 448 32)})

(def image-files
  (into #{"arrow.png" "exit.png" "missing_texture.png" "player.png"}
        (concat (keys tile-sprite-sheets)
                (keys character-sprite-sheets)
                (keys animation-sprite-sheets))))

(defn image-path [file-name]
  (str "images/" (image-files file-name "missing_texture.png")))

(defn load-textures [callback]
  (let [atlas (atom {})
        loaded (chan)]
    (run! (fn [file-name]
            (let [image (js/Image.)]
              (set! (.-onload image) #(put! loaded [file-name image]))
              (set! (.-src image) (image-path file-name))))
          (conj image-files "missing_texture.png" "arrow.png"))
    (take! (go
             (while (not= (count @atlas) (count image-files))
               (let [[file-name image] (<! loaded)]
                 (swap! atlas assoc file-name image)))
             @atlas)
           callback)))
