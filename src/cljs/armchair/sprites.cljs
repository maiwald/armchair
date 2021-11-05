(ns armchair.sprites
  (:require [clojure.core.async :refer [go chan take! put! <!]]
            [clojure.set :refer [union]]
            [armchair.util :refer [px]]
            [armchair.config :as config]
            [armchair.math :as m :refer [Rect]]))

(defn make-sprite-sheet
  ([file-name bounds]
   {:file-name file-name :bounds bounds :tile-size config/tile-size :gutter 0 :offset 0})
  ([file-name bounds tile-size]
   {:file-name file-name :bounds bounds :tile-size tile-size :gutter 0 :offset 0})
  ([file-name bounds tile-size gutter offset]
   {:file-name file-name :bounds bounds :tile-size tile-size :gutter gutter :offset offset}))

(def tile-sprite-sheets
  #{(make-sprite-sheet "32x32_map_tile_v3.1.png" (Rect. 0 0 736 928))
    (make-sprite-sheet "Castle2.png" (Rect. 0 0 512 512))
    (make-sprite-sheet "PathAndObjects_0.png" (Rect. 0 0 512 512))
    (make-sprite-sheet "adobe.png" (Rect. 0 0 256 384))
    (make-sprite-sheet "misc.png" (Rect. 0 0 704 32))
    (make-sprite-sheet "mountain_landscape.png" (Rect. 0 0 512 512))
    (make-sprite-sheet "roguelikeSheet_transparent.png" (Rect. 0 0 968 526) 16 1 0)})

(def character-sprite-sheets
  #{(make-sprite-sheet "characters.png" (Rect. 0 0 128 1664))})

(def animation-sprite-sheets
  #{(make-sprite-sheet "guy_sprite_sheet.png" (Rect. 0 0 96 128))
    (make-sprite-sheet "hare.png" (Rect. 0 0 448 32))})

(def sprite-sheets
  (into {} (map (fn [s] [(:file-name s) s])
                (union tile-sprite-sheets
                       character-sprite-sheets
                       animation-sprite-sheets))))

(def image-files
  (into #{"arrow.png" "exit.png" "missing_texture.png" "player.png"}
        (keys sprite-sheets)))

(defn image-path [file-name]
  (str "images/" (image-files file-name "missing_texture.png")))

(defn sprite-sheet-size [file-name]
  (if-let [{:keys [tile-size bounds]} (sprite-sheets file-name)]
    (m/rect-scale bounds (/ config/tile-size tile-size))
    (Rect. 0 0 config/tile-size config/tile-size)))

(defn load-images [file-names callback]
  (let [atlas (atom {})
        loaded (chan)]
    (run! (fn [file-name]
            (let [image (js/Image.)]
              (set! (.-onload image) #(put! loaded [file-name image]))
              (set! (.-src image) (image-path file-name))))
          file-names)
    (take! (go
             (while (not= (count @atlas) (count file-names))
               (let [[file-name image] (<! loaded)]
                 (swap! atlas assoc file-name image)))
             @atlas)
           callback)))

;; Sprite Component

(defn Sprite [[file-name {:keys [x y]}] title zoom-scale]
  (let [{:keys [tile-size gutter offset]
         {:keys [w h]} :bounds} (sprite-sheets file-name)
        tile-scale (/ config/tile-size tile-size)
        zoom-scale (or zoom-scale 1)
        bg-size (fn [dimension]
                  (px (* zoom-scale tile-scale dimension)))
        bg-position (fn [dimension]
                      (px (* zoom-scale
                             (- (* tile-scale offset)
                                (* (* tile-scale (+ tile-size gutter)) dimension)))))]
    [:div
     {:title title
      :style {:width (px (* zoom-scale config/tile-size))
              :height (px (* zoom-scale config/tile-size))
              :image-rendering "pixelated"
              :background-repeat "no-repeat"
              :background-image (str "url(" (image-path file-name) ")")
              :background-size (str (bg-size w) " " (bg-size h))
              :background-position (str (bg-position x) " " (bg-position y))}}]))

