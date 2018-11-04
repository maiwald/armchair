(ns armchair.game.subs
  (:require [clojure.spec.alpha :as s]
            [re-frame.core :refer [reg-sub]]
            [armchair.util :refer [map-keys
                                   map-values
                                   filter-map
                                   where-map
                                   rect->0]]))

(s/def :game/data (s/keys :req-un [:game/infos :game/lines :game/locations]))
(s/def :game/lines (s/map-of :entity/id (s/keys :req-un [:game/text :game/info-ids :game/options])))
(s/def :game/options (s/coll-of (s/keys :req-un [:game/text :game/next-line-id]) :kind vector?))
(s/def :game/locations (s/map-of :entity/id (s/keys :req-un [:game/dimension
                                                             :game/background
                                                             :game/walk-set
                                                             :game/outbound-connections
                                                             :game/inbound-connections
                                                             :game/npcs])))
(s/def :game/dimension :type/rect)
(s/def :game/walk-set (s/coll-of :type/point :kind set?))
(s/def :game/outbound-connections (s/map-of :type/point :game/location-id))
(s/def :game/inbound-connections (s/map-of :game/location-id :type/point))
(s/def :game/location-id :entity/id)
(s/def :game/position :type/point)
(s/def :game/npcs (s/map-of :type/point (s/keys :req-un [:game/npc-texture :game/initial-line-id])))
(s/def :game/initial-line-id :entity/id)
(s/def :game/next-line-id (s/nilable :entity/id))


(reg-sub
  :game/dialogues-by-location
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[dialogues characters] _]
    (->> (vals dialogues)
         (group-by :location-id)
         (map-values #(into {} (map (fn [{line-id :initial-line-id
                                          :keys [location-position character-id]}]
                                      [location-position
                                       {:npc-texture (get-in characters [character-id :texture])
                                        :initial-line-id line-id}])
                                    %))))))

(reg-sub
  :game/line-data
  :<- [:db-lines]
  (fn [lines]
    (map-values (fn [line]
                  (let [next-line (get lines (:next-line-id line))]
                    {:text (:text line)
                     :info-ids (:info-ids line)
                     :options (if-let [next-line (get lines (:next-line-id line))]
                                (case (:kind next-line)
                                  :npc (vector {:text "Continue..."
                                                :next-line-id (:next-line-id next-line)})
                                  :player (:options next-line))
                                (vector {:text "Yeah..., whatever. Farewell"
                                         :next-line-id nil}))}))
                (where-map :kind :npc lines))))

(defn reverse-map [m]
  (into {} (map (fn [[k v]] [v k]) m)))

(reg-sub
  :game/locations
  :<- [:db-locations]
  :<- [:game/dialogues-by-location]
  (fn [[locations dialogues-by-location]]
    (map-values (fn [{:keys [dimension background connection-triggers walk-set]
                      id :entity/id}]
                  (let [normalize-tile (fn [tile] (rect->0 dimension tile))
                        outbound-connections (map-keys normalize-tile connection-triggers)]
                    {:dimension dimension
                     :background (map-keys normalize-tile background)
                     :outbound-connections outbound-connections
                     :inbound-connections (reverse-map outbound-connections)
                     :walk-set (set (map normalize-tile walk-set))
                     :npcs (map-keys normalize-tile (dialogues-by-location id))}))
                locations)))

(reg-sub
  :game/data
  :<- [:game/locations]
  :<- [:game/line-data]
  :<- [:db-infos]
  (fn [[locations line-data infos] _]
    {:post [(or (s/valid? :game/data %)
                (s/explain :game/data %))]}
    {:infos infos
     :lines line-data
     :locations locations}))
