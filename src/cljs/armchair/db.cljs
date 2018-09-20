(ns armchair.db
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.set :refer [subset?]]
            [armchair.textures :refer [textures]]
            [armchair.util :refer [where-map]]))

;; Types

(s/def ::id pos-int?)
(s/def ::text #(not (string/blank? %)))
(s/def ::position (s/tuple integer? integer?))
(s/def ::entity-map (s/every (fn [[k v]] (= k (:id v)))))
(s/def ::undirected-connection (s/coll-of ::id :kind set? :count 2))
(s/def ::texture #(contains? (set textures) %))

;; UI State

(s/def ::pointer ::position)
(s/def ::position-ids (s/coll-of ::position-id))

(s/def ::start-position ::position)
(s/def ::connecting-lines (s/keys :req-un [::start-position ::line-id]
                                  :opt-un [::index]))
(s/def ::npcs (s/map-of ::position ::character-id))
(s/def ::connecting-locations (s/keys :req-un [::start-position ::location-id]
                                      :opt-un [::npcs]))
(s/def ::connecting (s/or :lines ::connecting-lines
                          :locations ::connecting-locations))

(s/def ::dragging (s/keys :req-un [::start-position ::position-ids]))
(s/def ::current-page (s/nilable string?))

;; Location Editor

(s/def ::painting? boolean?)
(s/def ::tool #{:select :collision :paint})
(s/def ::highlight ::position)
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

(s/def ::level (s/coll-of (s/coll-of ::texture :kind vector?) :kind vector?))
(s/def ::walk-set (s/coll-of ::position :kind set?))
(s/def ::location (s/keys :req-un [::id ::position-id ::level ::walk-set ::display-name]))
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

(s/def ::dialogue (s/keys :req-un [::id ::initial-line-id ::location-id ::display-name]))
(s/def ::dialogues (s/and ::entity-map
                          (s/map-of ::dialogue-id ::dialogue)))

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
                                       ::characters
                                       ::dialogues
                                       ::lines
                                       ::infos
                                       ::location-editor
                                       ::locations
                                       ::location-connections]
                              :opt-un [::connecting
                                       ::dragging
                                       ::pointer])))

(def default-db
  {:current-page nil
   :positions {1 [50 94]
               2 [335 85]
               3 [609 58]
               4 [1205 226]
               5 [1178 48]
               6 [1478 44]
               7 [901 47]
               16 [225 222]
               17 [258 102]
               18 [107 151]
               19 [407 151]
               20 [707 151]
               21 [885 389]
               22 [1196 387]}
   :location-editor {:tool :select
                     :painting? false
                     :active-texture (first textures)}
   :locations {1 {:id 1
                  :position-id 16
                  :display-name "Park - Camp"
                  :level [[:wall :wall :wall :wall :wall :wall :wall :wall :wall :wall :wall :wall :grass :wall]
                          [:wall :grass :wall :grass :grass :grass :grass :grass :grass :grass :grass :grass :grass :wall]
                          [:wall :grass :grass :grass :wall :wall :wall :wall :wall :wall :grass :grass :wall :wall]
                          [:wall :grass :grass :grass :wall :wall :wall :wall :wall :wall :grass :grass :grass :wall]
                          [:wall :grass :wall :grass :wall :wall :grass :grass :grass :grass :grass :wall :grass :wall]
                          [:wall :wall :wall :grass :grass :grass :grass :wall :wall :grass :wall :wall :grass :wall]
                          [:wall :wall :grass :grass :wall :wall :grass :wall :wall :grass :wall :grass :grass :wall]
                          [:wall :grass :grass :grass :grass :wall :grass :wall :wall :grass :grass :grass :grass :wall]
                          [:wall :grass :wall :grass :grass :wall :grass :grass :grass :grass :wall :wall :grass :wall]
                          [:wall :grass :wall :grass :grass :wall :grass :grass :grass :grass :wall :wall :grass :wall]
                          [:wall :grass :wall :grass :grass :wall :grass :grass :grass :grass :wall :wall :grass :wall]
                          [:wall :wall :wall :wall :grass :wall :grass :wall :wall :grass :wall :wall :wall :wall]
                          [:wall :grass :wall :grass :grass :grass :grass :grass :grass :grass :grass :wall :grass :wall]
                          [:wall :grass :grass :grass :wall :wall :wall :wall :wall :wall :grass :grass :grass :wall]
                          [:wall :grass :grass :grass :wall :wall :wall :wall :wall :wall :grass :grass :grass :wall]
                          [:wall :grass :wall :grass :wall :wall :grass :grass :grass :grass :grass :wall :grass :wall]
                          [:wall :grass :wall :grass :grass :grass :grass :wall :wall :grass :wall :wall :grass :wall]
                          [:wall :grass :grass :grass :wall :wall :grass :wall :wall :grass :wall :grass :grass :wall]
                          [:wall :grass :grass :grass :grass :wall :grass :wall :wall :grass :wall :grass :grass :wall]
                          [:wall :wall :wall :wall :wall :wall :grass :wall :wall :grass :wall :grass :grass :wall]
                          [:grass :grass :grass :grass :grass :wall :grass :grass :wall :grass :wall :grass :wall :wall]
                          [:wall :grass :grass :grass :grass :wall :grass :grass :wall :grass :grass :grass :wall :wall]
                          [:wall :grass :wall :grass :grass :wall :grass :wall :wall :grass :wall :grass :wall :wall]
                          [:wall :grass :wall :grass :grass :grass :grass :grass :grass :grass :wall :grass :grass :wall]
                          [:wall :wall :wall :wall :wall :wall :wall :wall :wall :wall :wall :wall :wall :wall]]
                  :walk-set #{[0 12]
                              [1 1] [1 3] [1 4] [1 5] [1 6] [1 7] [1 8] [1 9] [1 10] [1 11] [1 12]
                              [2 1] [2 2] [2 3] [2 10] [2 11]
                              [3 1] [3 2] [3 3] [3 10] [3 11] [3 12]
                              [4 1] [4 3] [4 6] [4 7] [4 8] [4 9] [4 10] [4 12]
                              [5 3] [5 4] [5 5] [5 6] [5 9] [5 12]
                              [6 2] [6 3] [6 6] [6 9] [6 11] [6 12]
                              [7 1] [7 2] [7 3] [7 4] [7 6] [7 9] [7 10] [7 11] [7 12]
                              [8 1] [8 3] [8 4] [8 6] [8 7] [8 8] [8 9] [8 12]
                              [9 1] [9 3] [9 4] [9 6] [9 7] [9 8] [9 9] [9 12]
                              [10 1] [10 3] [10 4] [10 6] [10 7] [10 8] [10 9] [10 12]
                              [11 4] [11 6] [11 9]
                              [12 1] [12 3] [12 4] [12 5] [12 6] [12 7] [12 8] [12 9] [12 10] [12 12]
                              [13 1] [13 2] [13 3] [13 10] [13 11] [13 12]
                              [14 1] [14 2] [14 3] [14 10] [14 11] [14 12]
                              [15 1] [15 3] [15 6] [15 7] [15 8] [15 9] [15 10] [15 12]
                              [16 1] [16 3] [16 4] [16 5] [16 6] [16 9] [16 12]
                              [17 1] [17 2] [17 3] [17 6] [17 9] [17 11] [17 12]
                              [18 1] [18 2] [18 3] [18 4] [18 6] [18 9] [18 11] [18 12]
                              [19 6] [19 9] [19 11] [19 12]
                              [20 0] [20 1] [20 2] [20 3] [20 4] [20 6] [20 7] [20 9] [20 11]
                              [21 1] [21 2] [21 3] [21 4] [21 6] [21 7] [21 9] [21 10] [21 11]
                              [22 1] [22 3] [22 4] [22 6] [22 9] [22 11]
                              [23 1] [23 3] [23 4] [23 5] [23 6] [23 7] [23 8] [23 9] [23 11] [23 12]}
                  :npcs {[6 6] 1
                         [5 12] 3}}
               2 {:id 2
                  :position-id 17
                  :display-name "Park - Entrance"
                  :level [[:wall :wall :wall]
                          [:wall :grass :wall]
                          [:wall :grass :wall]]
                  :walk-set #{[1 1] [2 1]}}}
   :location-connections #{#{1 2}}
   :characters {1 {:id 1 :display-name "Hugo" :color "rgba(255, 0, 0, .6)" :texture :hugo}
                3 {:id 3 :display-name "Gustav" :color "rgba(92, 154, 9, 0.8)" :texture :gustav}}
   :dialogues {1 {:id 1 :display-name "Hugo's Dialogue" :initial-line-id 1 :location-id 1}
               2 {:id 2 :display-name "Gustav's Dialogue" :initial-line-id 14 :location-id 1}}
   :infos {1 {:id 1 :description "Hugo's Name is Hugo"}}
   :lines {1 {:id 1
              :kind :npc
              :character-id 1
              :dialogue-id 1
              :position-id 1
              :text "Hey, who are you?"
              :next-line-id 2}
           2 {:id 2
              :dialogue-id 1
              :position-id 2
              :kind :player
              :options [{:text "I could ask you the same." :next-line-id 3}
                        {:text "My name does not matter." :next-line-id 4}
                        {:text "Silence! Hugo, you must come with me at once! The fate of the world is at stake."
                         :next-line-id 17
                         :required-info-ids #{1}}]}
           3 {:id 3
              :kind :npc
              :dialogue-id 1
              :character-id 1
              :position-id 3
              :text "I am Hugo. And you?"
              :next-line-id 7
              :info-ids #{1}}
           4 {:id 4
              :kind :npc
              :dialogue-id 1
              :character-id 1
              :position-id 4
              :text "Fine, be a jerk."
              :next-line-id nil}
           5 {:id 5
              :kind :npc
              :dialogue-id 1
              :character-id 1
              :position-id 5
              :text "What a strange coincidence! Two Hugos. Who would have thought."
              :next-line-id 6}
           6 {:id 6
              :dialogue-id 1
              :kind :npc
              :character-id 1
              :position-id 6
              :text "Anyway, ...bye!"
              :next-line-id nil}
           7 {:id 7
              :dialogue-id 1
              :position-id 7
              :kind :player
              :options [{:text "I am also Hugo! But for the sake of testing I keep talking way beyond what could possible fit into this box." :next-line-id 5}
                        {:text "That's none of your business!" :next-line-id 4}]}
           14 {:id 14
               :character-id 3
               :kind :npc
               :dialogue-id 2
               :position-id 18
               :text "Yes?"
               :next-line-id 15}
           15 {:id 15
               :dialogue-id 2
               :kind :player
               :position-id 19
               :options [{:text "Who are you?" :next-line-id 16}]}
           16 {:id 16
               :kind :npc
               :character-id 3
               :dialogue-id 2
               :position-id 20
               :text "I am Gustav!"
               :next-line-id nil}
           17 {:id 17
               :kind :npc
               :character-id 1
               :dialogue-id 1
               :position-id 21
               :text "Whaaaaaaaaaaat!?"
               :next-line-id 18}
           18 {:id 18
               :kind :npc
               :character-id 1
               :dialogue-id 1
               :position-id 22
               :text "How do you know my name!?"
               :next-line-id nil}}})

(when-not (s/valid? ::state default-db)
  (js/console.log "Default DB state explain:")
  (s/explain ::state default-db))

(defn line-count-for-character [lines character-id]
  (let [filter-fn #(= (:character-id %) character-id)]
    (->> lines vals (filter filter-fn) count)))
