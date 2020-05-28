(ns armchair.location-editor.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [armchair.util :as u]))

(reg-sub
  :location-editor/ui
  (fn [db]
    (select-keys (:location-editor db)
                 [:active-pane
                  :active-tool
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

(reg-sub
  :location-editor/characters
  :<- [:db-locations]
  :<- [:db-characters]
  :<- [:ui/inspector]
  (fn [[locations characters [inspector-type {inspector-location-id :location-id
                                              inspector-location-position :location-position}]] [_ location-id]]
    (->> (locations location-id)
         :placements
         (u/map-values
           (fn [{:keys [character-id]} tile]
             (let [character (characters character-id)]
               (merge (select-keys character [:texture :display-name])
                      {:inspecting? (and (= inspector-type :placement)
                                         (= inspector-location-id location-id)
                                         (= inspector-location-position tile))})))))))

(reg-sub
  :location-editor/location
  :<- [:db-locations]
  (fn [locations [_ location-id]]
    (locations location-id)))

(reg-sub
  :location-editor/connection-trigger-layer
  :<- [:db-locations]
  :<- [:ui/inspector]
  (fn [[locations [inspector-type {inspector-location-id :location-id
                                   inspector-location-position :location-position}]] [_ location-id]]
    (let [{:keys [bounds connection-triggers]} (locations location-id)]
      {:bounds bounds
       :connection-triggers (u/map-values
                              (fn [[target-id _] tile]
                                {:display-name (get-in locations [target-id :display-name])
                                 :inspecting? (and (= inspector-type :exit)
                                                   (= inspector-location-id location-id)
                                                   (= inspector-location-position tile))})
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
  :location-editor/location-inspector
  :<- [:db-locations]
  :<- [:db-characters]
  (fn [[locations characters] [_ location-id]]
    (let [location (locations location-id)]
      {:display-name (:display-name location)
       :characters (->> location :placements
                        (map (fn [[_ {:keys [character-id]}]]
                               (let [character (get characters character-id)]
                                 {:character-id character-id
                                  :character-name (:display-name character)
                                  :character-color (:color character)})))
                        distinct
                        (sort-by :character-name))})))

(reg-sub
  :location-editor/dimensions
  :<- [:db-locations]
  (fn [locations [_ location-id]]
    (let [bounds (get-in locations [location-id :bounds])]
      {:width (:w bounds)
       :height (:h bounds)})))

(reg-sub
  :location-editor/placement-inspector
  :<- [:db-locations]
  :<- [:db-dialogues]
  :<- [:db-characters]
  :<- [:character-options]
  (fn [[locations dialogues characters character-options] [_ location-id tile]]
    (let [{:keys [character-id dialogue-id]} (get-in locations [location-id :placements tile])
          texture (get-in characters [character-id :texture])
          dialogue-options (->> dialogues
                                (u/filter-map #(= character-id (:character-id %)))
                                (u/map-values :synopsis))]
      {:character-id character-id
       :character-options character-options
       :dialogue-id dialogue-id
       :dialogue-options dialogue-options
       :texture texture})))

(reg-sub
  :location-editor/trigger-inspector
  :<- [:db-locations]
  (fn [locations [_ location-id tile]]
    (let [[target-id position] (get-in locations [location-id :connection-triggers tile])
          display-name (get-in locations [target-id :display-name])]
      {:display-name display-name
       :target-id target-id
       :target-position position})))

(reg-sub
  :location-editor/physically-occupied-tiles
  :<- [:db-locations]
  :<- [:db-player]
  (fn [[locations player] [_ location-id]]
    (let [{:keys [blocked placements]} (get locations location-id)
          player (when (= location-id (:location-id player))
                   [(:location-position player)])]
      (set (concat player blocked (keys placements))))))
