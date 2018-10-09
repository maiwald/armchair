(ns armchair.game.subs
  (:require [re-frame.core :refer [reg-sub]]
            [armchair.util :refer [map-keys filter-map where-map rect->0]]))

(reg-sub
  :game/data
  :<- [:db-locations]
  :<- [:db-dialogues]
  :<- [:db-lines]
  :<- [:db-characters]
  :<- [:db-infos]
  (fn [[locations dialogues lines characters infos] _]
    (let [location-id (uuid "121fb127-fbc8-44b9-ba62-2ca2517b6995")
          location (get locations location-id)
          normalize-tile (fn [tile] (rect->0 (:dimension location) tile))
          location-dialogues (where-map :location-id location-id dialogues)]
      {:dimension (:dimension location)
       :npcs (into {} (map (fn [{tile :location-position character-id :character-id}]
                             (let [{id :entity/id
                                    texture :texture} (get characters character-id)]
                               [(normalize-tile tile) {:id id
                                                       :texture texture}]))
                           (vals location-dialogues)))
       :background (map-keys normalize-tile (:background location))
       :walk-set (into #{} (map normalize-tile (:walk-set location)))
       :infos infos
       :lines (filter-map #(location-dialogues (:dialogue-id %)) lines)
       :dialogues (into {} (map (fn [{:keys [character-id initial-line-id]}]
                                  [character-id initial-line-id])
                                (vals location-dialogues)))})))
