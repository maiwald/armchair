(ns armchair.game.subs
  (:require [clojure.spec.alpha :as s]
            [re-frame.core :refer [reg-sub]]
            [armchair.textures :refer [character-textures]]
            [armchair.util :refer [map-keys
                                   map-values
                                   filter-map
                                   where-map]]))

(s/def :game/data (s/keys :req-un [:game/infos
                                   :game/lines
                                   :game/locations
                                   :game/initial-state]))
(s/def :game/initial-state :armchair.game.core/state)
(s/def :game/lines (s/map-of :entity/id (s/keys :req-un [:game/text :game/options]
                                                :opt-un [:game/info-ids :game/state-triggers])))
(s/def :game/options (s/coll-of (s/keys :req-un [:game/text :game/next-line-id]
                                        :opt-un [:game/state-triggers])
                                :kind vector?))
(s/def :game/state-triggers (s/map-of :entity/id :entity/id))
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
(s/def :game/npcs (s/nilable (s/map-of :type/point (s/keys :req-un [:game/texture :game/dialogue-id]))))
(s/def :game/dialogue-id :entity/id)
(s/def :game/texture (fn [t] (contains? (set character-textures) t)))
(s/def :game/next-line-id (s/nilable :entity/id))


(reg-sub
  :game/npcs-by-location
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[dialogues characters] _]
    (reduce
      (fn [acc [id {:keys [location-id location-position character-id]}]]
        (assoc-in acc
                  [location-id location-position]
                  {:texture (get-in characters [character-id :texture])
                   :dialogue-id id}))
      {}
      dialogues)))

(reg-sub
  :game/line-data
  :<- [:db-lines]
  (fn [lines]
    (letfn [(transform-state-triggers [triggers]
              (into {} (map (fn [trigger]
                              [(get-in lines [trigger :dialogue-id]) trigger])
                            triggers)))]
      (map-values (fn [line]
                    (-> (select-keys line [:text :info-ids :state-triggers])
                        (update :state-triggers transform-state-triggers)
                        (merge {:options (if-let [next-line (get lines (:next-line-id line))]
                                           (case (:kind next-line)
                                             :npc (vector {:text "Continue..."
                                                           :next-line-id (:entity/id next-line)})
                                             :player (mapv (fn [option]
                                                             (update option :state-triggers transform-state-triggers))
                                                           (:options next-line)))
                                           (vector {:text "Yeah..., whatever. Farewell"
                                                    :next-line-id nil}))})))
        (where-map :kind :npc lines)))))

(defn reverse-map [m]
  (into {} (map (fn [[k v]] [v k]) m)))

(reg-sub
  :game/player-data
  :<- [:db-player]
  :<- [:db-locations]
  (fn [[{:keys [location-id location-position]} locations]]
    (let [dimension (get-in locations [location-id :dimension])]
      {:location-id location-id
       :position location-position})))

(reg-sub
  :game/locations
  :<- [:db-locations]
  :<- [:game/npcs-by-location]
  (fn [[locations npcs-by-location]]
    (map-values (fn [{:keys [dimension background connection-triggers walk-set]
                      id :entity/id}]
                  {:dimension dimension
                   :background background
                   :outbound-connections connection-triggers
                   :inbound-connections (reverse-map connection-triggers)
                   :walk-set walk-set
                   :npcs (npcs-by-location id)})
                locations)))

(reg-sub
  :game/data
  :<- [:game/player-data]
  :<- [:game/locations]
  :<- [:game/line-data]
  :<- [:db-dialogues]
  :<- [:db-infos]
  (fn [[player-data locations line-data dialogues infos] _]
    {:post [(or (s/valid? :game/data %)
                (s/explain :game/data %))]}
    {:infos infos
     :lines line-data
     :locations locations
     :initial-state {:dialogue-states (map-values :initial-line-id dialogues)
                     :player (merge player-data {:direction :right :infos #{}})}}))
