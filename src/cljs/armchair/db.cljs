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
(s/def ::rect (s/and (s/tuple ::position ::position)
                          (fn [[[x1 y1] [x2 y2]]] (and (< x1 x2)
                                                       (< y1 y2)))))
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

(s/def ::dimension ::rect)
(s/def ::background (s/map-of ::position ::texture))
(s/def ::walk-set (s/coll-of ::position :kind set?))
(s/def ::connection-triggers (s/map-of ::position ::location-id))
(s/def ::npcs (s/map-of ::position ::character-id))
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

(s/def ::dialogue (s/keys :req-un [::id ::initial-line-id ::location-id]
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
                                       ::characters
                                       ::dialogues
                                       ::lines
                                       ::infos
                                       ::location-editor
                                       ::locations
                                       ::location-connections]
                              :opt-un [::connecting
                                       ::dragging
                                       ::pointer
                                       ::modal])))

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
   :location-editor {:tool :paint
                     :painting? false
                     :active-texture (first background-textures)}
   :locations {1 {:id 1, :position-id 16, :display-name "Park - Camp",
                  :dimension [[0 0] [24 13]]
                  :background {[18 7] :stone_2, [13 2] :grass, [0 1] :wall, [18 13] :wall, [1 10] :stone_2, [14 0] :wall, [10 9] :stone_2, [6 8] :stone_grass_bottom-left, [13 11] :grass_stone_bottom, [6 9] :stone, [5 7] :stone_2, [18 8] :wall, [5 9] :stone_2, [21 9] :grass, [20 10] :wall, [0 10] :wall, [0 8] :wall, [16 13] :wall, [21 7] :grass, [9 1] :grass_dirt_top, [8 13] :wall, [4 3] :house_roof_top-left, [17 1] :grass, [18 9] :wall, [4 11] :grass_stone_bottom, [18 2] :grass, [14 10] :stone_grass_top-left, [3 4] :dirt, [1 2] :grass_dirt_left, [0 5] :wall, [0 11] :stone, [19 3] :wall, [0 0] :wall, [9 7] :grass, [5 12] :grass, [5 3] :house_roof_top, [24 7] :wall, [13 13] :wall, [23 8] :grass, [2 2] :dirt, [16 2] :wall, [22 4] :grass_stone_right, [1 5] :grass_dirt_bottom-left, [3 2] :dirt, [4 6] :house_bottom_left, [16 11] :wall, [21 5] :wall, [8 12] :grass, [24 3] :wall, [9 11] :wall, [8 5] :house_roof_bottom-right, [13 10] :stone, [14 12] :grass, [1 11] :stone, [7 1] :grass_dirt_top, [0 7] :wall, [21 2] :stone_2, [7 9] :stone, [20 9] :grass, [2 4] :dirt, [13 12] :grass, [23 3] :grass, [21 11] :stone, [2 12] :stone, [10 1] :grass_dirt_top, [23 7] :grass, [13 9] :stone, [22 2] :wall, [10 6] :grass, [7 2] :dirt, [4 2] :dirt, [21 0] :wall, [12 6] :grass, [4 10] :stone, [12 2] :grass, [24 9] :wall, [1 3] :grass_dirt_left, [10 11] :wall, [22 1] :grass_stone_right, [19 8] :wall, [5 5] :house_roof_bottom, [2 13] :wall, [5 6] :house_door_bottom, [2 3] :dirt, [15 12] :grass, [11 3] :wall, [8 1] :grass_dirt_top, [14 3] :grass, [18 12] :grass, [11 7] :grass, [0 13] :wall, [11 9] :stone_2, [23 1] :grass, [21 13] :wall, [0 9] :wall, [9 6] :grass, [15 3] :grass, [5 2] :grass_dirt_top, [16 7] :stone_2, [23 6] :grass, [9 3] :dirt, [9 0] :wall, [2 11] :stone, [12 0] :wall, [17 6] :grass_stone_top, [12 13] :wall, [4 7] :grass_stone_left, [8 3] :dirt, [3 1] :dirt, [18 6] :stone_grass_bottom-right, [17 2] :grass, [14 11] :grass_stone_bottom-right, [22 9] :grass, [19 10] :wall, [18 11] :grass, [9 13] :wall, [4 13] :wall, [21 10] :grass_stone_top, [7 0] :wall, [12 7] :grass, [17 9] :stone, [20 6] :stone_2, [0 2] :wall, [15 2] :wall, [2 9] :wall, [19 9] :grass, [18 1] :grass, [4 9] :stone_grass_bottom-right, [12 9] :stone_2, [15 8] :stone, [24 4] :wall, [7 13] :wall, [5 1] :wall, [17 8] :stone_2, [20 4] :stone_2, [20 5] :stone_2, [19 4] :wall, [3 0] :wall, [14 2] :grass, [2 7] :house_roof_bottom-left, [12 12] :grass, [13 5] :grass, [11 4] :wall, [6 13] :wall, [11 10] :stone, [15 4] :grass, [20 0] :stone_2, [21 8] :wall, [16 4] :grass, [22 10] :wall, [18 4] :grass, [13 8] :grass_stone_top, [0 6] :wall, [6 12] :grass, [8 8] :wall, [4 5] :house_roof_bottom-left, [16 3] :grass, [24 11] :wall, [20 8] :wall, [1 1] :grass_dirt_left, [15 7] :stone_grass_bottom-right, [23 9] :grass, [15 9] :stone, [23 13] :wall, [6 0] :wall, [15 11] :wall, [10 10] :stone, [12 4] :grass, [12 3] :grass, [10 7] :grass, [8 7] :grass, [12 5] :grass, [16 12] :grass, [9 5] :grass, [24 1] :wall, [19 5] :wall, [7 4] :house_roof_top-left, [11 12] :wall, [14 4] :grass, [1 12] :stone, [18 3] :grass, [14 13] :wall, [8 4] :house_roof_top-right, [13 1] :grass, [12 8] :grass_stone_top, [7 7] :grass, [22 6] :wall, [16 10] :grass_stone_bottom, [16 9] :stone, [16 1] :grass, [3 3] :dirt, [8 11] :wall, [14 7] :grass_stone_top-left, [20 12] :wall, [5 0] :wall, [12 10] :stone, [23 10] :wall, [11 13] :wall, [15 6] :grass_stone_top-left, [10 12] :grass, [18 5] :wall, [5 4] :house_roof_middle, [13 0] :wall, [21 12] :stone, [3 11] :stone_grass_top-left, [15 0] :wall, [2 6] :house_roof_middle-left, [17 4] :wall, [1 13] :wall, [18 0] :wall, [6 11] :grass_stone_bottom, [14 1] :grass, [10 3] :dirt, [9 9] :stone, [24 0] :wall, [20 7] :stone_grass_top-left, [7 12] :grass, [11 8] :grass_stone_top, [16 6] :grass_stone_top, [9 4] :dirt, [17 7] :stone_2, [19 12] :grass, [22 11] :stone, [6 3] :house_roof_top-right, [23 12] :stone, [21 6] :stone_grass_top-left, [22 8] :wall, [14 5] :grass, [3 5] :house_roof_top-right, [22 7] :wall, [8 6] :house_bottom_right, [22 13] :wall, [11 0] :wall, [3 9] :wall, [11 11] :wall, [6 6] :house_bottom_right, [15 10] :grass_stone_bottom, [17 12] :grass, [1 4] :grass_dirt_left, [19 1] :wall, [23 5] :grass, [12 1] :grass, [6 7] :grass_stone_right, [1 7] :grass, [4 8] :grass_stone_left, [24 12] :wall, [1 8] :grass, [24 13] :wall, [11 1] :wall, [19 13] :wall, [11 5] :wall, [23 4] :grass, [19 0] :wall, [19 7] :stone_2, [13 3] :grass, [0 3] :wall, [3 12] :grass_stone_right, [16 0] :wall, [24 10] :wall, [3 10] :stone, [6 5] :house_roof_bottom-right, [1 9] :wall, [24 6] :wall, [20 13] :wall, [10 2] :dirt, [2 10] :stone, [19 2] :wall, [7 10] :stone_2, [14 9] :stone, [5 10] :stone, [19 6] :stone_2, [10 4] :dirt, [2 1] :dirt, [7 8] :grass_stone_top, [4 1] :grass_dirt_top-right, [22 5] :wall, [16 8] :stone_2, [5 11] :grass_stone_bottom, [24 5] :wall, [7 5] :house_roof_bottom-left, [3 6] :house_roof_middle-right, [0 12] :stone, [17 0] :wall, [22 0] :wall, [24 2] :wall, [3 13] :wall, [15 1] :grass, [14 6] :grass, [5 8] :stone_2, [3 8] :grass, [6 1] :wall, [7 11] :grass_stone_bottom, [8 0] :wall, [9 10] :stone, [6 10] :stone, [1 6] :grass, [15 5] :grass, [10 0] :wall, [4 4] :house_roof_middle-left, [14 8] :stone_grass_bottom-right, [22 3] :grass_stone_right, [16 5] :grass, [17 11] :grass, [13 6] :grass, [21 1] :stone_2, [17 3] :grass, [22 12] :stone, [5 13] :wall, [3 7] :house_roof_bottom-right, [1 0] :wall, [9 8] :wall, [8 10] :stone_2, [4 12] :grass, [18 10] :wall, [10 5] :grass, [9 12] :grass, [11 6] :grass, [13 4] :grass, [20 2] :stone_2, [7 3] :grass_dirt_left, [21 3] :stone_2, [2 0] :wall, [2 8] :grass, [11 2] :wall, [8 9] :stone, [15 13] :wall, [23 2] :wall, [20 1] :stone_2, [4 0] :wall, [21 4] :stone_2, [17 5] :wall, [23 11] :stone, [12 11] :wall, [13 7] :grass, [10 8] :grass_stone_top, [24 8] :wall, [6 2] :grass_dirt_top, [20 11] :grass_stone_left, [8 2] :dirt, [19 11] :grass, [6 4] :house_roof_middle-right, [20 3] :stone_2, [2 5] :house_roof_top-left, [9 2] :dirt, [17 13] :wall, [0 4] :wall, [7 6] :house_bottom_left, [10 13] :wall, [23 0] :wall, [17 10] :grass_stone_bottom}
                  :walk-set #{[18 7] [13 2] [1 10] [10 9] [6 8] [13 11] [6 9] [5 7] [5 9] [21 9] [21 7] [9 1] [17 1] [4 11] [18 2] [14 10] [3 4] [1 2] [0 11] [9 7] [5 12] [23 8] [2 2] [22 4] [1 5] [3 2] [8 12] [13 10] [14 12] [1 11] [7 1] [21 2] [7 9] [20 9] [2 4] [13 12] [23 3] [21 11] [2 12] [10 1] [23 7] [13 9] [10 6] [7 2] [4 2] [12 6] [4 10] [12 2] [1 3] [22 1] [5 6] [2 3] [15 12] [8 1] [14 3] [18 12] [11 7] [11 9] [23 1] [9 6] [15 3] [5 2] [16 7] [23 6] [9 3] [2 11] [17 6] [4 7] [8 3] [3 1] [18 6] [17 2] [14 11] [22 9] [18 11] [21 10] [12 7] [17 9] [20 6] [19 9] [18 1] [4 9] [12 9] [15 8] [17 8] [20 4] [20 5] [14 2] [12 12] [13 5] [11 10] [15 4] [20 0] [16 4] [18 4] [13 8] [6 12] [16 3] [1 1] [15 7] [23 9] [15 9] [10 10] [12 4] [12 3] [10 7] [8 7] [12 5] [16 12] [9 5] [14 4] [1 12] [18 3] [13 1] [12 8] [7 7] [16 10] [16 9] [16 1] [3 3] [14 7] [12 10] [15 6] [10 12] [21 12] [3 11] [6 11] [14 1] [10 3] [9 9] [20 7] [7 12] [11 8] [16 6] [9 4] [17 7] [19 12] [22 11] [23 12] [21 6] [14 5] [15 10] [17 12] [1 4] [23 5] [12 1] [6 7] [1 7] [4 8] [1 8] [23 4] [19 7] [13 3] [3 12] [3 10] [10 2] [2 10] [7 10] [14 9] [5 10] [19 6] [10 4] [2 1] [7 8] [4 1] [16 8] [5 11] [0 12] [15 1] [14 6] [5 8] [3 8] [7 11] [9 10] [6 10] [1 6] [15 5] [14 8] [22 3] [16 5] [17 11] [13 6] [21 1] [17 3] [22 12] [8 10] [4 12] [10 5] [9 12] [11 6] [13 4] [20 2] [7 3] [21 3] [2 8] [8 9] [20 1] [21 4] [23 11] [13 7] [10 8] [6 2] [20 11] [8 2] [19 11] [20 3] [9 2] [17 10]},
                  :npcs {[5 4] 1 [6 8] 3},
                  :connection-triggers {[20 0] 2}}
               2 {:id 2
                  :position-id 17
                  :display-name "Park - Entrance"
                  :dimension [[0 0] [2 2]]
                  :background {[0 1] :wall, [1 2] :wall, [0 0] :wall, [2 2] :wall, [0 2] :wall, [1 1] :grass, [2 1] :grass, [1 0] :wall, [2 0] :wall}
                  :walk-set #{[1 1] [2 1]}
                  :connection-triggers {}}}
   :location-connections #{#{1 2}}
   :characters {1 {:id 1 :display-name "Hugo" :color "rgba(255, 0, 0, .6)" :texture :hugo}
                3 {:id 3 :display-name "Gustav" :color "rgba(92, 154, 9, 0.8)" :texture :gustav}}
   :dialogues {1 {:id 1 :description "Hugo's Dialogue" :initial-line-id 1 :location-id 1}
               2 {:id 2 :description "Gustav's Dialogue" :initial-line-id 14 :location-id 1}}
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
