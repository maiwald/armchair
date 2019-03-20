(ns armchair.game.subs
  (:require [clojure.spec.alpha :as s]
            [re-frame.core :refer [reg-sub]]
            [com.rpl.specter
             :refer [must ALL NONE MAP-VALS]
             :refer-macros [select transform]]
            [armchair.textures :refer [character-textures]]
            [armchair.util :as u]))

(s/def :game/data (s/keys :req-un [:game/lines
                                   :game/locations
                                   :game/initial-state]))
(s/def :game/initial-state :armchair.game.core/state)
(s/def :game/lines (s/map-of :entity/id (s/keys :req-un [:game/text :game/options])))
(s/def :game/options (s/coll-of (s/keys :req-un [:game/text :game/next-line-id])
                                :kind vector?))
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

(defn triggers-to-map [triggers]
  (into {} (for [{:keys [switch-id switch-value]} triggers]
             [switch-id switch-value])))

(reg-sub
  :game/lines-with-triggers
  :<- [:db-lines]
  :<- [:db-triggers]
  (fn [[lines triggers]]
    (transform [MAP-VALS
                #(= :npc (:kind %))
                #(= :trigger (get-in lines [(:next-line-id %) :kind]))]
               (fn [line]
                 (let [trigger (get lines (:next-line-id line))
                       triggers (->> (:trigger-ids trigger)
                                     (map triggers)
                                     (group-by :switch-kind)
                                     (u/map-values triggers-to-map))]
                   (assoc line
                          :next-line-id (:next-line-id trigger)
                          :triggers {:dialogue-states (:dialogue-state triggers)
                                     :switches (:switch triggers)})))
               lines)))

(reg-sub
  :game/player-options-with-triggers
  :<- [:db-player-options]
  :<- [:db-lines]
  :<- [:db-triggers]
  (fn [[player-options lines triggers]]
    (transform [MAP-VALS
                #(= :trigger (get-in lines [(:next-line-id %) :kind]))]
               (fn [player-option]
                 (let [trigger (get lines (:next-line-id player-option))
                       triggers (->> (:trigger-ids trigger)
                                     (map triggers)
                                     (group-by :switch-kind)
                                     (u/map-values triggers-to-map))]
                   (assoc player-option
                          :next-line-id (:next-line-id trigger)
                          :triggers {:dialogue-states (:dialogue-state triggers)
                                     :switches (:switch triggers)})))
               player-options)))


(reg-sub
  :game/line-data
  :<- [:game/lines-with-triggers]
  :<- [:game/player-options-with-triggers]
  (fn [[lines player-options]]
    (u/map-values
      (fn [{:keys [triggers text next-line-id]}]
        {:text text
         :triggers triggers
         :options (if-let [next-line (get lines next-line-id)]
                    (case (:kind next-line)
                      :npc (vector {:text "Continue..."
                                    :next-line-id (:entity/id next-line)})
                      :player (mapv player-options (:options next-line)))
                    (vector {:text "Yeah..., whatever. Farewell"
                             :next-line-id nil}))})
      (u/where-map :kind :npc lines))))

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
    (u/map-values
      (fn [{:keys [dimension background connection-triggers walk-set
                   id :entity/id]}]
        {:dimension dimension
         :background background
         :outbound-connections connection-triggers
         :inbound-connections (u/reverse-map connection-triggers)
         :walk-set walk-set
         :npcs (npcs-by-location id)})
      locations)))

(reg-sub
  :game/data
  :<- [:game/player-data]
  :<- [:game/locations]
  :<- [:game/line-data]
  :<- [:db-dialogues]
  :<- [:db-switches]
  (fn [[player-data locations line-data dialogues switches] _]
    {:post [(or (s/valid? :game/data %)
                (s/explain :game/data %))]}
    {:lines line-data
     :locations locations
     :initial-state {:dialogue-states (u/map-values :initial-line-id dialogues)
                     :switches (u/map-values (constantly nil) switches)
                     :player (merge player-data {:direction :right})}}))
