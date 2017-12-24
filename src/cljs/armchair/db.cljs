(ns armchair.db)

(def default-db
  {:lines '{
            1  {:id 1  :character-id 1 :position [0   200] :text "Hey, who are you?"}
            2  {:id 2  :character-id 2 :position [230 120] :text "I could ask you the same."}
            3  {:id 3  :character-id 2 :position [230 200] :text "My name does not matter."}
            4  {:id 4  :character-id 1 :position [460 120] :text "I am Hugo. And you...?"}
            5  {:id 5  :character-id 2 :position [0   0  ] :text "I am Hugo as well."}
            6  {:id 6  :character-id 2 :position [0   0  ] :text "None of your business!"}
            7  {:id 7  :character-id 1 :position [0   0  ] :text "Fine, be a jerk."}
            8  {:id 8  :character-id 1 :position [0   0  ] :text "Nice to meet you!"}
            9  {:id 9  :character-id 1 :position [0   0  ] :text "Ok, bye!"}
            10 {:id 10 :character-id 2 :position [230 280] :text "Hello Hugo!"}
            11 {:id 11 :character-id 1 :position [0   0  ] :text "How do you know my name?"}
            12 {:id 12 :character-id 2 :position [0   0  ] :text "We have met before. In the land far beyond."}
            13 {:id 13 :character-id 1 :position [0   0  ] :text "Trying to sound ominous or what?! Get outa here!"}
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
