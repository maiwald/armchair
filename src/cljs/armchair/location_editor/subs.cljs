(ns armchair.location-editor.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [com.rpl.specter
             :refer [keypath ALL MAP-KEYS MAP-VALS]
             :refer-macros [select select-one!]]
            [armchair.math :refer [relative-point]]
            [armchair.util :as u]))

(reg-sub
  :location-editor/dnd-payload
  (fn [db] (get-in db [:location-editor :dnd-payload])))

(reg-sub
  :location-editor/ui
  (fn [db]
    (select-keys (:location-editor db)
                 [:highlight
                  :active-pane
                  :active-tool
                  :visible-layers
                  :active-layer
                  :active-walk-state
                  :active-texture])))

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
  (fn [[locations characters] [_ location-id]]
    (->> (locations location-id)
         :placements
         (u/map-values
           (fn [{:keys [character-id]}]
             (let [character (characters character-id)]
               (select-keys character [:texture :display-name])))))))

(reg-sub
  :location-editor/location
  :<- [:db-locations]
  (fn [locations [_ location-id]]
    (locations location-id)))

(reg-sub
  :location-editor/connection-trigger-layer
  :<- [:db-locations]
  (fn [locations [_ location-id]]
    (let [{:keys [dimension connection-triggers]} (locations location-id)]
      {:dimension dimension
       :connection-triggers (u/map-values
                              (fn [[target-id _]]
                                (get-in locations [target-id :display-name]))
                              connection-triggers)})))

(reg-sub
  :location-editor/entity-layer
  (fn [[_ location-id]]
    [(subscribe [:db-locations])
     (subscribe [:location-editor/characters location-id])
     (subscribe [:location-editor/player-position location-id])])
  (fn [[locations characters player-position] [_ location-id]]
    {:dimension (get-in locations [location-id :dimension])
     :characters characters
     :player-position player-position}))

(reg-sub
  :location-editor/available-npcs
  :<- [:db-characters]
  :<- [:db-dialogues]
  (fn [[characters dialogues] _]
    (let [placed-characters (->> (vals dialogues)
                                 (filter #(contains? % :location-id))
                                 (reduce #(conj %1 (:character-id %2)) #{}))]
      (u/filter-map #(not (contains? placed-characters (:entity/id %)))
                    characters))))

(reg-sub
  :location-editor/display-name
  :<- [:db-locations]
  (fn [locations [_ location-id]]
    (get-in locations [location-id :display-name])))

(reg-sub
  :location-editor/dimensions
  :<- [:db-locations]
  (fn [locations [_ location-id]]
    (let [dimension (get-in locations [location-id :dimension])]
      {:width (:w dimension)
       :height (:h dimension)})))

(reg-sub
  :location-editor/character-popover
  :<- [:db-locations]
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[locations dialogues characters] [_ location-id tile]]
    (let [placement (get-in locations [location-id :placements tile])
          {:keys [character-id dialogue-id]} placement
          character (characters character-id)]
      (merge {:id character-id
              :dialogue-id dialogue-id
              :dialogue-synopsis (get-in dialogues [dialogue-id :synopsis])}
             (select-keys character [:texture :display-name])))))

(reg-sub
  :location-editor/trigger-popover
  :<- [:db-locations]
  (fn [locations [_ location-id tile]]
    (let [[target-id position] (get-in locations [location-id :connection-triggers tile])
          {:keys [dimension display-name]} (locations target-id)]
      {:id target-id
       :position position
       :position-normalized (relative-point position dimension)
       :display-name display-name})))

(reg-sub
  :location-editor/occupied-tiles
  :<- [:db-locations]
  :<- [:db-player]
  (fn [[locations player] [_ location-id]]
    (let [location (get locations location-id)
          player (when (= location-id (:location-id player))
                   [(:location-position player)])]
      (set (concat (keys (:connection-triggers location))
                   (keys (:placements location))
                   player)))))

(reg-sub
  :location-editor/physically-occupied-tiles
  :<- [:db-locations]
  :<- [:db-dialogues]
  :<- [:db-player]
  (fn [[locations dialogues player] [_ location-id]]
    (let [blocked (get-in locations [location-id :blocked])
          npcs (select [MAP-VALS
                        #(= location-id (:location-id %))
                        :location-position]
                       dialogues)
          player (when (= location-id (:location-id player))
                   [(:location-position player)])]
      (set (concat blocked npcs player)))))
