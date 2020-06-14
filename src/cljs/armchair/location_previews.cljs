(ns armchair.location-previews
  (:require [re-frame.core :refer [dispatch ->interceptor]]
            [armchair.textures :refer [load-textures]]
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

(defn location-preview-url [{rect :bounds :as location} callback]
  (let [texture-files (-> (select [(multi-path :background1
                                               :background2
                                               :foreground1
                                               :foreground2) MAP-VALS FIRST] location)
                          distinct
                          vec)
        w (* config/tile-size (:w rect))
        h (* config/tile-size (:h rect))]
    (load-textures
      texture-files
      (fn [atlas]
        (let [foreground-canvas (create-canvas w h)
              foreground-ctx (.getContext foreground-canvas "2d")
              background-canvas (create-canvas w h)
              background-ctx (.getContext background-canvas "2d")]
          (canvas/clear! background-ctx)
          (canvas/fill-rect! background-ctx (Rect. 0 0 w h))
          (doseq [layer (vector :background1 :background2)]
            (doseq [x (range (:x rect) (+ (:x rect) (:w rect)))
                    y (range (:y rect) (+ (:y rect) (:h rect)))
                    :let [tile (Point. x y)
                          [file {:keys [x y]}] (get-in location [layer tile])]
                    :when (some? file)]
              (if-let [sprite-sheet (get atlas file)]
                (canvas/draw-image! background-ctx sprite-sheet
                                    (Point. (* config/tile-size x)
                                            (* config/tile-size y))
                                    (u/tile->coord (global-point tile rect))))))
          (doseq [layer (vector :foreground1 :foreground2)]
            (doseq [x (range (:x rect) (+ (:x rect) (:w rect)))
                    y (range (:y rect) (+ (:y rect) (:h rect)))
                    :let [tile (Point. x y)
                          [file {:keys [x y]}] (get-in location [layer tile])]
                    :when (some? file)]
              (if-let [sprite-sheet (get atlas file)]
                (canvas/draw-image! foreground-ctx sprite-sheet
                                    (Point. (* config/tile-size x)
                                            (* config/tile-size y))
                                    (u/tile->coord (global-point tile rect))))))
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
