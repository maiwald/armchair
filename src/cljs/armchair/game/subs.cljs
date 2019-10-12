(ns armchair.game.subs
  (:require [clojure.spec.alpha :as s]
            [clojure.set :refer [rename-keys]]
            [re-frame.core :refer [reg-sub]]
            [com.rpl.specter
             :refer [must ALL NONE MAP-VALS]
             :refer-macros [select setval transform]]
            [armchair.config :as config]
            [armchair.textures :refer [character-textures]]
            [armchair.math :as m]
            [armchair.util :as u]))

(s/def :game/data (s/keys :req-un [:game/lines
                                   :game/locations
                                   :game/initial-state]))
(s/def :game/initial-state :armchair.game.core/state)

(defmulti node-type :kind)
(defmethod node-type :npc [_]
  (s/keys :req-un [:game/text]
          :opt-un [:game/next-line-id]))

(defmethod node-type :player [_]
  (s/keys :req-un [:game/options]))

(s/def :game/options
  (s/coll-of (s/keys :req-un [:game/text]
                     :opt-un [:game/condition
                              :game/next-line-id])
             :kind vector?))

(s/def :game/condition fn?)

(defmethod node-type :trigger [_]
  (s/keys :req-un [:game/trigger-changes]
          :opt-un [:game/next-line-id]))

(s/def :game/trigger-changes
  (s/map-of :entity/id :entity/id))

(defmethod node-type :case [_]
  (s/keys :req-un [:game/next-line-id-fn]))

(s/def :game/next-line-id-fn fn?)

(s/def :game/lines
  (s/map-of :entity/id
            (s/multi-spec node-type :kind)))

(s/def :game/locations (s/map-of :entity/id (s/keys :req-un [:game/dimension
                                                             :game/background1
                                                             :game/background2
                                                             :game/foreground1
                                                             :game/foreground1
                                                             :game/blocked
                                                             :game/outbound-connections
                                                             :game/characters])))
(s/def :game/dimension :type/rect)
(s/def :game/blocked (s/coll-of :type/point :kind set?))
(s/def :game/outbound-connections
  (s/map-of :type/point (s/tuple :game/location-id :type/point)))
(s/def :game/location-id :entity/id)
(s/def :game/position :type/point)
(s/def :game/characters
  (s/nilable (s/map-of :type/point
                       (s/keys :req-un [:game/texture :game/dialogue-id]))))
(s/def :game/dialogue-id (s/nilable :entity/id))
(s/def :game/texture (fn [t] (contains? (set character-textures) t)))
(s/def :game/next-line-id (s/nilable :entity/id))


(defn triggers-to-map [triggers]
  (into {} (for [{:keys [switch-id switch-value]} triggers]
             [switch-id switch-value])))

(defn condition->fn [{:keys [conjunction terms]}]
  (fn [switches]
    ((-> conjunction config/condition-conjunctions :func)
     (fn [{:keys [switch-id operator switch-value-id]}]
       ((-> operator config/condition-operators :func)
        (switches switch-id) switch-value-id))
     terms)))

(defn triggers->changes-map [trigger-ids triggers]
  (into {} (map
             #(let [t (triggers %)]
                [(:switch-id t) (:switch-value t)])
             trigger-ids)))

(defn clauses->fn [clauses switch-id]
  (fn [switches]
    (get clauses (switches switch-id))))

(reg-sub
  :game/line-data
  :<- [:db-lines]
  :<- [:db-player-options]
  :<- [:db-triggers]
  (fn [[lines player-options triggers]]
    (u/map-values
      (fn [{:keys [kind] :as line}]
        (case kind
          :player (->> line
                       (transform [:options ALL] player-options)
                       (transform [:options ALL (must :condition)] condition->fn))
          :trigger (assoc line :trigger-changes
                          (triggers->changes-map (:trigger-ids line) triggers))
          :case (assoc line :next-line-id-fn
                       (clauses->fn (:clauses line) (:switch-id line)))
          line))
      lines)))

(reg-sub
  :game/player-data
  :<- [:db-player]
  (fn [{:keys [location-id location-position]}]
    {:location-id location-id
     :position location-position}))

(reg-sub
  :game/locations
  :<- [:db-locations]
  :<- [:db-characters]
  :<- [:db-dialogues]
  (fn [[locations characters dialogues]]
    (u/map-values
      (fn [location]
        (-> location
            (select-keys [:dimension
                          :background1
                          :background2
                          :foreground1
                          :foreground2
                          :blocked])
            (assoc :outbound-connections (:connection-triggers location)
                   :characters (->> (:placements location)
                                    (u/map-values
                                      (fn [{:keys [character-id dialogue-id]}]
                                        {:texture (get-in characters [character-id :texture])
                                         :dialogue-id dialogue-id}))))))
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
                (js/console.log (s/explain-data :game/data %)))]}
    {:lines line-data
     :locations locations
     :initial-state {:dialogue-states (u/map-values :initial-line-id dialogues)
                     :switches (u/map-values :default switches)
                     :player (merge player-data {:direction :down})
                     :enemies {(uuid "8594a767-8036-409d-81eb-104c799cf26e")
                               (list {:texture :orc_knight :position (m/Point. 0 1)}
                                     {:texture :orc_warrior :position (m/Point. 3 2)})}}}))
