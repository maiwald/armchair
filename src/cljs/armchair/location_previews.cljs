(ns armchair.location-previews
  (:require [re-frame.core :refer [dispatch ->interceptor]]
            [armchair.textures :refer [sprite-sheets load-textures]]
            [armchair.events :refer [reg-event-meta]]
            [com.rpl.specter
             :refer [multi-path FIRST MAP-VALS]
             :refer-macros [select]]
            [armchair.config :as config]
            [armchair.game.canvas :as canvas]
            [armchair.math :refer [Point Rect global-point]]
            [armchair.util :as u]
            [goog.object :as g]))

(defn create-canvas [w h]
  (doto (.createElement js/document "canvas")
    (g/set "width" w)
    (g/set "height" h)))

(defn draw-layer [ctx layer {rect :bounds :as location} atlas]
  (g/set ctx "imageSmoothingEnabled" false)
  (doseq [x (range (:x rect) (+ (:x rect) (:w rect)))
          y (range (:y rect) (+ (:y rect) (:h rect)))
          :let [tile (Point. x y)
                [file {:keys [x y]}] (get-in location [layer tile])]
          :when (some? file)]
    (if-let [sprite-sheet (get atlas file)]
      (let [{:keys [tile-size gutter offset]} (sprite-sheets file)]
        (canvas/draw-image! ctx sprite-sheet
                            (Point. (+ offset (* (+ gutter tile-size) x))
                                    (+ offset (* (+ gutter tile-size) y)))
                            [tile-size tile-size]
                            (u/tile->coord (global-point tile rect))
                            [config/tile-size config/tile-size])))))

(defn location-preview-url [{rect :bounds :as location} callback]
  (let [texture-files (-> (select [(multi-path :background1
                                               :background2
                                               :foreground1
                                               :foreground2) MAP-VALS FIRST] location)
                          distinct
                          vec)
        canvas-w (* config/tile-size (:w rect))
        canvas-h (* config/tile-size (:h rect))]
    (load-textures
      texture-files
      (fn [atlas]
        (let [foreground-canvas (create-canvas canvas-w canvas-h)
              foreground-ctx (.getContext foreground-canvas "2d")
              background-canvas (create-canvas canvas-w canvas-h)
              background-ctx (.getContext background-canvas "2d")]
          (canvas/clear! background-ctx)
          (canvas/fill-rect! background-ctx (Rect. 0 0 canvas-w canvas-h))
          (doseq [layer (vector :background1 :background2)]
            (draw-layer background-ctx layer location atlas))
          (doseq [layer (vector :foreground1 :foreground2)]
            (draw-layer foreground-ctx layer location atlas))
          (callback (.toDataURL background-canvas)
                    (.toDataURL foreground-canvas)))))))

(reg-event-meta
  ::generate-preview
  (fn [db [_ location-id]]
    (let [location (get-in db [:locations location-id])]
      (location-preview-url location
                            #(dispatch [::store location-id %1 %2])))
    db))

(def build-location-preview
  (->interceptor
    :id ::build-preview
    :after (fn [context]
             (let [location-id (get-in context [:coeffects :event 1])
                   location (get-in context [:effects :db :locations location-id])]
               (location-preview-url
                 location
                 #(dispatch [::store location-id %1 %2])))
             context)))

(reg-event-meta
  ::regenerate-all
  (fn [db]
    (doseq [[location-id location] (:locations db)]
      (location-preview-url
        location
        #(dispatch [::store location-id %1 %2])))
    db))

(reg-event-meta
  ::store
  (fn [db [_ location-id background foreground]]
    (-> db
        (assoc-in [:ui/location-preview-cache-background location-id] background)
        (assoc-in [:ui/location-preview-cache-foreground location-id] foreground))))
