(ns armchair.db)

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
               18 [307 151]
               }
   :locations {
               1 { :id 1 :position-id 16 :display-name "Park - Camp" }
               2 { :id 2 :position-id 17 :display-name "Park - Entrance" }
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
               1 { :id 1 :display-name "Hugo's Dialogue" :initial-character-id 1 :location-id 1 }
               2 { :id 2 :display-name "Gustav's Dialogue" :initial-character-id 3 :location-id 1 }
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
               :text "Hi, my name is Gustav!"}
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
                       }
  })

(defn line-count-for-character [lines character-id]
  (let [filter-fn #(= (:character-id %) character-id)]
    (->> lines vals (filter filter-fn) count)))
