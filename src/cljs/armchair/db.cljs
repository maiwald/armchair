(ns armchair.db
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.set :refer [subset?]]
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
(s/def ::dragging (s/keys :req-un [::cursor-start ::position-ids]))
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
  {:dialogues {1 {:id 1, :description "Hugo's Dialogue", :character-id 1, :initial-line-id 1, :location-id 1}, 2 {:id 2, :description "Gustav's Dialogue", :character-id 3, :initial-line-id 14, :location-id 1}, 3 {:id 3, :initial-line-id 19, :location-id 3, :character-id 4, :description "Conni im Erdgeschoss"}, 4 {:id 4, :initial-line-id 20, :location-id 3, :character-id 6, :description "Grogg erzählt dir was"}, 5 {:id 5, :initial-line-id 39, :location-id 2, :character-id 7, :description "Stay off the lawn!"}, 6 {:id 6, :initial-line-id 40, :location-id 1, :character-id 8, :description "Zeolith asks an important question"}}, :location-editor {:tool :npcs-select, :painting? false, :active-texture :stairs}, :characters {1 {:id 1, :display-name "Hugo", :color "rgba(255, 0, 0, .6)", :texture :hugo}, 3 {:id 3, :display-name "Gustav", :color "rgba(92, 154, 9, 0.8)", :texture :gustav}, 4 {:id 4, :color "blue", :display-name "Conni", :texture :rourke}, 6 {:id 6, :color "magenta", :display-name "Grog the Green", :texture :agent}, 7 {:id 7, :color "brown", :display-name "Lawn Monitor", :texture :goth_idle}, 8 {:id 8, :color "grey", :display-name "Zeolith", :texture :dead_squirrel_idle}}, :lines {32 {:id 32, :kind :player, :dialogue-id 4, :position-id 49, :options [{:text "My man!", :next-line-id nil}]}, 1 {:id 1, :kind :npc, :character-id 1, :dialogue-id 1, :position-id 1, :text "Hey, who are you?", :next-line-id 2}, 33 {:id 33, :kind :player, :dialogue-id 3, :position-id 50, :options [{:text "\"You nod, while you walk on by, raising your fist\"", :next-line-id 34} {:text "Hey, Ho! Thanks a Lot!", :next-line-id 35} {:text "My Man!", :next-line-id 36}]}, 2 {:id 2, :dialogue-id 1, :position-id 2, :kind :player, :options [{:text "I could ask you the same.", :next-line-id 3} {:text "My name does not matter.", :next-line-id 4} {:text "Silence! Hugo, you must come with me at once! The fate of the world is at stake.", :next-line-id 17, :required-info-ids #{1}}]}, 34 {:id 34, :kind :npc, :character-id 4, :dialogue-id 3, :position-id 51, :text "\"Conni raises his fist and bumbs your fist. \"Cool guy\" he mutters\"", :next-line-id nil}, 3 {:id 3, :kind :npc, :dialogue-id 1, :character-id 1, :position-id 3, :text "I am Hugo. And you?", :next-line-id 7, :info-ids #{1}}, 35 {:id 35, :kind :npc, :character-id 4, :dialogue-id 3, :position-id 52, :text "The entry went up by 500 Gold just now", :next-line-id 37}, 4 {:id 4, :kind :npc, :dialogue-id 1, :character-id 1, :position-id 4, :text "Fine, be a jerk.", :next-line-id nil, :info-ids #{}}, 36 {:id 36, :kind :npc, :character-id 4, :dialogue-id 3, :position-id 53, :text "My Man!", :next-line-id nil}, 5 {:id 5, :kind :npc, :dialogue-id 1, :character-id 1, :position-id 5, :text "What a strange coincidence! Two Hugos. Who would have thought.", :next-line-id 6}, 37 {:id 37, :kind :player, :dialogue-id 3, :position-id 54, :options [{:text "WWHHHY!", :next-line-id nil}]}, 6 {:id 6, :dialogue-id 1, :kind :npc, :character-id 1, :position-id 6, :text "Anyway, ...bye!", :next-line-id nil, :info-ids #{}}, 7 {:id 7, :dialogue-id 1, :position-id 7, :kind :player, :options [{:text "I am also Hugo! But for the sake of testing I keep talking way beyond what could possible fit into this box.", :next-line-id 5} {:text "That's none of your business!", :next-line-id 4}]}, 39 {:id 39, :kind :npc, :character-id 7, :dialogue-id 5, :position-id 57, :text "Stay off the lawn!", :next-line-id nil}, 40 {:id 40, :kind :npc, :character-id 8, :dialogue-id 6, :position-id 58, :text "Good morning fine Sir! It is a beautiful day to push the button isn't it?", :next-line-id 41}, 41 {:id 41, :kind :player, :dialogue-id 6, :position-id 59, :options [{:text "What the hell you talking bout?", :next-line-id 43} {:text "Yes, indeed…..quite.", :next-line-id 43} {:text "Man! Why you gotta push that button. Man. For Real!", :next-line-id 43}]}, 42 {:id 42, :kind :player, :dialogue-id 6, :position-id 60, :options [{:text "WHHHYYY!", :next-line-id nil}]}, 43 {:id 43, :kind :npc, :character-id 8, :dialogue-id 6, :position-id 62, :text "\"Zeolith's hand hovers over the buttin. A satisfied smirk crosses his face.\"", :next-line-id 42}, 14 {:id 14, :character-id 3, :kind :npc, :dialogue-id 2, :position-id 18, :text "Yes?", :next-line-id 15}, 15 {:id 15, :dialogue-id 2, :kind :player, :position-id 19, :options [{:text "Who are you?", :next-line-id 16}]}, 16 {:id 16, :kind :npc, :character-id 3, :dialogue-id 2, :position-id 20, :text "I am Gustav!", :next-line-id nil}, 17 {:id 17, :kind :npc, :character-id 1, :dialogue-id 1, :position-id 21, :text "Whaaaaaaaaaaat!?", :next-line-id 18}, 18 {:id 18, :kind :npc, :character-id 1, :dialogue-id 1, :position-id 22, :text "How do you know my name!?", :next-line-id nil}, 19 {:id 19, :kind :npc, :character-id 4, :dialogue-id 3, :position-id 24, :text "Alright you can go in.", :next-line-id 33}, 20 {:id 20, :kind :npc, :character-id 6, :dialogue-id 4, :position-id 25, :text "Hey my friend. I see you finally got in the club.", :next-line-id 21}, 21 {:id 21, :kind :player, :dialogue-id 4, :position-id 26, :options [{:text "Yea bitch, took a lot of work man!", :next-line-id 22} {:text "Thank you kind sir, it was a hassle of epic proportions.", :next-line-id 23} {:text "Fuck you!", :next-line-id 24}]}, 22 {:id 22, :kind :npc, :character-id 6, :dialogue-id 4, :position-id 27, :text "You say it!", :next-line-id 27}, 23 {:id 23, :kind :npc, :character-id 6, :dialogue-id 4, :position-id 28, :text "Oh, I see you pretentious fuck. Take a hike!", :next-line-id nil}, 24 {:id 24, :kind :npc, :character-id 6, :dialogue-id 4, :position-id 29, :text "You said it good sir! Alright, how many weapons do you need?", :next-line-id 25}, 25 {:id 25, :kind :player, :dialogue-id 4, :position-id 30, :options [{:text "I don't know. How many weapons does one need, in order to rob a bank?", :next-line-id 28} {:text "I don't know. I need a realiable weapon for hunting. ", :next-line-id 30} {:text "Gimme that Glock!", :next-line-id 31}]}, 27 {:id 27, :kind :player, :dialogue-id 4, :position-id 32, :options [{:text "I need weapons for a bank heist!", :next-line-id 28}]}, 28 {:id 28, :kind :npc, :character-id 6, :dialogue-id 4, :position-id 33, :text "Oh Jessus!", :next-line-id 29}, 29 {:id 29, :kind :player, :dialogue-id 4, :position-id 34, :options [{:text "WHHHYY!", :next-line-id nil}]}, 30 {:id 30, :kind :npc, :character-id 6, :dialogue-id 4, :position-id 35, :text "Here that this hunting rifle. It'll get the \"job\" done ;)", :next-line-id 32}, 31 {:id 31, :kind :npc, :character-id 6, :dialogue-id 4, :position-id 36, :text "Alright player, calm done. Here you go.", :next-line-id 32}}, :infos {1 {:id 1, :description "Hugo's Name is Hugo"}}, :locations {1 {:id 1, :position-id 16, :display-name "Park - Camp", :dimension [[0 0] [24 13]], :background {[18 7] :stone_2, [13 2] :red_wall_top-right, [0 1] :wall, [18 13] :wall, [1 10] :stone_2, [14 0] :wall, [10 9] :stone_2, [6 8] :stone_grass_bottom-left, [13 11] :grass_stone_bottom, [6 9] :stone, [5 7] :stone_2, [18 8] :house_roof_top-right, [5 9] :stone_2, [21 9] :grass, [20 10] :wall, [0 10] :wall, [0 8] :wall, [16 13] :wall, [21 7] :grass, [9 1] :grass_dirt_top, [8 13] :wall, [4 3] :house_roof_top-left, [17 1] :grass, [18 9] :house_roof_bottom-right, [4 11] :grass_stone_bottom, [18 2] :grass, [14 10] :stone_grass_top-left, [3 4] :dirt, [1 2] :grass_dirt_left, [0 5] :wall, [0 11] :stone, [19 3] :wall, [0 0] :wall, [9 7] :grass, [5 12] :grass, [5 3] :house_roof_top, [24 7] :wall, [13 13] :wall, [23 8] :grass, [2 2] :dirt, [16 2] :wall, [22 4] :grass_stone_right, [1 5] :grass, [3 2] :dirt, [4 6] :house_bottom_left, [16 11] :house_bottom_left, [21 5] :wall, [8 12] :grass, [24 3] :wall, [9 11] :wall, [8 5] :house_roof_bottom-right, [13 10] :stone, [14 12] :grass, [1 11] :stone, [7 1] :grass_dirt_top-left, [0 7] :wall, [21 2] :stone_2, [7 9] :stone, [20 9] :grass, [2 4] :grass_dirt_bottom, [13 12] :grass, [23 3] :grass, [21 11] :stone, [2 12] :stone, [10 1] :grass_dirt_top, [23 7] :grass, [13 9] :stone, [22 2] :wall, [10 6] :grass, [7 2] :dirt, [4 2] :dirt, [21 0] :wall, [12 6] :grass, [4 10] :stone, [12 2] :red_wall-top-left, [24 9] :wall, [1 3] :grass_dirt_left, [10 11] :wall, [22 1] :grass_stone_right, [19 8] :wall, [5 5] :house_roof_bottom, [2 13] :wall, [5 6] :house_door_bottom, [2 3] :dirt, [15 12] :grass, [11 3] :wall, [8 1] :grass_dirt_top, [14 3] :grass, [18 12] :grass, [11 7] :grass, [0 13] :wall, [11 9] :stone_2, [23 1] :grass, [21 13] :wall, [0 9] :wall, [9 6] :grass, [15 3] :grass, [5 2] :grass_dirt_top, [16 7] :stone_2, [23 6] :grass, [9 3] :dirt, [9 0] :wall, [2 11] :stone, [12 0] :wall, [17 6] :grass_stone_top, [12 13] :wall, [4 7] :grass_stone_left, [8 3] :dirt, [3 1] :grass_dirt_top, [18 6] :stone_grass_bottom-right, [17 2] :grass, [14 11] :grass_stone_bottom-right, [22 9] :grass, [19 10] :wall, [18 11] :house_bottom_right, [9 13] :wall, [4 13] :wall, [21 10] :grass_stone_top, [7 0] :wall, [12 7] :grass, [17 9] :stairs, [20 6] :stone_2, [0 2] :wall, [15 2] :wall, [2 9] :wall, [19 9] :grass, [18 1] :grass, [4 9] :stone_grass_bottom-right, [12 9] :stone_2, [15 8] :stone_2, [24 4] :wall, [7 13] :wall, [5 1] :wall, [17 8] :house_roof_top, [20 4] :stone_2, [20 5] :stone_2, [19 4] :wall, [3 0] :wall, [14 2] :grass, [2 7] :house_roof_bottom-left, [12 12] :grass, [13 5] :house_door_bottom, [11 4] :wall, [6 13] :wall, [11 10] :stone, [15 4] :red_wall_top-right, [20 0] :stone_2, [21 8] :wall, [16 4] :grass, [22 10] :wall, [18 4] :grass, [13 8] :grass_stone_top, [0 6] :wall, [6 12] :grass, [8 8] :wall, [4 5] :house_roof_bottom-left, [16 3] :grass, [24 11] :wall, [20 8] :wall, [1 1] :grass_dirt_top-left, [15 7] :stone_grass_bottom-right, [23 9] :grass, [15 9] :stone_2, [23 13] :wall, [6 0] :wall, [15 11] :grass, [10 10] :stone, [12 4] :red_wall-left, [12 3] :red_wall-left, [10 7] :grass, [8 7] :grass, [12 5] :red_wall-bottom-left, [16 12] :grass, [9 5] :grass, [24 1] :wall, [19 5] :wall, [7 4] :house_roof_top-left, [11 12] :wall, [14 4] :red_wall-top-left, [1 12] :stone, [18 3] :grass, [14 13] :wall, [8 4] :house_roof_top-right, [13 1] :house_roof_bottom-right, [12 8] :grass_stone_top, [7 7] :grass, [22 6] :wall, [16 10] :house_roof_middle-left, [16 9] :house_roof_bottom-left, [16 1] :grass, [3 3] :dirt, [8 11] :wall, [14 7] :grass_stone_top-left, [20 12] :wall, [5 0] :wall, [12 10] :stone, [23 10] :wall, [11 13] :wall, [15 6] :grass_stone_top-left, [10 12] :grass, [18 5] :wall, [5 4] :house_roof_middle, [13 0] :wall, [21 12] :stone, [3 11] :stone_grass_top-left, [15 0] :wall, [2 6] :house_roof_middle-left, [17 4] :wall, [1 13] :wall, [18 0] :wall, [6 11] :grass_stone_bottom, [14 1] :grass, [10 3] :dirt, [9 9] :stone, [24 0] :wall, [20 7] :stone_grass_top-left, [7 12] :grass, [11 8] :grass_stone_top, [16 6] :grass_stone_top, [9 4] :dirt, [17 7] :stone_2, [19 12] :grass, [22 11] :stone, [6 3] :house_roof_top-right, [23 12] :stone, [21 6] :stone_grass_top-left, [22 8] :wall, [14 5] :red_wall-bottom-left, [3 5] :house_roof_top-right, [22 7] :wall, [8 6] :house_bottom_right, [22 13] :wall, [11 0] :wall, [3 9] :wall, [11 11] :wall, [6 6] :house_bottom_right, [15 10] :grass_stone_bottom, [17 12] :grass, [1 4] :grass_dirt_bottom-left, [19 1] :wall, [23 5] :grass, [12 1] :house_roof_bottom-left, [6 7] :grass_stone_right, [1 7] :grass, [4 8] :grass_stone_left, [24 12] :wall, [1 8] :grass, [24 13] :wall, [11 1] :wall, [19 13] :wall, [11 5] :wall, [23 4] :grass, [19 0] :wall, [19 7] :stone_2, [13 3] :red_wall-right, [0 3] :wall, [3 12] :grass_stone_right, [16 0] :wall, [24 10] :wall, [3 10] :stone, [6 5] :house_roof_bottom-right, [1 9] :wall, [24 6] :wall, [20 13] :wall, [10 2] :dirt, [2 10] :stone, [19 2] :wall, [7 10] :stone_2, [14 9] :stone, [5 10] :stone, [19 6] :stone_2, [10 4] :dirt, [2 1] :grass_dirt_top, [7 8] :grass_stone_top, [4 1] :grass_dirt_top-right, [22 5] :wall, [16 8] :house_roof_top-left, [5 11] :grass_stone_bottom, [24 5] :wall, [7 5] :house_roof_bottom-left, [3 6] :house_roof_middle-right, [0 12] :stone, [17 0] :wall, [22 0] :wall, [24 2] :wall, [3 13] :wall, [15 1] :grass, [14 6] :grass, [5 8] :stone_2, [3 8] :grass, [6 1] :wall, [7 11] :grass_stone_bottom, [8 0] :wall, [9 10] :stone, [6 10] :stone, [1 6] :grass, [15 5] :red_wall-bottom-right, [10 0] :wall, [4 4] :house_roof_middle-left, [14 8] :stone_grass_bottom-right, [22 3] :grass_stone_right, [16 5] :grass, [17 11] :stairs, [13 6] :grass, [21 1] :stone_2, [17 3] :grass, [22 12] :stone, [5 13] :wall, [3 7] :house_roof_bottom-right, [1 0] :wall, [9 8] :wall, [8 10] :stone_2, [4 12] :grass, [18 10] :house_roof_middle-right, [10 5] :grass, [9 12] :grass, [11 6] :grass, [13 4] :red_wall-right, [20 2] :stone_2, [7 3] :grass_dirt_left, [21 3] :stone_2, [2 0] :wall, [2 8] :grass, [11 2] :wall, [8 9] :stone, [15 13] :wall, [23 2] :wall, [20 1] :stone_2, [4 0] :wall, [21 4] :stone_2, [17 5] :wall, [23 11] :stone, [12 11] :wall, [13 7] :grass, [10 8] :grass_stone_top, [24 8] :wall, [6 2] :grass_dirt_top, [20 11] :grass_stone_left, [8 2] :dirt, [19 11] :grass, [6 4] :house_roof_middle-right, [20 3] :stone_2, [2 5] :house_roof_top-left, [9 2] :dirt, [17 13] :wall, [0 4] :wall, [7 6] :house_bottom_left, [10 13] :wall, [23 0] :wall, [17 10] :house_roof_middle}, :walk-set #{[18 7] [1 10] [10 9] [6 8] [13 11] [6 9] [5 7] [5 9] [21 9] [21 7] [9 1] [17 1] [4 11] [18 2] [14 10] [3 4] [1 2] [0 11] [9 7] [5 12] [23 8] [2 2] [22 4] [1 5] [3 2] [8 12] [13 10] [14 12] [1 11] [7 1] [21 2] [7 9] [20 9] [2 4] [13 12] [23 3] [21 11] [2 12] [10 1] [23 7] [13 9] [10 6] [7 2] [4 2] [12 6] [4 10] [1 3] [22 1] [5 6] [2 3] [15 12] [8 1] [14 3] [18 12] [11 7] [11 9] [23 1] [9 6] [15 3] [5 2] [16 7] [23 6] [9 3] [2 11] [17 6] [4 7] [8 3] [3 1] [18 6] [17 2] [14 11] [22 9] [21 10] [12 7] [17 9] [20 6] [19 9] [18 1] [4 9] [12 9] [15 8] [20 4] [20 5] [14 2] [12 12] [13 5] [11 10] [20 0] [16 4] [18 4] [13 8] [6 12] [16 3] [1 1] [15 7] [23 9] [15 9] [15 11] [10 10] [10 7] [8 7] [16 12] [9 5] [1 12] [18 3] [12 8] [7 7] [16 1] [3 3] [14 7] [12 10] [15 6] [10 12] [21 12] [3 11] [6 11] [14 1] [10 3] [9 9] [20 7] [7 12] [11 8] [16 6] [9 4] [17 7] [19 12] [22 11] [23 12] [21 6] [15 10] [17 12] [1 4] [23 5] [6 7] [1 7] [4 8] [1 8] [23 4] [19 7] [3 12] [3 10] [10 2] [2 10] [7 10] [14 9] [5 10] [19 6] [10 4] [2 1] [7 8] [4 1] [5 11] [0 12] [15 1] [14 6] [5 8] [3 8] [7 11] [9 10] [6 10] [1 6] [14 8] [22 3] [16 5] [17 11] [13 6] [21 1] [17 3] [22 12] [8 10] [4 12] [10 5] [9 12] [11 6] [20 2] [7 3] [21 3] [2 8] [8 9] [20 1] [21 4] [23 11] [13 7] [10 8] [6 2] [20 11] [8 2] [19 11] [20 3] [9 2] [17 10]}, :npcs {[7 11] 1, [6 7] 3, [17 8] 8}, :connection-triggers {[5 6] 3, [0 12] 2}}, 2 {:id 2, :position-id 17, :display-name "Park - Entrance", :dimension [[-4 -5] [7 6]], :background {[0 1] :grass, [2 -3] :red_wall_top-right, [7 -2] :stone_2, [4 3] :grass_stone_right, [1 -4] :house_roof_bottom, [-3 -2] :grass, [5 -5] :grass, [3 4] :stone_grass_top-left, [1 2] :stone_grass_bottom-right, [0 5] :stone_2, [0 0] :grass, [-1 1] :grass, [4 -2] :grass, [5 3] :grass, [0 -5] :house_roof_middle, [2 2] :stone_2, [-2 5] :wall, [1 5] :stone_2, [0 -3] :red_wall-top, [1 -2] :red_wall-center, [3 2] :stone_2, [4 6] :grass, [-1 -3] :red_wall-top, [7 1] :grass_stone_bottom-right, [-4 -2] :wall, [2 4] :stone_2, [-1 3] :grass, [-2 3] :grass, [6 -2] :stone_2, [-4 3] :wall, [7 2] :grass, [4 2] :stone_grass_top-left, [1 3] :stone_2, [5 5] :wall, [5 6] :grass, [2 3] :stone_2, [-2 -3] :red_wall-top-left, [-3 5] :wall, [-2 0] :grass, [-4 5] :wall, [5 2] :grass_stone_bottom, [-1 5] :wall, [7 -3] :stone_grass_bottom-right, [3 1] :stone_grass_bottom-right, [7 0] :stone_grass_top-left, [-3 -1] :grass, [-4 1] :wall, [-4 0] :wall, [0 2] :grass_stone_top-left, [4 -4] :grass, [-3 4] :grass, [7 -4] :grass_stone_top-left, [7 -5] :grass, [5 1] :stone_2, [3 0] :grass_stone_top-left, [2 -4] :house_roof_bottom-right, [-4 2] :wall, [5 -1] :stone_grass_bottom-right, [0 6] :stone_2, [-3 1] :grass, [4 -3] :grass, [-2 -2] :red_wall-left, [-1 6] :grass_stone_left, [4 5] :wall, [0 -4] :house_roof_bottom, [-1 -5] :house_roof_middle, [1 1] :grass_stone_top-left, [6 0] :stone_2, [7 -1] :stone_2, [-2 -4] :house_roof_bottom-left, [-4 4] :wall, [3 -2] :grass, [7 4] :grass, [6 -5] :grass, [-3 -5] :grass, [-3 -3] :grass, [-1 2] :grass, [-1 -4] :house_roof_bottom, [3 3] :stone_2, [4 -1] :grass_stone_top-left, [-1 4] :grass_stone_top-left, [5 0] :stone_2, [-1 -2] :red_wall-center, [-3 6] :grass, [-2 1] :grass, [5 4] :grass, [2 6] :stone_2, [-3 0] :grass, [-4 -5] :wall, [-2 2] :grass, [-2 4] :grass, [5 -2] :grass_stone_left, [3 -4] :grass, [6 3] :grass, [3 5] :wall, [-4 -3] :wall, [3 -5] :grass, [2 -2] :red_wall-right, [-4 -4] :wall, [3 -3] :grass, [6 6] :grass, [6 -3] :grass_stone_top, [2 -5] :house_roof_middle-right, [1 4] :stone_2, [6 -1] :stone_2, [1 -5] :house_roof_middle, [4 -5] :grass, [-4 6] :grass, [5 -3] :grass_stone_top-left, [0 3] :grass_stone_left, [1 -3] :red_wall-top, [6 5] :wall, [5 -4] :grass, [-3 2] :grass, [2 1] :grass_stone_top, [6 -4] :grass, [4 1] :stone_2, [7 5] :wall, [3 6] :grass_stone_right, [6 1] :stone_grass_top-left, [1 6] :stone_2, [2 -1] :red_wall-bottom-right, [-3 3] :grass, [3 -1] :grass, [1 -1] :red_wall-bottom, [4 4] :grass_stone_bottom-right, [-2 6] :grass, [-2 -1] :red_wall-bottom-left, [1 0] :grass, [-2 -5] :house_roof_middle-left, [-3 -4] :grass, [7 3] :grass, [0 -2] :house_arch_top, [-4 -1] :wall, [2 0] :grass, [-1 0] :grass, [4 0] :stone_grass_bottom-right, [-1 -1] :red_wall-bottom, [6 2] :grass_stone_bottom-right, [6 4] :grass, [2 5] :stone_2, [0 4] :stone_grass_bottom-right, [7 6] :grass, [0 -1] :house_door_bottom}, :walk-set #{[7 -2] [4 3] [3 4] [1 2] [0 5] [0 0] [-1 1] [2 2] [1 5] [3 2] [2 4] [6 -2] [4 2] [1 3] [2 3] [-3 5] [-2 0] [5 2] [7 -3] [3 1] [7 0] [-3 4] [5 1] [5 -1] [0 6] [-3 1] [-1 6] [6 0] [7 -1] [3 3] [5 0] [-3 6] [-2 1] [2 6] [-2 2] [5 -2] [6 -3] [1 4] [6 -1] [0 3] [-3 2] [2 1] [4 1] [3 6] [6 1] [1 6] [-3 3] [-2 6] [-1 0] [4 0] [2 5] [0 4]}, :connection-triggers {}, :npcs {[0 5] 7}}, 3 {:id 3, :dimension [[-9 -4] [2 8]], :background {[-8 3] :wall, [0 1] :stone, [2 -3] :wall, [-6 4] :stone, [-7 7] :stone, [-5 -1] :grass_dirt_bottom, [-7 -3] :grass_dirt_top, [0 8] :wall, [-6 7] :stone, [-5 4] :wall, [-8 2] :stone, [1 -4] :wall, [-3 -2] :dirt, [1 2] :stone, [0 5] :stone, [-9 2] :wall, [0 0] :red_wall-center, [-9 1] :wall, [-1 1] :stone, [-1 7] :stone, [2 2] :wall, [-2 5] :house_roof_top, [-8 -3] :grass_dirt_top-left, [1 5] :stone, [0 -3] :grass_dirt_top, [1 -2] :grass_dirt_right, [-6 8] :wall, [-6 3] :wall, [-1 -3] :grass_dirt_top, [-5 1] :stone, [-4 -2] :dirt, [0 7] :stone, [-5 -2] :dirt, [2 4] :wall, [-1 3] :wall, [-2 3] :wall, [-4 3] :wall, [1 3] :wall, [-7 6] :stone, [2 3] :wall, [-2 -3] :grass_dirt_top, [-6 -2] :dirt, [-5 2] :stone, [-7 -2] :dirt, [-3 5] :house_roof_top, [-2 0] :red_wall-center, [-4 5] :house_roof_top, [-1 5] :stone, [-7 -4] :wall, [-3 -1] :grass_dirt_bottom, [-4 1] :stone, [-5 6] :wall, [-4 0] :red_wall-center, [-8 5] :stone, [0 2] :stone, [-3 4] :stone, [-8 0] :red_wall-center, [-9 4] :wall, [2 7] :wall, [2 -4] :wall, [-4 2] :stone, [-9 3] :wall, [0 6] :stone_bush, [-3 1] :stone, [-2 -2] :dirt, [-1 6] :stone, [0 -4] :wall, [-8 6] :stone, [1 1] :stone, [-2 -4] :wall, [-4 4] :stone_2, [-6 -3] :grass_dirt_top, [-3 -3] :grass_dirt_top, [-7 -1] :dirt, [-1 2] :stone, [-1 -4] :wall, [-8 -4] :wall, [-1 4] :stone, [-8 7] :stone, [-6 1] :stone, [-6 6] :stone, [-1 -2] :dirt, [-9 -1] :wall, [-9 -2] :wall, [-2 8] :wall, [-3 6] :stone, [-2 1] :stone, [-4 7] :stone, [2 6] :wall, [-9 0] :wall, [-3 0] :red_wall-center, [-5 -4] :wall, [-8 -2] :grass_dirt_left, [-5 5] :wall, [-5 -3] :grass_dirt_top, [-5 7] :wall, [-2 2] :stone, [-2 4] :stone, [-1 8] :stone, [-9 7] :wall, [-4 -3] :grass_dirt_top, [2 -2] :wall, [-4 -4] :wall, [-9 8] :wall, [1 4] :stone_2, [-7 2] :stone, [-7 3] :house_door_bottom, [1 7] :stone, [-7 1] :stone_2, [1 8] :wall, [-4 6] :stone, [-6 0] :red_wall-center, [-3 7] :stone, [0 3] :stone_2, [1 -3] :grass_dirt_top-right, [-9 5] :wall, [-6 -1] :grass_dirt_bottom, [-9 -3] :wall, [-7 8] :wall, [-5 3] :wall, [-8 1] :stone, [-3 2] :stone, [2 1] :wall, [-4 8] :wall, [-6 -4] :wall, [-5 8] :wall, [-8 4] :stone, [1 6] :stone, [2 -1] :wall, [-3 3] :wall, [1 -1] :grass_dirt_bottom-right, [-2 6] :stone, [-7 5] :stone, [-2 -1] :grass_dirt_bottom, [1 0] :red_wall-center, [-7 0] :stairs, [-3 -4] :wall, [0 -2] :dirt, [-4 -1] :grass_dirt_bottom, [2 0] :wall, [2 8] :wall, [-7 4] :stone, [-1 0] :red_wall-center, [-6 5] :stone, [-9 -4] :wall, [-2 7] :stone, [-1 -1] :grass_dirt_bottom, [-8 -1] :grass_dirt_bottom-left, [-9 6] :wall, [-3 8] :wall, [-8 8] :wall, [2 5] :wall, [-6 2] :stone, [0 4] :stone_2, [-5 0] :red_wall-center, [0 -1] :grass_dirt_bottom}, :walk-set #{[0 1] [-6 4] [-7 7] [-5 -1] [-7 -3] [-6 7] [-8 2] [-3 -2] [1 2] [0 5] [-1 1] [-1 7] [-8 -3] [1 5] [0 -3] [1 -2] [-1 -3] [-5 1] [-4 -2] [0 7] [-5 -2] [-7 6] [-2 -3] [-6 -2] [-5 2] [-7 -2] [-1 5] [-3 -1] [-4 1] [-8 5] [0 2] [-3 4] [-4 2] [0 6] [-3 1] [-2 -2] [-1 6] [-8 6] [1 1] [-4 4] [-6 -3] [-3 -3] [-7 -1] [-1 2] [-1 4] [-8 7] [-6 1] [-6 6] [-1 -2] [-3 6] [-2 1] [-4 7] [-8 -2] [-5 -3] [-2 2] [-2 4] [-1 8] [-4 -3] [1 4] [-7 2] [-7 3] [1 7] [-7 1] [-4 6] [-3 7] [0 3] [1 -3] [-6 -1] [-8 1] [-3 2] [-8 4] [1 6] [1 -1] [-2 6] [-7 5] [-2 -1] [-7 0] [0 -2] [-4 -1] [-7 4] [-6 5] [-2 7] [-1 -1] [-8 -1] [-6 2] [0 4] [0 -1]}, :connection-triggers {}, :display-name "Taverne Erdgeschoss", :position-id 23, :npcs {[-6 -2] 6, [-1 4] 4}}}, :location-connections #{#{1 2} #{3 1}}, :current-page "/locations", :positions {32 [123 147], 1 [-98 266], 33 [483 169], 2 [187 257], 34 [855 179], 3 [461 230], 35 [491 462], 4 [1041 429], 36 [491 625], 5 [1049 208], 37 [375 147], 6 [1330 216], 38 [20 20], 7 [768 209], 39 [20 20], 40 [20 20], 41 [20 20], 42 [20 20], 43 [20 20], 44 [20 20], 45 [20 20], 46 [20 20], 47 [20 20], 16 [368 270], 48 [20 20], 17 [191 397], 49 [854 436], 18 [107 151], 50 [180 152], 19 [407 151], 51 [501 82], 20 [707 151], 52 [496 252], 21 [737 561], 53 [490 417], 22 [1048 559], 54 [819 259], 23 [618 133], 55 [1001 227], 24 [-251 163], 56 [20 20], 25 [-776 174], 57 [99 144], 26 [-487 176], 58 [-361 201], 27 [-180 160], 59 [-54 213], 28 [-174 311], 60 [720 329], 29 [-164 498], 61 [268 213], 30 [155 415], 62 [368 236], 31 [557 526]}})

(when-not (s/valid? ::state default-db)
  (js/console.log "Default DB state explain:")
  (s/explain ::state default-db))
