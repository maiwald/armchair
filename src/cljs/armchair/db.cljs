(ns armchair.db
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.set :refer [union subset?]]
            [cognitect.transit :as t]
            [armchair.dummy-data :refer [dummy-data]]
            [armchair.textures :refer [background-textures texture-set]]
            [armchair.config :as config]
            [armchair.util :refer [where
                                   filter-map
                                   where-map
                                   map-values
                                   update-in-map
                                   translate-point]]))

(def db-version 4)

(def migrations
  "Map of migrations. Key is the version we are coming from."
  {1 (fn [db]
       (-> db
           (assoc :player {:location-id (-> db :locations keys first)
                           :location-position [0 0]})
           (update :dialogues (fn [ds]
                                (map-values
                                  (fn [d]
                                    (-> d
                                        (assoc :synopsis (:description d))
                                        (dissoc :description)))
                                  ds)))))

   2 (fn [db]
       "Extract player options from lines"
       (let [player-lines (where-map :kind :player (:lines db))]
         (reduce (fn [new-db [line-id {:keys [options] :as line}]]
                   (let [new-options (mapv #(assoc %
                                                   :entity/id (random-uuid)
                                                   :entity/type :player-option)
                                           options)
                         option-ids (mapv :entity/id new-options)]
                     (-> new-db
                         (update :player-options merge (zipmap option-ids new-options))
                         (assoc-in [:lines line-id :options] option-ids))))
                 db
                 player-lines)))

   3 (fn [db]
       "Remove inline state trigger and information concepts"
       (-> db
           (dissoc :infos)
           (update :lines
                   (fn [lines]
                     (map-values
                       #(dissoc % :state-triggers :info-ids)
                       lines)))
           (update :player-options
                   (fn [player-options]
                     (map-values
                       #(dissoc % :state-triggers :required-info-ids)
                       player-options)))))})

(defn migrate [{:keys [version payload]}]
  (assert (<= version db-version)
          (str "Save file version is invalid: " version ", current: " db-version))
  (loop [version version
         payload payload]
    (if (= version db-version)
      payload
      (recur
        (inc version)
        ((get migrations version) payload)))))

(defn content-data [db]
  (let [content-keys [:ui/positions
                      :player
                      :locations
                      :characters
                      :dialogues
                      :player-options
                      :location-connections
                      :lines
                      :triggers]]
    (select-keys db content-keys)))

(defn serialize-db [db]
  (let [w (t/writer :json)]
    (t/write w {:version db-version
                :payload (content-data db)})))

(defn deserialize-db [json]
  (let [r (t/reader :json {:handlers {"u" uuid}})]
    (t/read r json)))

;; Types

(s/def :entity/id uuid?)
(s/def :entity/type #{:location :character :line :dialogue :player-option :trigger})
(s/def ::text #(not (string/blank? %)))
(s/def :type/point (s/tuple integer? integer?))
(s/def :type/rect (s/and (s/tuple :type/point :type/point)
                     (fn [[[x1 y1] [x2 y2]]] (and (< x1 x2)
                                                  (< y1 y2)))))
(s/def ::entity-map (s/every (fn [[k v]] (= k (:entity/id v)))))
(s/def ::undirected-connection (s/coll-of :entity/id :kind set? :count 2))
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

(s/def ::tool #{:info
                :npcs-select
                :resize
                :collision
                :background-painter
                :connection-select})
(s/def ::highlight :type/point)
(s/def ::active-texture ::texture)
(s/def ::active-walk-state boolean?)
(s/def ::location-editor (s/keys :req-un [::tool
                                          ::active-walk-state
                                          ::active-texture]
                                 :opt-un [::highlight]))

;; Data

(s/def ::character-id :entity/id)
(s/def ::dialogue-id :entity/id)
(s/def ::line-id :entity/id)
(s/def ::location-id :entity/id)
(s/def ::player-option-id :entity/id)
(s/def ::trigger-id :entity/id)

(s/def ::display-name ::text)
(s/def ::color ::text)
(s/def ::description ::text)

(s/def ::dimension :type/rect)
(s/def ::background (s/map-of :type/point ::texture))
(s/def ::walk-set (s/coll-of :type/point :kind set?))
(s/def ::connection-triggers (s/map-of :type/point ::location-id))
(s/def ::location (s/keys :req [:entity/id
                                :entity/type]
                          :req-un [::dimension
                                   ::display-name
                                   ::background
                                   ::walk-set
                                   ::connection-triggers]))

(s/def ::locations (s/and ::entity-map
                          (s/map-of ::location-id ::location)))
(s/def ::location-connections (s/coll-of ::undirected-connection :kind set?))

(s/def ::character (s/keys :req [:entity/id :entity/type]
                           :req-un [::display-name ::color ::texture]))
(s/def ::characters (s/and ::entity-map
                           (s/map-of ::character-id ::character)))

;; Dialogue & Lines

(s/def ::next-line-id (s/or :line-id (s/nilable ::line-id)))

(s/def ::npc-line (s/and #(= (:kind %) :npc)
                         (s/keys :req-un [::text
                                          ::next-line-id
                                          ::character-id])))

(s/def ::player-line (s/and #(= (:kind %) :player)
                            (s/keys :req-un [::options])))
(s/def ::options (s/coll-of ::player-option-id :kind vector?))

(s/def ::player-options (s/and ::entity-map
                               (s/map-of ::player-option-id ::player-option)))

(s/def ::player-option (s/keys :req [:entity/id :entity/type]
                               :req-un [::text ::next-line-id]))


(s/def ::trigger-node (s/and #(= (:kind %) :trigger)
                             (s/keys :req-un [::trigger-ids ::next-line-id])))
(s/def ::trigger-ids (s/coll-of ::trigger-id :kind vector?))

(s/def ::triggers (s/and ::entity-map
                         (s/map-of ::trigger-id ::trigger)))

(s/def :trigger/switch-kind #{:dialogue-state})
(s/def :trigger/switch-id :entity/id)
(s/def :trigger/switch-value :entity/id)
(s/def ::trigger (s/keys :req [:entity/id
                               :entity/type]
                         :req-un [:trigger/switch-kind
                                  :trigger/switch-id
                                  :trigger/switch-value]))

(s/def :dialogue-node/kind #{:npc :player :trigger})
(s/def ::npc-or-player-line (s/and (s/keys :req [:entity/id
                                                 :entity/type]
                                           :req-un [::dialogue-id
                                                    :dialogue-node/kind])
                                   (s/or :npc ::npc-line
                                         :player ::player-line
                                         :trigger ::trigger-node)))

(s/def ::lines (s/and ::entity-map
                      (s/map-of ::line-id ::npc-or-player-line)))
(s/def ::location-position :type/point)

(s/def ::dialogue (s/keys :req [:entity/id
                                :entity/type]
                          :req-un [::character-id
                                   :dialogue/initial-line-id
                                   ::location-id
                                   ::location-position
                                   ::synopsis]
                          :opt-un [:dialogue/states]))

(s/def :dialogue/states (s/map-of ::line-id ::text))
(s/def :dialogue/initial-line-id ::line-id)
(s/def ::dialogues (s/and ::entity-map
                          (s/map-of ::dialogue-id ::dialogue)))

;; Modals

(s/def ::npc-line-id ::line-id)
(s/def ::player-line-id ::line-id)
(s/def ::modal (s/keys :opt-un [:modal/character-form
                                :modal/dialogue-creation
                                :modal/location-creation
                                :modal/trigger-creation
                                ::npc-line-id
                                ::player-line-id
                                ::dialogue-state]))

(s/def :modal/location-creation ::display-name)

(s/def :modal/character-form
  (s/keys :req-un [::display-name ::color ::texture]
          :opt-un [:entity/id]))

(s/def :modal/dialogue-creation
  (s/keys :req-un [::character-id ::synopsis]
          :opt-un [::location-id ::location-position]))

(s/def :modal/trigger-creation
  (s/keys :req-un [::trigger-node-id
                   :trigger/switch-id
                   :trigger/switch-kind
                   :trigger/switch-value]))

(s/def ::dialogue-state (s/keys :req-un [::line-id]
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
    (let [player-lines (->> lines (where-map :kind :player))
          option-target-ids (map :next-line-id (vals options))]
      (not-any? #(contains? player-lines %) option-target-ids))))

(s/def ::state (s/and ::dialogue-must-start-with-npc-line
                      ::player-options-must-not-point-to-player-line
                      ::location-connection-validation
                      (s/keys :req-un [::current-page
                                       ::player
                                       ::characters
                                       ::dialogues
                                       ::player-options
                                       ::lines
                                       ::triggers
                                       ::location-editor
                                       ::locations
                                       ::location-connections]
                              :opt-un [::connecting
                                       ::dragging
                                       ::cursor
                                       ::modal])))

(s/def ::player (s/keys :req-un [::location-id
                                 ::location-position]))

(defn clear-dialogue-state [db line-id]
  (let [dialogue-id (get-in db [:lines line-id :dialogue-id])
        trigger-ids (->> (:triggers db)
                         (filter-map (fn [{v :switch-value kind :kind}]
                                       (and (= kind :dialogue-state)
                                            (= v line-id))))
                         keys
                         set)]
    (-> db
        (update :lines (fn [lines]
                         (map-values (fn [l]
                                       (if (contains? l :trigger-ids)
                                         (update l :trigger-ids
                                                 (fn [ids]
                                                   (vec (remove #(contains? trigger-ids %) ids))))
                                         l))
                                     lines)))
        (update-in [:dialogues dialogue-id :states] dissoc line-id)
        (update :triggers #(apply dissoc % trigger-ids)))))

(def default-db
  (merge {:location-editor {:tool :info
                            :active-walk-state true
                            :active-texture (first background-textures)}
          :ui/positions {}
          :characters {}
          :locations {}
          :location-connections #{}
          :dialogues {}
          :lines {}
          :triggers {}}
         (->> dummy-data
              ((migrations 2))
              ((migrations 3)))))
