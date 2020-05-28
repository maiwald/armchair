(ns armchair.db
  (:require [clojure.spec.alpha :as s]
            [clojure.set :refer [subset?]]
            [com.rpl.specter
             :refer [must NONE MAP-VALS]
             :refer-macros [setval]]
            [cognitect.transit :as t]
            [armchair.migrations :refer [db-version migrate]]
            [armchair.config :as config]
            [armchair.math :refer [Point Rect]]
            [armchair.util :as u])
  (:require-macros [armchair.slurp :refer [slurp]]))

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

(s/def :point/x integer?)
(s/def :point/y integer?)
(s/def :type/point
  (s/keys :req-un [:type/x :type/y]))

(s/def :rect/w pos-int?)
(s/def :rect/h pos-int?)
(s/def :type/rect
  (s/keys :req-un [:point/x :point/y :rect/w :rect/h]))

(s/def ::entity-map (s/every (fn [[k v]] (= k (:entity/id v)))))

(s/def :texture/file string?)
(s/def :texture/tile :type/point)
(s/def ::texture
  (s/nilable (s/tuple :texture/file :texture/tile)))

;; UI State

(s/def :ui/connecting (s/or :lines ::connecting-lines
                            :locations ::connecting-locations))
(s/def ::cursor-start :type/point)

(s/def ::connecting-lines (s/keys :req-un [::cursor-start]
                                  :opt-un [::line-id ::dialogue-id ::index]))

(s/def ::connecting-locations (s/keys :req-un [::cursor-start ::location-id]))

(s/def :ui/cursor :type/point)
(s/def :ui/dragging (s/keys :req-un [::cursor-start ::ids]))
(s/def :ui/positions (s/map-of uuid? :type/point))
(s/def :ui/location-map-scroll-center :type/point)
(s/def :ui/location-map-zoom-scale float?)

(s/def :ui/inspector (s/tuple :inspector/type :inspector/data))
(s/def :inspector/type #{:location :tile})
(s/def :inspector/data map?)

(s/def ::current-page (s/nilable string?))

;; Location Editor


(s/def :location-editor/location-editor
  (s/keys :req-un [:location-editor/visible-layers
                   :location-editor/active-layer
                   :location-editor/active-pane
                   :location-editor/active-tool
                   :location-editor/active-walk-state
                   :location-editor/active-texture]))

(s/def :location-editor/active-pane #{:info :level})
(s/def :location-editor/active-tool #{:brush :eraser})
(s/def :location-editor/layers (set (map first config/location-editor-layers)))
(s/def :location-editor/visible-layers (s/coll-of :location-editor/layers :kind set?))
(s/def :location-editor/active-layer :location-editor/layers)
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
          :req-un [:location/bounds
                   ::display-name
                   :location/background1
                   :location/background2
                   :location/foreground1
                   :location/foreground2
                   :location/blocked
                   :location/placements
                   :location/connection-triggers]))

(s/def :location/position :type/point)
(s/def :location/texture-layer (s/map-of :location/position ::texture))

(s/def :location/bounds :type/rect)
(s/def :location/background1 :location/texture-layer)
(s/def :location/background2 :location/texture-layer)
(s/def :location/foreground1 :location/texture-layer)
(s/def :location/foreground1 :location/texture-layer)
(s/def :location/blocked (s/coll-of :location/position :kind set?))

(s/def :location/placements
  (s/map-of :location/position
            :location/placement))

(s/def :location/placement
  (s/keys :req-un [:placement/character-id]
          :opt-un [:placement/dialogue-id]))

(s/def :location/connection-triggers
  (s/map-of :location/position
            :location/connection-trigger-target))

(s/def :location/connection-trigger-target
  (s/tuple :connection-trigger/target-id
           :connection-trigger/target-position))

(s/def :connection-trigger/target-id ::location-id)
(s/def :connection-trigger/target-position :location/position)


(s/def :character/character
  (s/keys :req [:entity/id :entity/type]
          :req-un [::display-name ::color ::texture]))

;; Dialogue & Lines

(s/def ::next-line-id ::line-id)
(s/def :node/kind #{:npc :player :trigger :case})

(defmulti node-type :kind)
(defmethod node-type :npc [_]
  (s/keys :req-un [::text
                   ::character-id]
          :opt-un [::next-line-id]))

(defmethod node-type :player [_]
  (s/keys :req-un [::options]))

(s/def ::options
  (s/coll-of ::player-option-id :kind vector?))

(s/def ::player-options (s/and ::entity-map
                               (s/map-of ::player-option-id ::player-option)))

(s/def ::player-option (s/keys :req [:entity/id :entity/type]
                               :req-un [::text]
                               :opt-un [:player-option/condition
                                        ::next-line-id]))

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
  (s/keys :req-un [::trigger-ids]
          :opt-un [::next-line-id]))

(s/def ::trigger-ids (s/coll-of ::trigger-id :kind vector?))

(s/def :trigger/switch-id :entity/id)
(s/def :trigger/switch-value ::switch-value-id)
(s/def :trigger/trigger
  (s/keys :req [:entity/id
                :entity/type]
          :req-un [:trigger/switch-id
                   :trigger/switch-value]))

(defmethod node-type :case [_]
  (s/keys :req-un [::switch-id
                   :case/clauses]))

(s/def :case/clauses
  (s/map-of ::switch-value-id ::next-line-id))

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
                   :dialogue/synopsis]
          :opt-un [:dialogue/states]))

(s/def :dialogue/states (s/map-of ::line-id ::text))
(s/def :dialogue/initial-line-id ::line-id)

(s/def :switch/switch
  (s/keys :req [:entity/id
                :entity/type]
          :req-un [::display-name
                   :switch/value-ids]
          :req-opt [:switch/default]))

(s/def :switch/default ::switch-value-id)
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
                                     :modal/case-node-creation
                                     :modal/switch-form
                                     :modal/unlock-conditions-form
                                     :modal/connection-trigger-creation
                                     :modal/texture-selection
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
                   :trigger/switch-value]))

(s/def :modal/case-node-creation
  (s/keys :req-un [::dialogue-id
                   :trigger/switch-id]))

(s/def :modal/switch-form
  (s/keys :req-un [::display-name
                   :switch-form/values]
          :opt-un [::switch-id
                   :switch-form/default]))

(s/def :switch-form/values
  (s/coll-of :switch/value
             :min-count 2))

(s/def :switch-form/default
  (s/nilable #(and (int? %) (<= 0 %))))

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

(s/def :modal/texture-selection
  (s/keys :req-un [:texture-selection/file
                   :texture-selection/tile]))

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
                      (s/keys :req [:ui/positions]
                              :req-un [::current-page
                                       :state/player
                                       :state/characters
                                       :state/dialogues
                                       ::player-options
                                       ::lines
                                       :state/triggers
                                       :location-editor/location-editor
                                       :state/locations
                                       :state/switches
                                       ::switch-values]
                              :opt-un [:ui/connecting
                                       :ui/dragging
                                       :ui/dnd
                                       :ui/cursor
                                       :ui/inspector
                                       :ui/location-map-scroll-center
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

(defn delete-node-with-references [db node-id]
  (let [node (get-in db [:lines node-id])
        node-ref? (fn [id] (= id node-id))
        clear-node-ref
        (fn [node]
          (->> node
               (setval [(must :next-line-id) node-ref?] NONE)
               (setval [(must :clauses) MAP-VALS node-ref?] NONE)))]
    (-> (case (:kind node)
          :npc (update-in db [:dialogues (:dialogue-id node)]
                          (fn [dialogue]
                            (if-let [states (dissoc (:states dialogue) node-id)]
                              (assoc dialogue :states states)
                              (dissoc dialogue :states))))
          :trigger (update db :triggers #(apply dissoc % (:trigger-ids node)))
          :player (update db :player-options #(apply dissoc % (:options node)))
          db)
        (update :lines dissoc node-id)
        (update :ui/positions dissoc node-id)
        (u/update-values :lines clear-node-ref)
        (u/update-values :player-options clear-node-ref))))

;; Serialization

(def content-keys
  [:ui/positions
   :player
   :locations
   :characters
   :dialogues
   :player-options
   :lines
   :triggers
   :switches
   :switch-values])

(def undo-keys
  (conj content-keys
        :ui/location-preview-cache-background
        :ui/location-preview-cache-foreground))


(defn content-data [db] (select-keys db content-keys))
(defn undo-data [db] (select-keys db undo-keys))

(defn serialize-db [db]
  (let [w (t/writer :json
                    {:handlers {Point (t/write-handler
                                        (fn [] "point")
                                        (fn [p] #js [(:x p) (:y p)]))
                                Rect (t/write-handler
                                       (fn [] "rect")
                                       (fn [r] #js [(:x r) (:y r)
                                                    (:w r) (:h r)]))}})]
    (t/write w {:version db-version
                :payload (content-data db)})))

(defn deserialize-db [json]
  (let [r (t/reader :json
                    {:handlers {"u" uuid
                                "point" (fn [[x y]] (Point. x y))
                                "rect" (fn [[x y w h]] (Rect. x y w h))}})]
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
                            :active-texture ["PathAndObjects_0.png" (Point. 4 1)]}
          :ui/positions {}
          :ui/location-preview-cache-foreground {}
          :ui/location-preview-cache-background {}
          :ui/location-map-zoom-scale 0.6
          :characters {}
          :locations {}
          :dialogues {}
          :lines {}
          :triggers {}
          :switches {}
          :switch-values {}}
         (migrate (deserialize-db (slurp "resources/dummy_data.json")))))
