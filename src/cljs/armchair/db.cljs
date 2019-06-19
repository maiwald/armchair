(ns armchair.db
  (:require [clojure.spec.alpha :as s]
            [clojure.set :refer [subset?]]
            [com.rpl.specter
             :refer [must ALL NONE MAP-VALS]
             :refer-macros [setval]]
            [cognitect.transit :as t]
            [armchair.dummy-data :refer [dummy-data]]
            [armchair.textures :refer [background-textures texture-set]]
            [armchair.config :as config]
            [armchair.util :as u]))

(def db-version 8)

;; Types

(s/def :entity/id uuid?)
(s/def :entity/type #{:location
                      :character
                      :line
                      :dialogue
                      :player-option
                      :trigger
                      :switch
                      :switch-value})
(s/def ::text string?)
(s/def :type/point (s/tuple integer? integer?))
(s/def :type/rect (s/and (s/tuple :type/point :type/point)
                     (fn [[[x1 y1] [x2 y2]]] (and (< x1 x2)
                                                  (< y1 y2)))))
(s/def ::entity-map (s/every (fn [[k v]] (= k (:entity/id v)))))
(s/def ::texture (s/nilable #(contains? texture-set %)))

(s/def :ui/cursor :type/point)

;; UI State

(s/def ::cursor-start :type/point)
(s/def ::connecting-lines (s/keys :req-un [::cursor-start ::line-id]
                                  :opt-un [::index]))
(s/def ::connecting-locations (s/keys :req-un [::cursor-start ::location-id]))
(s/def ::connecting (s/or :lines ::connecting-lines
                          :locations ::connecting-locations))

(s/def ::dragging (s/keys :req-un [::cursor-start ::ids]))
(s/def ::current-page (s/nilable string?))

;; Location Editor


(s/def ::location-editor
  (s/keys :req-un [:location-editor/visible-layers
                   :location-editor/active-layer
                   :location-editor/active-pane
                   :location-editor/active-tool
                   :location-editor/active-walk-state
                   :location-editor/active-texture]
          :opt-un [:location-editor/highlight]))

(s/def :location-editor/active-pane #{:info :level})
(s/def :location-editor/active-tool #{:brush :eraser})
(s/def :location-editor/layers (set (map first config/location-editor-layers)))
(s/def :location-editor/visible-layers (s/coll-of :location-editor/layers :kind set?))
(s/def :location-editor/active-layer :location-editor/layers)
(s/def :location-editor/highlight :type/point)
(s/def :location-editor/active-texture ::texture)
(s/def :location-editor/active-walk-state boolean?)

;; Data

(s/def ::character-id :entity/id)
(s/def ::dialogue-id :entity/id)
(s/def ::line-id :entity/id)
(s/def ::location-id :entity/id)
(s/def ::player-option-id :entity/id)
(s/def ::trigger-id :entity/id)
(s/def ::switch-id :entity/id)
(s/def ::switch-value-id :entity/id)

(s/def ::display-name ::text)
(s/def ::color ::text)
(s/def ::description ::text)

(s/def :location/location
  (s/keys :req [:entity/id
                :entity/type]
          :req-un [:location/dimension
                   ::display-name
                   :location/background1
                   :location/background2
                   :location/foreground1
                   :location/foreground2
                   :location/blocked
                   :location/connection-triggers]))

(s/def :location/position :type/point)
(s/def :location/texture-layer (s/map-of :type/point ::texture))

(s/def :location/dimension :type/rect)
(s/def :location/background1 :location/texture-layer)
(s/def :location/background2 :location/texture-layer)
(s/def :location/foreground1 :location/texture-layer)
(s/def :location/foreground1 :location/texture-layer)
(s/def :location/blocked (s/coll-of :type/point :kind set?))
(s/def :location/connection-triggers
  (s/map-of :type/point :location/connection-trigger-target))

(s/def :location/connection-trigger-target
  (s/tuple :connection-trigger/target-id
           :connection-trigger/target-position))

(s/def :connection-trigger/target-id ::location-id)
(s/def :connection-trigger/target-position :location/position)


(s/def :character/character
  (s/keys :req [:entity/id :entity/type]
          :req-un [::display-name ::color ::texture]))

;; Dialogue & Lines

(s/def ::next-line-id (s/or :line-id (s/nilable ::line-id)))
(s/def :node/kind keyword?)

(defmulti node-type :kind)
(defmethod node-type :npc [_]
  (s/keys :req-un [::text
                   ::next-line-id
                   ::character-id]))

(defmethod node-type :player [_]
  (s/keys :req-un [::options]))

(s/def ::options (s/coll-of ::player-option-id :kind vector?))


(s/def ::player-options (s/and ::entity-map
                               (s/map-of ::player-option-id ::player-option)))

(s/def ::player-option (s/keys :req [:entity/id :entity/type]
                               :req-un [::text ::next-line-id]
                               :opt-un [:player-option/condition]))

(s/def :player-option/condition
  (s/keys :req-un [:condition/conjunction
                   :condition/terms]))

(s/def :condition/conjunction
  (-> config/condition-conjunctions keys set))

(s/def :condition/terms
  (s/coll-of :condition/term
             :kind vector?))

(s/def :condition/term
  (s/keys :req-un [::switch-id
                   :condition/operator
                   ::switch-value-id]))

(s/def :condition/operator
  (-> config/condition-operators keys set))

(defmethod node-type :trigger [_]
  (s/keys :req-un [::trigger-ids ::next-line-id]))

(s/def ::trigger-ids (s/coll-of ::trigger-id :kind vector?))

(s/def :trigger/switch-kind #{:dialogue-state :switch})
(s/def :trigger/switch-id :entity/id)
(s/def :trigger/switch-value ::switch-value-id)
(s/def :trigger/trigger
  (s/keys :req [:entity/id
                :entity/type]
          :req-un [:trigger/switch-kind
                   :trigger/switch-id
                   :trigger/switch-value]))

(s/def :node/node
  (s/and (s/keys :req [:entity/id
                       :entity/type]
                 :req-un [::dialogue-id
                          :node/kind])
         (s/multi-spec node-type :kind)))

(s/def ::lines (s/and ::entity-map
                      (s/map-of ::line-id :node/node)))

(s/def ::location-position :location/position)
(s/def :dialogue/dialogue
  (s/keys :req [:entity/id
                :entity/type]
          :req-un [::character-id
                   :dialogue/initial-line-id
                   ::location-id
                   ::location-position
                   :dialogue/synopsis]
          :opt-un [:dialogue/states]))

(s/def :dialogue/states (s/map-of ::line-id ::text))
(s/def :dialogue/initial-line-id ::line-id)

(s/def :switch/switch
  (s/keys :req [:entity/id
                :entity/type]
          :req-un [::display-name
                   :switch/value-ids]))

(s/def :switch/value-ids (s/coll-of ::switch-value-id
                                    :kind vector?
                                    :min-count 2))

(s/def ::switch-values (s/and ::entity-map
                              (s/map-of ::switch-value-id :switch/value)))

(s/def :switch/value (s/keys :req [:entity/id
                                   :entity/type]
                             :req-un [::display-name]))

;; Modals

(s/def ::npc-line-id ::line-id)
(s/def :modal/modal (s/keys :opt-un [:modal/character-form
                                     :modal/dialogue-creation
                                     :modal/location-creation
                                     :modal/trigger-creation
                                     :modal/switch-form
                                     :modal/unlock-conditions-form
                                     :modal/connection-trigger-creation
                                     ::npc-line-id
                                     ::dialogue-state]))

(s/def :modal/location-creation ::display-name)

(s/def :modal/character-form
  (s/keys :req-un [::display-name ::color ::texture]
          :opt-un [:entity/id]))

(s/def :modal/dialogue-creation
  (s/keys :req-un [::character-id :dialogue/synopsis]
          :opt-un [::location-id ::location-position]))

(s/def :modal/trigger-creation
  (s/keys :req-un [::trigger-node-id
                   :trigger/switch-id
                   :trigger/switch-kind
                   :trigger/switch-value]))

(s/def :modal/switch-form
  (s/keys :req-un [::display-name
                   :switch-form/values]
          :opt-un [::switch-id]))

(s/def :switch-form/values
  (s/coll-of :switch/value
             :min-count 2))

(s/def :modal/unlock-conditions-form
  (s/keys :req-un [::player-option-id
                   :condition/terms
                   :condition/conjunction]))

(s/def :modal/connection-trigger-creation
  (s/keys :req-un [::location-id
                   ::location-position
                   :connection-trigger/target-id
                   :connection-trigger/target-position]))

(s/def ::dialogue-state
  (s/keys :req-un [::line-id]
          :opt-un [::description]))

;; Invariants

(s/def ::location-connection-validation
  (fn [state] (subset? (reduce into #{} (:location-connections state))
                       (-> state :locations keys set))))

(s/def ::dialogue-must-start-with-npc-line
  (fn [{:keys [dialogues lines]}]
    (every? #(= :npc (:kind %))
            (select-keys lines (map :initial-line-id dialogues)))))

(s/def ::player-options-must-not-point-to-player-line
  (fn [{options :player-options lines :lines}]
    (let [player-lines (->> lines (u/where-map :kind :player))
          option-target-ids (map :next-line-id (vals options))]
      (not-any? #(contains? player-lines %) option-target-ids))))

(s/def ::state (s/and ::dialogue-must-start-with-npc-line
                      ::player-options-must-not-point-to-player-line
                      ::location-connection-validation
                      (s/keys :req-un [::current-page
                                       :state/player
                                       :state/characters
                                       :state/dialogues
                                       ::player-options
                                       ::lines
                                       :state/triggers
                                       ::location-editor
                                       :state/locations
                                       :state/switches
                                       ::switch-values]
                              :opt-un [::connecting
                                       ::dragging
                                       ::cursor
                                       :modal/modal])))

(s/def :state/player
  (s/keys :req-un [::location-id
                   ::location-position]))

(s/def :state/dialogues
  (s/and ::entity-map
         (s/map-of ::dialogue-id :dialogue/dialogue)))

(s/def :state/switches
  (s/and ::entity-map
         (s/map-of ::switch-id :switch/switch)))

(s/def :state/locations
  (s/and ::entity-map
         (s/map-of ::location-id :location/location)))

(s/def :state/characters
  (s/and ::entity-map
         (s/map-of ::character-id :character/character)))

(s/def :state/triggers
  (s/and ::entity-map
         (s/map-of ::trigger-id :trigger/trigger)))

(defn clear-dialogue-state [db line-id]
  (let [dialogue-id (get-in db [:lines line-id :dialogue-id])
        trigger-ids (->> (:triggers db)
                         (u/filter-map (fn [{v :switch-value
                                             kind :switch-kind}]
                                         (and (= kind :dialogue-state)
                                              (= v line-id))))
                         keys
                         set)]
    (-> (setval [:lines MAP-VALS (must :trigger-ids) ALL #(contains? trigger-ids %)]
                NONE db)
        (update-in [:dialogues dialogue-id :states] dissoc line-id)
        (update :triggers #(apply dissoc % trigger-ids)))))

;; Serialization

(defn content-data [db]
  (let [content-keys [:ui/positions
                      :player
                      :locations
                      :characters
                      :dialogues
                      :player-options
                      :lines
                      :triggers
                      :switches
                      :switch-values]]
    (select-keys db content-keys)))

(defn serialize-db [db]
  (let [w (t/writer :json)]
    (t/write w {:version db-version
                :payload (content-data db)})))

(defn deserialize-db [json]
  (let [r (t/reader :json {:handlers {"u" uuid}})]
    (t/read r json)))

;; Default DB

(def default-db
  (merge {:location-editor {:active-pane :info
                            :active-tool :brush
                            :active-layer :background1
                            :visible-layers #{:background1
                                              :background2
                                              :foreground1
                                              :foreground2
                                              :entities
                                              :triggers}
                            :active-walk-state true
                            :active-texture (first background-textures)}
          :ui/positions {}
          :characters {}
          :locations {}
          :dialogues {}
          :lines {}
          :triggers {}
          :switches {}
          :switch-values {}}
         dummy-data))
