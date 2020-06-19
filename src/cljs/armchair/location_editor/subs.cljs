(ns armchair.location-editor.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [armchair.math :as m]
            [armchair.util :as u]))

(reg-sub
  :location-editor/ui
  (fn [db]
    (select-keys (:location-editor db)
                 [:active-tool
                  :visible-layers
                  :active-layer
                  :active-walk-state
                  :active-texture])))

(reg-sub
  :location-editor/tile-type
  (fn [[_ location-id]]
    (subscribe [:db/location location-id]))
  (fn [location [_ _location-id tile]]
    (cond
      (contains? (:placements location) tile) :placement
      (contains? (:connection-triggers location) tile) :trigger)))
(reg-sub
  :location-editor/player-position
  (fn [{{:keys [location-id location-position]} :player} [_ current-location]]
    (when (= location-id current-location) location-position)))

(reg-sub
  :location-editor/location-exists?
  :<- [:db-locations]
  (fn [locations [_ location-id]]
    (contains? locations location-id)))

(defn inspecting-tile? [inspector location-id tile]
  (= inspector [:tile location-id tile]))

(reg-sub
  :location-editor/characters
  (fn [[_ location-id]]
    [(subscribe [:db/location location-id])
     (subscribe [:db-characters])
     (subscribe [:ui/inspector])])
  (fn [[{placements :placements} characters inspector] [_ location-id]]
    (u/map-values
      (fn [{:keys [character-id]} tile]
        (let [character (characters character-id)]
          (merge (select-keys character [:texture :display-name])
                 {:inspecting? (inspecting-tile? inspector location-id tile)})))
      placements)))

(reg-sub
  :location-editor/location
  :<- [:db-locations]
  (fn [locations [_ location-id]]
    (locations location-id)))

(reg-sub
  :location-editor/connection-trigger-layer
  :<- [:db-locations]
  :<- [:ui/inspector]
  (fn [[locations inspector] [_ location-id]]
    (let [{:keys [bounds connection-triggers]} (locations location-id)]
      {:bounds bounds
       :connection-triggers (u/map-values
                              (fn [[target-id _] tile]
                                {:display-name (get-in locations [target-id :display-name])
                                 :inspecting? (inspecting-tile? inspector location-id tile)})
                              connection-triggers)})))

(reg-sub
  :location-editor/entity-layer
  (fn [[_ location-id]]
    [(subscribe [:db-locations])
     (subscribe [:location-editor/characters location-id])
     (subscribe [:location-editor/player-position location-id])])
  (fn [[locations characters player-position] [_ location-id]]
    {:bounds (get-in locations [location-id :bounds])
     :characters characters
     :player-position player-position}))

(reg-sub
  :location-editor/available-characters
  :<- [:db-characters]
  (fn [characters]
    (->> characters
         (map (fn [[id {:keys [texture display-name]}]]
                {:character-id id
                 :display-name display-name
                 :texture texture}))
         (sort-by :display-name))))

(reg-sub
  :location-editor/dimensions
  (fn [[_ location-id]]
    (subscribe [:db/location location-id]))
  (fn [{{:keys [w h]} :bounds}]
    {:width w
     :height h}))

(reg-sub
  :location-editor/trigger-inspector
  :<- [:db-locations]
  (fn [locations [_ location-id tile]]
    (let [{source-display-name :display-name
           source-bounds :bounds
           :as location} (get locations location-id)
          [target-id position] (get-in location [:connection-triggers tile])
          {target-display-name :display-name
           target-bounds :bounds} (get locations target-id)]
      {:source-display-name source-display-name
       :source-position-normalized (m/global-point tile source-bounds)
       :target-id target-id
       :target-display-name target-display-name
       :target-position position
       :target-position-normalized (m/global-point position target-bounds)})))

(reg-sub
  :location-editor/physically-occupied-tiles
  :<- [:db-locations]
  :<- [:db-player]
  (fn [[locations player] [_ location-id]]
    (let [{:keys [blocked placements]} (get locations location-id)
          player (when (= location-id (:location-id player))
                   [(:location-position player)])]
      (set (concat player blocked (keys placements))))))
