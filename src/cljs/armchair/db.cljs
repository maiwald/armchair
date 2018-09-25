(ns armchair.db
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.set :refer [subset?]]
            [armchair.textures :refer [background-textures texture-set]]
            [armchair.util :refer [where-map]]))

;; Types

(s/def ::id pos-int?)
(s/def ::text #(not (string/blank? %)))
(s/def ::position (s/tuple integer? integer?))
(s/def ::entity-map (s/every (fn [[k v]] (= k (:id v)))))
(s/def ::undirected-connection (s/coll-of ::id :kind set? :count 2))
(s/def ::texture (s/nilable #(contains? texture-set %)))

;; UI State

(s/def ::pointer ::position)
(s/def ::position-ids (s/coll-of ::position-id))

(s/def ::start-position ::position)
(s/def ::connecting-lines (s/keys :req-un [::start-position ::line-id]
                                  :opt-un [::index]))
(s/def ::connecting-locations (s/keys :req-un [::start-position ::location-id]))
(s/def ::connecting (s/or :lines ::connecting-lines
                          :locations ::connecting-locations))

(s/def ::dragging (s/keys :req-un [::start-position ::position-ids]))
(s/def ::current-page (s/nilable string?))

;; Location Editor

(s/def ::painting? boolean?)
(s/def ::tool #{:select :resize :collision :paint :connections})
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
(s/def ::connection-triggers (s/map-of ::position ::location-id))
(s/def ::npcs (s/map-of ::position ::character-id))
(s/def ::location (s/keys :req-un [::id
                                   ::display-name
                                   ::position-id
                                   ::level
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
                     :active-texture (first background-textures)}
   :locations {1 {:id 1, :position-id 16, :display-name "Park - Camp", :level [[:wall :wall :wall :wall :wall :wall :wall :wall :wall :wall :wall :stone :stone :wall] [:wall :grass_dirt_left :grass_dirt_left :grass_dirt_left :grass_dirt_left :grass_dirt_bottom-left :grass :grass :grass :wall :stone_2 :stone :stone :wall] [:wall :dirt :dirt :dirt :dirt :house_roof_top-left :house_roof_middle-left :house_roof_bottom-left :grass :wall :stone :stone :stone :wall] [:wall :dirt :dirt :dirt :dirt :house_roof_top-right :house_roof_middle-right :house_roof_bottom-right :grass :wall :stone :stone_grass_top-left :grass_stone_right :wall] [:wall :grass_dirt_top-right :dirt :house_roof_top-left :house_roof_middle-left :house_roof_bottom-left :house_bottom_left :grass_stone_left :grass_stone_left :stone_grass_bottom-right :stone :grass_stone_bottom :grass :wall] [:wall :wall :grass_dirt_top :house_roof_top :house_roof_middle :house_roof_bottom :house_door_bottom :stone_2 :stone_2 :stone_2 :stone :grass_stone_bottom :grass :wall] [:wall :wall :grass_dirt_top :house_roof_top-right :house_roof_middle-right :house_roof_bottom-right :house_bottom_right :grass_stone_right :stone_grass_bottom-left :stone :stone :grass_stone_bottom :grass :wall] [:wall :grass_dirt_top :dirt :grass_dirt_left :house_roof_top-left :house_roof_bottom-left :house_bottom_left :grass :grass_stone_top :stone :stone_2 :grass_stone_bottom :grass :wall] [:wall :grass_dirt_top :dirt :dirt :house_roof_top-right :house_roof_bottom-right :house_bottom_right :grass :wall :stone :stone_2 :wall :grass :wall] [:wall :grass_dirt_top :dirt :dirt :dirt :grass :grass :grass :wall :stone :stone :wall :grass :wall] [:wall :grass_dirt_top :dirt :dirt :dirt :grass :grass :grass :grass_stone_top :stone_2 :stone :wall :grass :wall] [:wall :wall :wall :wall :wall :wall :grass :grass :grass_stone_top :stone_2 :stone :wall :wall :wall] [:wall :grass :grass :grass :grass :grass :grass :grass :grass_stone_top :stone_2 :stone :wall :grass :wall] [:wall :grass :grass :grass :grass :grass :grass :grass :grass_stone_top :stone :stone :grass_stone_bottom :grass :wall] [:wall :grass :grass :grass :grass :grass :grass :grass_stone_top-left :stone_grass_bottom-right :stone :stone_grass_top-left :grass_stone_bottom-right :grass :wall] [:wall :grass :wall :grass :grass :grass :grass_stone_top-left :stone_grass_bottom-right :stone :stone :grass_stone_bottom :wall :grass :wall] [:wall :grass :wall :grass :grass :grass :grass_stone_top :stone_2 :stone_2 :stone :grass_stone_bottom :wall :grass :wall] [:wall :grass :grass :grass :wall :wall :grass_stone_top :stone_2 :stone_2 :stone :grass_stone_bottom :grass :grass :wall] [:wall :grass :grass :grass :grass :wall :stone_grass_bottom-right :stone_2 :wall :wall :wall :grass :grass :wall] [:wall :wall :wall :wall :wall :wall :stone_2 :stone_2 :wall :grass :wall :grass :grass :wall] [:stone_2 :stone_2 :stone_2 :stone_2 :stone_2 :stone_2 :stone_2 :stone_grass_top-left :wall :grass :wall :grass_stone_left :wall :wall] [:wall :stone_2 :stone_2 :stone_2 :stone_2 :wall :stone_grass_top-left :grass :wall :grass :grass_stone_top :stone :stone :wall] [:wall :grass_stone_right :wall :grass_stone_right :grass_stone_right :wall :wall :wall :wall :grass :wall :stone :stone :wall] [:wall :grass :wall :grass :grass :grass :grass :grass :grass :grass :wall :stone :stone :wall] [:wall :wall :wall :wall :wall :wall :wall :wall :wall :wall :wall :wall :wall :wall]],
 :walk-set #{[18 7] [13 2] [1 10] [10 9] [6 8] [13 11] [6 9] [5 7] [5 9] [21 9] [21 7] [9 1] [17 1] [4 11] [18 2] [14 10] [3 4] [1 2] [0 11] [9 7] [5 12] [23 8] [2 2] [22 4] [1 5] [3 2] [8 12] [13 10] [14 12] [1 11] [7 1] [21 2] [7 9] [20 9] [2 4] [13 12] [23 3] [21 11] [2 12] [10 1] [23 7] [13 9] [10 6] [7 2] [4 2] [12 6] [4 10] [12 2] [1 3] [22 1] [5 6] [2 3] [15 12] [8 1] [14 3] [18 12] [11 7] [11 9] [23 1] [9 6] [15 3] [5 2] [16 7] [23 6] [9 3] [2 11] [17 6] [4 7] [8 3] [3 1] [18 6] [17 2] [14 11] [22 9] [18 11] [21 10] [12 7] [17 9] [20 6] [19 9] [18 1] [4 9] [12 9] [15 8] [17 8] [20 4] [20 5] [14 2] [12 12] [13 5] [11 10] [15 4] [20 0] [16 4] [18 4] [13 8] [6 12] [16 3] [1 1] [15 7] [23 9] [15 9] [10 10] [12 4] [12 3] [10 7] [8 7] [12 5] [16 12] [9 5] [14 4] [1 12] [18 3] [13 1] [12 8] [7 7] [16 10] [16 9] [16 1] [3 3] [14 7] [12 10] [15 6] [10 12] [21 12] [3 11] [6 11] [14 1] [10 3] [9 9] [20 7] [7 12] [11 8] [16 6] [9 4] [17 7] [19 12] [22 11] [23 12] [21 6] [14 5] [15 10] [17 12] [1 4] [23 5] [12 1] [6 7] [1 7] [4 8] [1 8] [23 4] [19 7] [13 3] [3 12] [3 10] [10 2] [2 10] [7 10] [14 9] [5 10] [19 6] [10 4] [2 1] [7 8] [4 1] [16 8] [5 11] [0 12] [15 1] [14 6] [5 8] [3 8] [7 11] [9 10] [6 10] [1 6] [15 5] [14 8] [22 3] [16 5] [17 11] [13 6] [21 1] [17 3] [22 12] [8 10] [4 12] [10 5] [9 12] [11 6] [13 4] [20 2] [7 3] [21 3] [2 8] [8 9] [20 1] [21 4] [23 11] [13 7] [10 8] [6 2] [20 11] [8 2] [19 11] [20 3] [9 2] [17 10]},
 :npcs {[5 4] 1 [6 8] 3}, :connection-triggers {[20 0] 2}}
               2 {:id 2
                  :position-id 17
                  :display-name "Park - Entrance"
                  :level [[:wall :wall :wall]
                          [:wall :grass :wall]
                          [:wall :grass :wall]]
                  :walk-set #{[1 1] [2 1]}
                  :connection-triggers {}}}
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
