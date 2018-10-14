(ns armchair.game.subs
  (:require [clojure.spec.alpha :as s]
            [re-frame.core :refer [reg-sub]]
            [armchair.util :refer [map-keys
                                   map-values
                                   filter-map
                                   where-map
                                   rect->0]]))

(defn line-screen [lines line]
  {:pre [(= (:kind line) :npc)]}
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


(s/def :game/data (s/keys :req-un [:game/infos :game/lines :game/locations]))
(s/def :game/lines (s/map-of :entity/id (s/keys :req-un [:game/text :game/info-ids :game/options])))
(s/def :game/options (s/coll-of (s/keys :req-un [:game/text :game/next-line-id]) :kind vector?))
(s/def :game/locations (s/map-of :entity/id (s/keys :req-un [:game/dimension
                                                             :game/background
                                                             :game/walk-set
                                                             :game/connection-triggers
                                                             :game/npcs])))
(s/def :game/dimension :type/rect)
(s/def :game/walk-set (s/coll-of :type/point :kind set?))
(s/def :game/connection-triggers (s/map-of :type/point (s/keys :req-un [:game/location-id :game/position])))
(s/def :game/npcs (s/map-of :type/point (s/keys :req-un [:game/npc-texture :game/initial-line-id])))
(s/def :game/initial-line-id :entity/id)
(s/def :game/next-line-id (s/nilable :entity/id))

(reg-sub
  :game/data
  :<- [:db-locations]
  :<- [:db-dialogues]
  :<- [:db-lines]
  :<- [:db-characters]
  :<- [:db-infos]
  (fn [[locations dialogues lines characters infos] _]
    {:post [(or (s/valid? :game/data %)
                (s/explain :game/data %))]}
    (let [dialogues-by-location (->> dialogues vals (group-by :location-id))]
      {:infos infos
       :lines (map-values (partial line-screen lines)
                          (->> lines (where-map :kind :npc)))
       :locations (map-values (fn [{:keys [dimension background connection-triggers walk-set]
                                    id :entity/id
                                    :as location}]
                                (letfn [(normalize-tile [tile]
                                          (rect->0 dimension tile))]
                                  {:dimension dimension
                                   :background (map-keys normalize-tile background)
                                   :connection-triggers (->> connection-triggers
                                                          (map (fn [[pos target-loctation-id]]
                                                                 (let [target-dimension (get-in locations [target-loctation-id :dimension])]
                                                                   [(normalize-tile pos) {:location-id target-loctation-id
                                                                                          :position (->> (get locations target-loctation-id)
                                                                                                         :connection-triggers
                                                                                                         (filter-map #(= id %))
                                                                                                         keys
                                                                                                         (map #(rect->0 target-dimension %))
                                                                                                         first)}])))
                                                          (into {}))
                                   :walk-set (set (map normalize-tile walk-set))
                                   :npcs (->> (dialogues-by-location id)
                                           (map (fn [{line-id :initial-line-id
                                                      :keys [location-position character-id]}]
                                                  [(normalize-tile location-position)
                                                   {:npc-texture (get-in characters [character-id :texture])
                                                    :initial-line-id line-id}]))
                                           (into {}))}))
                              locations)})))
