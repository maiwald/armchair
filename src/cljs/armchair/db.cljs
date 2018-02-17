(ns armchair.db)

(def default-db
  {
   :current-page "Dialogue"
   :selected-dialogue-id 1
   :locations {
               1 { :id 1 :display-name "Park - Camp" }
               2 { :id 2 :display-name "Park - Entrance" }
               }
   :characters {
                1 { :id 1 :display-name "Hugo" :color "rgba(255, 0, 0, .6)" }
                2 { :id 2 :display-name "Player" :color "rgba(0, 0, 255, .6)" }
                }
   :dialogues {
               1 { :id 1 :display-name "First Dialogue" :location-id 1 }
               }
   :lines {
           1  {:id 1
               :character-id 1
               :dialogue-id 1
               :position [0 200]
               :text "Hey, who are you?"}
           2  {:id 2
               :character-id 2
               :dialogue-id 1
               :position [229 198]
               :text "I could ask you the same."}
           3  {:id 3
               :character-id 2
               :dialogue-id 1
               :position [259  91]
               :text "My name does not matter."}
           4  {:id 4
               :character-id 1
               :dialogue-id 1
               :position [450 214]
               :text "I am Hugo. And you...?"}
           5  {:id 5
               :character-id 2
               :dialogue-id 1
               :position [695 178]
               :text "I am Hugo as well."}
           6  {:id 6
               :character-id 2
               :dialogue-id 1
               :position [699 239]
               :text "None of your business!"}
           7  {:id 7
               :character-id 1
               :dialogue-id 1
               :position [939  90]
               :text "Fine, be a jerk."}
           8  {:id 8
               :character-id 1
               :dialogue-id 1
               :position [965 183]
               :text "Nice to meet you!"}
           9  {:id 9
               :character-id 1
               :dialogue-id 1
               :position [1216 197]
               :text "Ok, bye!"}
           10 {:id 10
               :character-id 2
               :dialogue-id 1
               :position [230 280]
               :text "Hello Hugo!"}
           11 {:id 11
               :character-id 1
               :dialogue-id 1
               :position [457 280]
               :text "How do you know my name?"}
           12 {:id 12
               :character-id 2
               :dialogue-id 1
               :position [764 314]
               :text "We have met before. In the land far beyond."}
           13 {:id 13
               :character-id 1
               :dialogue-id 1
               :position [1012 284]
               :text "Trying to sound ominous or what?! Get outa here!"}
           }
   :connections #{
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
