(ns armchair.db
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.set :refer [subset?]]
            [armchair.util :refer [where-map]]))

;; Types

(s/def ::id pos-int?)
(s/def ::text #(not (string/blank? %)))
(s/def ::position (s/tuple integer? integer?))
(s/def ::entity-map (s/every (fn [[k v]] (= k (:id v)))))
(s/def ::undirected-connection (s/coll-of ::id :kind set? :count 2))

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
(s/def ::name ::text)
(s/def ::payload some?)
(s/def ::current-page (s/keys :req-un [::name] :opt-un [::payload]))

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

(s/def ::location (s/keys :req-un [::id ::position-id ::display-name]))
(s/def ::locations (s/and ::entity-map
                          (s/map-of ::location-id ::location)))
(s/def ::location-connections (s/coll-of ::undirected-connection :kind set?))

(s/def ::character (s/keys :req-un [::id ::display-name ::color]))
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
                                       ::locations
                                       ::location-connections]
                              :opt-un [::connecting ::dragging ::pointer])))

(def default-db
  {:current-page {:name "Game"}
   :positions {1 [65 221]
               2 [335 218]
               3 [624 185]
               4 [1212 365]
               5 [1193 175]
               6 [1493 171]
               7 [916 174]
               16 [229 198]
               17 [259 91]
               18 [107 151]
               19 [407 151]
               20 [707 151]}
   :locations {1 {:id 1
                  :position-id 16
                  :display-name "Park - Camp"
                  :level [[0 0 0 0 0 0 0 0 0 0 0 0 1 0]
                          [0 1 0 1 1 1 1 1 1 1 1 1 1 0]
                          [0 1 1 1 0 0 0 0 0 0 1 1 0 0]
                          [0 1 1 1 0 0 0 0 0 0 1 1 1 0]
                          [0 1 0 1 0 0 1 1 1 1 1 0 1 0]
                          [0 0 0 1 1 1 1 0 0 1 0 0 1 0]
                          [0 0 1 1 0 0 1 0 0 1 0 1 1 0]
                          [0 1 1 1 1 0 1 0 0 1 1 1 1 0]
                          [0 1 0 1 1 0 1 1 1 1 0 0 1 0]
                          [0 1 0 1 1 0 1 1 1 1 0 0 1 0]
                          [0 1 0 1 1 0 1 1 1 1 0 0 1 0]
                          [0 0 0 0 1 0 1 0 0 1 0 0 0 0]
                          [0 1 0 1 1 1 1 1 1 1 1 0 1 0]
                          [0 1 1 1 0 0 0 0 0 0 1 1 1 0]
                          [0 1 1 1 0 0 0 0 0 0 1 1 1 0]
                          [0 1 0 1 0 0 1 1 1 1 1 0 1 0]
                          [0 1 0 1 1 1 1 0 0 1 0 0 1 0]
                          [0 1 1 1 0 0 1 0 0 1 0 1 1 0]
                          [0 1 1 1 1 0 1 0 0 1 0 1 1 0]
                          [0 0 0 0 0 0 1 0 0 1 0 1 1 0]
                          [1 1 1 1 1 0 1 1 0 1 0 1 0 0]
                          [0 1 1 1 1 0 1 1 0 1 1 1 0 0]
                          [0 1 0 1 1 0 1 0 0 1 0 1 0 0]
                          [0 1 0 1 1 1 1 1 1 1 0 1 1 0]
                          [0 0 0 0 0 0 0 0 0 0 0 0 0 0]]
                  :enemies {1 [6 6]
                            3 [5 12]}}
               2 {:id 2
                  :position-id 17
                  :display-name "Park - Entrance"}}
   :location-connections #{#{1 2}}
   :characters {1 {:id 1 :display-name "Hugo" :color "rgba(255, 0, 0, .6)"}
                3 {:id 3 :display-name "Gustav" :color "rgba(92, 154, 9, 0.8)"}}
   :dialogues {1 {:id 1 :display-name "Hugo's Dialogue" :initial-line-id 1 :location-id 1}
               2 {:id 2 :display-name "Gustav's Dialogue" :initial-line-id 14 :location-id 1}}
   :infos {1 {:id 1 :description "A secret Fact"}
           2 {:id 2 :description "Another secret Fact"}}
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
                        {:text "My name does not matter." :next-line-id 4}]}
           3 {:id 3
              :kind :npc
              :dialogue-id 1
              :character-id 1
              :position-id 3
              :text "I am Hugo. And you?"
              :next-line-id 7}
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
               :next-line-id nil}}})

(when-not (s/valid? ::state default-db)
  (.log js/console "Default DB state explain:")
  (s/explain ::state default-db))

(defn line-count-for-character [lines character-id]
  (let [filter-fn #(= (:character-id %) character-id)]
    (->> lines vals (filter filter-fn) count)))
