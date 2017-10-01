(ns armchair.db)

(def default-db
  {:lines '{
            1  {:character-id 1 :x 0   :y 200 :text "Hey, who are you?"}
            2  {:character-id 2 :x 230 :y 120 :text "I could ask you the same."}
            3  {:character-id 2 :x 230 :y 200 :text "My name does not matter."}
            4  {:character-id 1 :x 460 :y 120 :text "I am Hugo. And you...?"}
            5  {:character-id 2 :x 0   :y 0   :text "I am Hugo as well."}
            6  {:character-id 2 :x 0   :y 0   :text "None of your business!"}
            7  {:character-id 1 :x 0   :y 0   :text "Fine, be a jerk."}
            8  {:character-id 1 :x 0   :y 0   :text "Nice to meet you!"}
            9  {:character-id 1 :x 0   :y 0   :text "Ok, bye!"}
            10 {:character-id 2 :x 230 :y 280 :text "Hello Hugo!"}
            11 {:character-id 1 :x 0   :y 0   :text "How do you know my name?"}
            12 {:character-id 2 :x 0   :y 0   :text "We have met before. In the land far beyond."}
            13 {:character-id 1 :x 0   :y 0   :text "Trying to sound ominous or what?! Get outa here!"}
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
