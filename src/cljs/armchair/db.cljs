(ns armchair.db
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.set :refer [subset?]]))

(s/def ::position (s/tuple integer? integer?))

(s/def ::id pos-int?)
(s/def ::character-id ::id)
(s/def ::dialogue-id ::id)
(s/def ::line-id ::id)
(s/def ::initial-line-id ::line-id)
(s/def ::location-id ::id)
(s/def ::position-id ::id)

(s/def ::entity-map (s/every (fn [[k v]] (= k (:id v)))))

(s/def ::directed-connection (s/tuple ::id ::id))
(s/def ::undirected-connection (s/coll-of ::id :kind set? :count 2))

(s/def ::text #(not (string/blank? %)))
(s/def ::display-name ::text)
(s/def ::color ::text)

(s/def ::pointer ::position)

(s/def ::location (s/keys :req-un [::id ::position-id ::display-name]))
(s/def ::locations (s/and (s/map-of ::location-id ::location)
                          ::entity-map))
(s/def ::location-connections (s/coll-of ::undirected-connection :kind set?))

(s/def ::character (s/keys :req-un [::id ::display-name ::color]))
(s/def ::characters (s/and (s/map-of ::character-id ::character)
                           ::entity-map))

(s/def ::line (s/keys :req-un [::id ::character-id ::text ::dialogue-id ::position-id]))
(s/def ::lines (s/and (s/map-of ::line-id ::line)
                      ::entity-map))

(s/def ::dialogue (s/keys :req-un [::id ::initial-line-id ::location-id ::display-name]))
(s/def ::dialogues (s/and (s/map-of ::dialogue-id ::dialogue)
                          ::entity-map))

(s/def ::line-connections (s/coll-of ::directed-connection :kind set?))

(s/def ::position-ids (s/coll-of ::position-id))
(s/def ::start-position ::position)
(s/def ::connecting (s/keys :req-un [::start-position (or ::location-id ::line-id)]))
(s/def ::dragging (s/keys :req-un [::start-position ::position-ids]))

(defn connection-validation [entity-path connection-path]
  (fn [state] (subset? (reduce into #{} (connection-path state))
                       (-> state entity-path keys set))))

(s/def ::line-connection-validation (connection-validation :lines :line-connections))
(s/def ::location-connection-validation (connection-validation :locations :location-connections))

(s/def ::state (s/and (s/keys :req-un [::characters
                                       ::dialogues
                                       ::line-connections
                                       ::lines
                                       ::locations
                                       ::location-connections]
                              :opt-un [::connecting ::dragging ::pointer])
                      ::line-connection-validation
                      ::location-connection-validation))

(def default-db
  {
   :current-page {:name "Game"
                  :payload nil}
   :positions {
               1 [100 200]
               2 [329 198]
               3 [359 91]
               4 [550 214]
               5 [795 178]
               6 [799 239]
               7 [1039 90]
               8 [1065 183]
               9 [1316 197]
               10 [330 280]
               11 [557 280]
               12 [864 314]
               13 [1112 284]
               16 [229 198]
               17 [259 91]
               18 [107 151]
               19 [357 151]
               20 [607 151]
               }
   :locations {
               1 {:id 1
                  :position-id 16
                  :display-name "Park - Camp"
                  :level [[ 0 0 0 0 0 0 0 0 0 0 0 0 1 0 ]
                          [ 0 1 0 1 1 1 1 1 1 1 1 1 1 0 ]
                          [ 0 1 1 1 0 0 0 0 0 0 1 1 0 0 ]
                          [ 0 1 1 1 0 0 0 0 0 0 1 1 1 0 ]
                          [ 0 1 0 1 0 0 1 1 1 1 1 0 1 0 ]
                          [ 0 0 0 1 1 1 1 0 0 1 0 0 1 0 ]
                          [ 0 0 1 1 0 0 1 0 0 1 0 1 1 0 ]
                          [ 0 1 1 1 1 0 1 0 0 1 1 1 1 0 ]
                          [ 0 1 0 1 1 0 1 1 1 1 0 0 1 0 ]
                          [ 0 1 0 1 1 0 1 1 1 1 0 0 1 0 ]
                          [ 0 1 0 1 1 0 1 1 1 1 0 0 1 0 ]
                          [ 0 0 0 0 1 0 1 0 0 1 0 0 0 0 ]
                          [ 0 1 0 1 1 1 1 1 1 1 1 0 1 0 ]
                          [ 0 1 1 1 0 0 0 0 0 0 1 1 1 0 ]
                          [ 0 1 1 1 0 0 0 0 0 0 1 1 1 0 ]
                          [ 0 1 0 1 0 0 1 1 1 1 1 0 1 0 ]
                          [ 0 1 0 1 1 1 1 0 0 1 0 0 1 0 ]
                          [ 0 1 1 1 0 0 1 0 0 1 0 1 1 0 ]
                          [ 0 1 1 1 1 0 1 0 0 1 0 1 1 0 ]
                          [ 0 0 0 0 0 0 1 0 0 1 0 1 1 0 ]
                          [ 1 1 1 1 1 0 1 1 0 1 0 1 0 0 ]
                          [ 0 1 1 1 1 0 1 1 0 1 1 1 0 0 ]
                          [ 0 1 0 1 1 0 1 0 0 1 0 1 0 0 ]
                          [ 0 1 0 1 1 1 1 1 1 1 0 1 1 0 ]
                          [ 0 0 0 0 0 0 0 0 0 0 0 0 0 0 ]]
                  :enemies {1 [6 6]
                            3 [5 12]}}
               2 {:id 2
                  :position-id 17
                  :display-name "Park - Entrance"}
               }
   :location-connections #{
                           #{1 2}
                           }
   :characters {
                1 { :id 1 :display-name "Hugo" :color "rgba(255, 0, 0, .6)" }
                2 { :id 2 :display-name "Player" :color "rgba(0, 0, 255, .6)" }
                3 { :id 3 :display-name "Gustav" :color "rgba(92, 154, 9, 0.8)" }
                }
   :dialogues {
               1 { :id 1 :display-name "Hugo's Dialogue" :initial-line-id 1 :location-id 1 }
               2 { :id 2 :display-name "Gustav's Dialogue" :initial-line-id 14 :location-id 1 }
               }
   :lines {
           1  {:id 1
               :character-id 1
               :dialogue-id 1
               :position-id 1
               :text "Hey, who are you?"}
           2  {:id 2
               :character-id 2
               :dialogue-id 1
               :position-id 2
               :text "I could ask you the same."}
           3  {:id 3
               :character-id 2
               :dialogue-id 1
               :position-id 3
               :text "My name does not matter."}
           4  {:id 4
               :character-id 1
               :dialogue-id 1
               :position-id 4
               :text "I am Hugo. And you...?"}
           5  {:id 5
               :character-id 2
               :dialogue-id 1
               :position-id 5
               :text "I am Hugo as well."}
           6  {:id 6
               :character-id 2
               :dialogue-id 1
               :position-id 6
               :text "None of your business!"}
           7  {:id 7
               :character-id 1
               :dialogue-id 1
               :position-id 7
               :text "Fine, be a jerk."}
           8  {:id 8
               :character-id 1
               :dialogue-id 1
               :position-id 8
               :text "Nice to meet you!"}
           9  {:id 9
               :character-id 1
               :dialogue-id 1
               :position-id 9
               :text "Ok, bye!"}
           10 {:id 10
               :character-id 2
               :dialogue-id 1
               :position-id 10
               :text "Hello Hugo!"}
           11 {:id 11
               :character-id 1
               :dialogue-id 1
               :position-id 11
               :text "How do you know my name?"}
           12 {:id 12
               :character-id 2
               :dialogue-id 1
               :position-id 12
               :text "We have met before. In the land far beyond."}
           13 {:id 13
               :character-id 1
               :dialogue-id 1
               :position-id 13
               :text "Trying to sound ominous or what?! Get outa here!"}
           14 {:id 14
               :character-id 3
               :dialogue-id 2
               :position-id 18
               :text "Yes?"}
           15 {:id 15
               :character-id 2
               :dialogue-id 2
               :position-id 19
               :text "Who are you?"}
           16 {:id 16
               :character-id 3
               :dialogue-id 2
               :position-id 20
               :text "I am Gustav!"}
           }
   :line-connections #{
                       [1 2]
                       [2 4]
                       [4 5]
                       [4 6]
                       [6 7]
                       [5 8]
                       [8 9]
                       [1 3]
                       [3 7]
                       [7 9]
                       [1 10]
                       [10 11]
                       [11 12]
                       [12 13]
                       [11 6]
                       [14 15]
                       [15 16]
                       }
  })

(defn line-count-for-character [lines character-id]
  (let [filter-fn #(= (:character-id %) character-id)]
    (->> lines vals (filter filter-fn) count)))
