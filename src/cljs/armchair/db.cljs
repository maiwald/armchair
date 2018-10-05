(ns armchair.db
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.set :refer [subset?]]
            [armchair.datascript :refer [conn]]
            [armchair.textures :refer [background-textures texture-set]]
            [armchair.util :refer [where-map]]))

;; Types

(s/def ::id pos-int?)
(s/def ::text #(not (string/blank? %)))
(s/def ::point (s/tuple integer? integer?))
(s/def ::rect (s/and (s/tuple ::point ::point)
                     (fn [[[x1 y1] [x2 y2]]] (and (< x1 x2)
                                                  (< y1 y2)))))
(s/def ::entity-map (s/every (fn [[k v]] (= k (:id v)))))
(s/def ::undirected-connection (s/coll-of ::id :kind set? :count 2))
(s/def ::texture (s/nilable #(contains? texture-set %)))

(s/def ::cursor ::point)
(s/def ::position ::point)

;; UI State

(s/def ::positions (s/map-of ::position-id ::position))

(s/def ::cursor-start ::point)
(s/def ::connecting-lines (s/keys :req-un [::cursor-start ::line-id]
                                  :opt-un [::index]))
(s/def ::connecting-locations (s/keys :req-un [::cursor-start ::location-id]))
(s/def ::connecting (s/or :lines ::connecting-lines
                          :locations ::connecting-locations))

(s/def ::position-ids (s/coll-of ::position-id))
(s/def ::dragging (s/keys :req-un [::cursor-start ::ids]))
(s/def ::current-page (s/nilable string?))

;; Location Editor

(s/def ::painting? boolean?)
(s/def ::tool #{:npcs-select :resize :collision :background-painter :connection-select})
(s/def ::highlight ::point)
(s/def ::active-texture ::texture)
(s/def ::location-editor (s/keys :req-un [::tool
                                          ::painting?
                                          ::active-texture]
                                 :opt-un [::highlight]))

;; Data

(s/def ::character-id ::id)
(s/def ::dialogue-id ::id)
(s/def ::line-id ::id)
(s/def ::initial-line-id ::line-id)
(s/def ::location-id ::id)
(s/def ::position-id ::id)
(s/def ::info-id ::id)

(s/def ::display-name ::text)
(s/def ::color ::text)

(s/def ::dimension ::rect)
(s/def ::background (s/map-of ::point ::texture))
(s/def ::walk-set (s/coll-of ::point :kind set?))
(s/def ::connection-triggers (s/map-of ::point ::location-id))
(s/def ::npcs (s/map-of ::point ::character-id))
(s/def ::location (s/keys :req-un [::id
                                   ::dimension
                                   ::display-name
                                   ::position-id
                                   ::background
                                   ::walk-set
                                   ::connection-triggers]
                          :opt-un [::npcs]))
(s/def ::locations (s/and ::entity-map
                          (s/map-of ::location-id ::location)))
(s/def ::location-connections (s/coll-of ::undirected-connection :kind set?))

(s/def ::character (s/keys :req-un [::id ::display-name ::color ::texture]))
(s/def ::characters (s/and ::entity-map
                           (s/map-of ::character-id ::character)))

(s/def ::info (s/keys :req-un [::id ::description]))
(s/def ::infos (s/and ::entity-map
                      (s/map-of ::info-id ::info)))

;; Dialogue & Lines

(s/def ::next-line-id (s/or :line-id (s/nilable ::line-id)))
(s/def ::line (s/keys :req-un [::text ::next-line-id]))

(s/def ::info-ids (s/coll-of ::info-id :kind set?))
(s/def ::npc-line (s/and ::line
                         (s/keys :req-un [::character-id]
                                 :opt-un [::info-ids])
                         #(= (:kind %) :npc)))
(s/def ::required-info-ids ::info-ids)
(s/def ::option (s/and ::line
                       (s/keys :opt-un [::required-info-ids])))
(s/def ::options (s/coll-of ::line :kind vector?))
(s/def ::player-line (s/and #(= (:kind %) :player)
                            (s/keys :req-un [::options])))

(s/def ::npc-or-player-line (s/and (s/keys :req-un [::id ::dialogue-id ::position-id])
                                   (s/or :npc ::npc-line :player ::player-line)))

(s/def ::lines (s/and ::entity-map
                      (s/map-of ::line-id ::npc-or-player-line)))

(s/def ::dialogue (s/keys :req-un [::id ::character-id ::initial-line-id ::location-id]
                          :opt-un [::description]))
(s/def ::dialogues (s/and ::entity-map
                          (s/map-of ::dialogue-id ::dialogue)))

;; Modals

(s/def ::npc-line-id ::line-id)
(s/def ::player-line-id ::line-id)
(s/def ::dialogue-creation (s/keys :req-un [::character-id
                                            ::location-id]
                                   :opt-un [::description]))

(s/def ::modal (s/keys :opt-un [::character-id
                                ::info-id
                                ::npc-line-id
                                ::player-line-id
                                ::dialogue-creation]))

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

(s/def ::state (s/and ::dialogue-must-start-with-npc-line
                      ::player-options-must-not-point-to-player-line
                      ::location-connection-validation
                      (s/keys :req-un [::current-page
                                       ::positions
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

(def default-db
  {:current-page nil
   :store @conn
   :location-editor {:tool :background-painter
                     :painting? false
                     :active-texture (first background-textures)}})

(when-not (s/valid? ::state default-db)
  (js/console.log "Default DB state explain:")
  (s/explain ::state default-db))

(defn line-count-for-character [lines character-id]
  (let [filter-fn #(= (:character-id %) character-id)]
    (->> lines vals (filter filter-fn) count)))
