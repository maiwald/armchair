(ns armchair.textures
  (:require [clojure.core.async :refer [go chan take! put! <!]]
            [armchair.util :refer [px]]
            [armchair.config :refer [tile-size]]
            [armchair.math :refer [Rect]]))

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

(def image-sizes
  (merge tile-sprite-sheets
         character-sprite-sheets
         animation-sprite-sheets))

(defn image-size [file-name]
  (get image-sizes file-name (Rect. 0 0 tile-size tile-size)))

(defn image-path [file-name]
  (str "images/" (image-files file-name "missing_texture.png")))

(defn load-textures [files callback]
  (let [atlas (atom {})
        loaded (chan)]
    (run! (fn [file-name]
            (let [image (js/Image.)]
              (set! (.-onload image) #(put! loaded [file-name image]))
              (set! (.-src image) (image-path file-name))))
          files)
    (take! (go
             (while (not= (count @atlas) (count files))
               (let [[file-name image] (<! loaded)]
                 (swap! atlas assoc file-name image)))
             @atlas)
           callback)))

;; Sprite Texture

(defn sprite-texture [[file {:keys [x y]}] title zoom-scale]
  (let [{:keys [w h]} (image-size file)
        zoom-scale (or zoom-scale 1)]
    [:div
     {:title title
      :style {:width (px (* zoom-scale tile-size))
              :height (px (* zoom-scale tile-size))
              :background-repeat "no-repeat"
              :background-image (str "url(" (image-path file) ")")
              :background-size (str (px (* w zoom-scale))
                                    " "
                                    (px (* h zoom-scale)))
              :background-position (str (px (- (* zoom-scale tile-size x)))
                                        " "
                                        (px (- (* zoom-scale tile-size y))))}}]))

