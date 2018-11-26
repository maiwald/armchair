(ns armchair.db
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.set :refer [union subset?]]
            [cognitect.transit :as t]
            [armchair.dummy-data :refer [dummy-data]]
            [armchair.textures :refer [background-textures texture-set]]
            [armchair.util :refer [where where-map map-values]]))

(def db-version 2)

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
                                  ds)))))})

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
                      :infos
                      :locations
                      :characters
                      :dialogues
                      :location-connections
                      :lines]]
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
(s/def :entity/type #{:location :character :line :info :dialogue})
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
(s/def ::info-id :entity/id)

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

(s/def ::info (s/keys :req [:entity/id :entity/type]
                      :req-un [::description]))
(s/def ::infos (s/and ::entity-map
                      (s/map-of ::info-id ::info)))

;; Dialogue & Lines

(s/def ::next-line-id (s/or :line-id (s/nilable ::line-id)))

(s/def ::npc-line (s/and (s/keys :req-un [::text ::next-line-id ::character-id]
                                 :opt-un [::info-ids :dialogue/state-triggers])
                         #(= (:kind %) :npc)))
(s/def ::info-ids (s/coll-of ::info-id :kind set?))

(s/def ::player-line (s/and #(= (:kind %) :player)
                            (s/keys :req-un [::options])))
(s/def ::options (s/coll-of ::option :kind vector?))
(s/def ::option (s/keys :req-un [::text ::next-line-id]
                        :opt-un [::required-info-ids :dialogue/state-triggers]))
(s/def ::required-info-ids ::info-ids)

(s/def ::npc-or-player-line (s/and (s/keys :req [:entity/id
                                                 :entity/type]
                                           :req-un [::dialogue-id])
                                   (s/or :npc ::npc-line
                                         :player ::player-line)))

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
(s/def :dialogue/state-triggers (s/coll-of ::line-id :kind set?))
(s/def :dialogue/initial-line-id ::line-id)
(s/def ::dialogues (s/and ::entity-map
                          (s/map-of ::dialogue-id ::dialogue)))

;; Modals

(s/def ::npc-line-id ::line-id)
(s/def ::player-line-id ::line-id)
(s/def ::modal (s/keys :opt-un [:modal/character-form
                                :modal/dialogue-creation
                                :modal/info-form
                                ::npc-line-id
                                ::player-line-id
                                ::dialogue-state]))

(s/def :modal/info-form
  (s/keys :req-un [::description]
          :opt-un [:entity/id]))

(s/def :modal/character-form
  (s/keys :req-un [::display-name ::color ::texture]
          :opt-un [:entity/id]))

(s/def :modal/dialogue-creation
  (s/keys :req-un [::character-id ::synopsis]
          :opt-un [::location-id ::location-position]))

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
  (fn [{lines :lines}]
    (let [player-lines (->> lines (where-map :kind :player))
          option-target-ids (->> player-lines vals (map :options) flatten (map :next-line-id))]
      (not-any? #(contains? player-lines %) option-target-ids))))

(s/def ::state-trigger-must-point-to-dialogue-state
  (fn [{:keys [lines dialogues]}]
    (let [states (->> (vals dialogues)
                      (map :states)
                      (filter seq)
                      (map keys)
                      flatten
                      set)
          npc-triggers (->> (vals lines)
                            (where :kind :npc)
                            (map :state-triggers)
                            (filter seq)
                            (apply union))
          player-triggers (->> (vals lines)
                               (where :kind :player)
                               (map :options)
                               flatten
                               (map :state-triggers)
                               (filter seq)
                               (apply union))]
      (and (subset? npc-triggers states)
           (subset? player-triggers states)))))

(s/def ::lines-contain-max-one-state-triggers-per-dialogue
  (fn [{:keys [lines]}]
    (let [has-max-one-dialogue (fn [state-triggers]
                                 (->> state-triggers
                                      (select-keys lines)
                                      vals
                                      (group-by :dialogue-id)
                                      vals
                                      (every? #(= 1 (count %)))))
          npc-triggers (->> (vals lines)
                            (where :kind :npc)
                            (map :state-triggers)
                            (filter seq))
          player-triggers (->> (vals lines)
                               (where :kind :player)
                               (map :options)
                               flatten
                               (map :state-triggers)
                               (filter seq))]
      (and (every? has-max-one-dialogue npc-triggers)
           (every? has-max-one-dialogue player-triggers)))))

(s/def ::state (s/and ::dialogue-must-start-with-npc-line
                      ::player-options-must-not-point-to-player-line
                      ::location-connection-validation
                      ::state-trigger-must-point-to-dialogue-state
                      ::lines-contain-max-one-state-triggers-per-dialogue
                      (s/keys :req-un [::current-page
                                       ::player
                                       ::characters
                                       ::dialogues
                                       ::lines
                                       ::infos
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
  (let [dialogue-id (get-in db [:lines line-id :dialogue-id])]
    (letfn [(clear-line [line]
              (if (set? (:state-triggers line))
                (update line :state-triggers disj line-id)
                line))
            (clear-options [line]
              (update line :options #(mapv clear-line %)))]
      (-> db
        (update :lines #(map-values (fn [line]
                                      (case (:kind line)
                                        :npc (clear-line line)
                                        :player (clear-options line)))
                                    %))
        (update-in [:dialogues dialogue-id :states] dissoc line-id)))))

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
          :infos {}}
         dummy-data))
