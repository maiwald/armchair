(ns armchair.location-map.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub subscribe]]
            [com.rpl.specter
             :refer [collect-one ALL FIRST LAST]
             :refer-macros [select]]
            [armchair.config :as config]
            [armchair.util :as u]
            [armchair.math :as m :refer [translate-point]]))

(reg-sub
  :location-map/tile-size
  :<- [:ui/location-map-zoom-scale]
  (fn [zoom-scale] (* config/tile-size zoom-scale)))

(reg-sub
  :location-map/location-characters
  (fn [[_ location-id]]
    [(subscribe [:db/location location-id])
     (subscribe [:db-characters])
     (subscribe [:db-player])])
  (fn [[{:keys [placements bounds]} characters player] [_ location-id]]
    (u/map-keys
      (fn [tile] (m/global-point tile bounds))
      (cond-> (u/map-values
                (fn [{:keys [character-id]}]
                  (let [character (characters character-id)]
                    (select-keys character [:texture :display-name])))
                placements)
        (= location-id (:location-id player))
        (assoc (:location-position player) {:texture ["hare.png" (m/Point. 6 0)]
                                            :display-name "Player"})))))

(reg-sub
  :location-map/location
  (fn [[_ location-id]]
    [(subscribe [:db/location location-id])
     (subscribe [:db-player])
     (subscribe [:location-map/location-characters location-id])
     (subscribe [:ui/dnd])
     (subscribe [:ui/location-preview-cache-background])
     (subscribe [:ui/location-preview-cache-foreground])
     (subscribe [:ui/inspector])
     (subscribe [:ui/location-map-zoom-scale])])
  (fn [[{:keys [display-name placements connection-triggers blocked]
         {:keys [w h] :as bounds} :bounds}
        player
        characters
        [dnd-type & dnd-payload]
        preview-cache-background
        preview-cache-foreground
        [inspector-type inspector-location-id inspector-tile]
        zoom-scale]
       [_ location-id]]
    (let [preview-image-background-src (get preview-cache-background location-id)
          preview-image-foreground-src (get preview-cache-foreground location-id)]
      {:display-name display-name
       :zoom-scale zoom-scale
       :bounds bounds
       :characters characters
       :preview-image-background-src preview-image-background-src
       :preview-image-foreground-src preview-image-foreground-src
       :preview-image-w (* zoom-scale config/tile-size w)
       :preview-image-h (* zoom-scale config/tile-size h)
       :is-inspecting (= [inspector-type inspector-location-id]
                         [:location location-id])
       :inspected-tile (when (= [inspector-type inspector-location-id]
                                [:tile location-id])
                         (m/global-point inspector-tile bounds))
       :can-drop?
       (fn [global-tile]
         (let [tile (m/relative-point global-tile bounds)]
           (not (or (contains? blocked tile)
                    (case dnd-type
                      :tile (or (contains? placements tile)
                                (= [location-id tile]
                                   ((juxt :location-id :location-position) player))
                                (= [location-id tile]
                                   dnd-payload))
                      (or (contains? placements tile)
                          (contains? connection-triggers tile)))))))
       :drag-entity
       (let [occupied (merge
                        (u/map-values
                          (fn [_ k] [:connection-trigger location-id k])
                          connection-triggers)
                        (u/map-values
                          (fn [_ k] [:placement location-id k])
                          placements)
                        (when (= location-id (:location-id player))
                          (u/spy {(:location-position player) [:player]})))]
         (fn [global-tile]
           (let [tile (m/relative-point global-tile bounds)]
             (if-let [entity (occupied tile)]
              entity
              ;; If nothing is occupying the given tile and it is not blocked by
              ;; collision we start dragging the tile itself to start creating a
              ;; new connection trigger.
              (when-not (contains? blocked tile)
                [:tile location-id tile])))))})))

(reg-sub
  :location-map/bounds
  :<- [:db-locations]
  :<- [:ui/positions]
  :<- [:ui/location-map-zoom-scale]
  :<- [:location-map/tile-size]
  (fn [[locations positions zoom-scale tile-size]]
    (m/rect-resize
      (->> locations
           (mapcat (fn [[id {{:keys [w h]} :bounds}]]
                     (let [p (m/point-scale (positions id) zoom-scale)]
                       [p (translate-point p (* w tile-size) (* h tile-size))])))
           m/containing-rect)
      (zipmap [:left :right :top :bottom]
              (repeat (* zoom-scale 400))))))

(reg-sub
  :location-map/location-position
  (fn [[_ location-id]]
    [(subscribe [:ui/position location-id])
     (subscribe [:ui/location-map-zoom-scale])
     (subscribe [:location-map/bounds])])
  (fn [[position zoom-scale bounds]]
    (-> position
        (m/point-scale zoom-scale)
        (m/global-point bounds))))

(reg-sub
  :location-map/connection-position
  (fn [[_ location-id]]
    [(subscribe [:db/location location-id])
     (subscribe [:location-map/location-position location-id])
     (subscribe [:location-map/tile-size])])
  (fn [[location location-position tile-size] [_ _location-id tile]]
    (let [tile-center-offset (+ 1 (* tile-size 0.5))
          tile-position (-> tile
                            (m/global-point (:bounds location))
                            (m/point-scale tile-size))]
      {:tile (-> tile-position
                 (m/translate-point location-position))
       :tile-center (-> tile-position
                        (m/translate-point location-position)
                        (m/translate-point tile-center-offset tile-center-offset))})))

(reg-sub
  :location-map/connections
  :<- [:db-locations]
  (fn [locations]
    (->> locations
         (select [ALL (collect-one FIRST) LAST :connection-triggers ALL])
         (map (fn [[from-id [from-position to]]]
                (vector [from-id from-position] to))))))

(reg-sub
  :location-map/dnd-connection-preview
  :<- [:ui/dnd]
  :<- [:ui/dnd-preview]
  (fn [[[dnd-type & dnd-payload] dnd-preview]]
    (when (and (= dnd-type :tile)
               (some? dnd-preview))
      {:start dnd-payload
       :end dnd-preview})))

(reg-sub
  :location-map
  :<- [:db-locations]
  :<- [:location-map/bounds]
  :<- [:ui/location-map-scroll-center]
  :<- [:ui/location-map-zoom-scale]
  (fn [[locations bounds scroll-center zoom-scale]]
    {:bounds bounds
     :scroll-center scroll-center
     :zoom-scale zoom-scale
     :location-ids (keys locations)}))
