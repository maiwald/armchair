(ns armchair.db
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.set :refer [subset?]]))

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
(s/def ::connecting (s/keys :req-un [::start-position (or ::location-id ::line-id)]))
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

(s/def ::display-name ::text)
(s/def ::color ::text)

(s/def ::location (s/keys :req-un [::id ::position-id ::display-name]))
(s/def ::locations (s/and ::entity-map
                          (s/map-of ::location-id ::location)))
(s/def ::location-connections (s/coll-of ::undirected-connection :kind set?))

(s/def ::character (s/keys :req-un [::id ::display-name ::color]))
(s/def ::characters (s/and ::entity-map
                           (s/map-of ::character-id ::character)))

(s/def ::next-line-id (s/or :line-id ::line-id
                            :end #(= :end)))
(s/def ::line (s/keys :req-un [::text ::next-line-id]))

(s/def ::character-line (s/and ::line
                               (s/keys :req-un [::character-id])))

(s/def ::options (s/coll-of ::line :kind vector?))
(s/def ::response (s/keys :req-un [::options]))

(s/def ::line-or-response (s/and (s/keys :req-un [::id ::dialogue-id ::position-id])
                                 (s/or :line ::line
                                       :response ::response)))

(s/def ::lines (s/and ::entity-map
                      (s/map-of ::line-id ::line-or-response)))

(s/def ::dialogue (s/keys :req-un [::id ::initial-line-id ::location-id ::display-name]))
(s/def ::dialogues (s/and ::entity-map
                          (s/map-of ::dialogue-id ::dialogue)))

(defn connection-validation [entity-path connection-path]
  (fn [state] (subset? (reduce into #{} (connection-path state))
                       (-> state entity-path keys set))))

(s/def ::location-connection-validation (connection-validation :locations :location-connections))

(s/def ::state (s/and (s/keys :req-un [::current-page
                                       ::characters
                                       ::dialogues
                                       ::lines
                                       ::locations
                                       ::location-connections]
                              :opt-un [::connecting ::dragging ::pointer])
                      ::location-connection-validation))

(def default-db
  {
   :current-page {:name "Game"}
   :positions {1 [100 200]
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
               20 [607 151]}
   :locations {1 {:id 1
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
                  :display-name "Park - Entrance"}}
   :location-connections #{#{1 2}}
   :characters {1 {:id 1 :display-name "Hugo" :color "rgba(255, 0, 0, .6)"}
                3 {:id 3 :display-name "Gustav" :color "rgba(92, 154, 9, 0.8)"}}
   :dialogues {1 {:id 1 :display-name "Hugo's Dialogue" :initial-line-id 1 :location-id 1}
               2 {:id 2 :display-name "Gustav's Dialogue" :initial-line-id 14 :location-id 1}}
   :lines {1  {:id 1
               :character-id 1
               :dialogue-id 1
               :position-id 1
               :text "Hey, who are you?"
               :next-line-id 2}
           2  {:id 2
               :dialogue-id 1
               :position-id 2
               :options [{:text "I could ask you the same." :next-line-id 3}
                         {:text "My name does not matter." :next-line-id 4}]}
           3 {:id 3
              :dialogue-id 1
              :character-id 1
              :position-id 3
              :text "I am Hugo. And you?"
              :next-line-id 5}
           4 {:id 4
              :dialogue-id 1
              :character-id 1
              :position-id 4
              :text "Fine, be a jerk."
              :next-line-id :end}
           5 {:id 5
              :dialogue-id 1
              :character-id 1
              :position-id 5
              :text "What a strange coincidence! I am Hugo as well."
              :next-line-id 6}
           6 {:id 6
              :dialogue-id 1
              :character-id 1
              :position-id 6
              :text "Anyway, ...bye!"
              :next-line-id :end}
           14 {:id 14
               :character-id 3
               :dialogue-id 2
               :position-id 18
               :text "Yes?"
               :next-line-id 15}
           15 {:id 15
               :dialogue-id 2
               :position-id 19
               :options [{:text "Who are you?" :next-line-id 16}]}
           16 {:id 16
               :character-id 3
               :dialogue-id 2
               :position-id 20
               :text "I am Gustav!"
               :next-line-id :end}
           }
})

(when-not (s/valid? ::state default-db)
  (.log js/console "Default DB state explain:")
  (s/explain ::state default-db))

(defn line-count-for-character [lines character-id]
  (let [filter-fn #(= (:character-id %) character-id)]
    (->> lines vals (filter filter-fn) count)))
