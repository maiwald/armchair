(ns armchair.core
  (:gen-class))

(defonce state (atom {:commits (list :dialog1)
                      :information #{}}))

(def lines {:l1 {:character :hugo :text "Hey, who are you?"}
            :l2 {:character :hugo :text "I am Hugo. And you...?"}
            :l3 {:character :hugo :text "How do you know this? We have never met!"}
            :l4 {:character :hugo :text "Fine, be a jerk."}
            :l5 {:character :hugo :text "Nice to meet you!"}
            :l6 {:character :player :text "I could ask you the same."}
            :l7 {:character :player :text "I know your name is Hugo!"}
            :l8 {:character :player :text "My name does not matter."}
            :l9 {:character :player :text "I am Hugo as well."}
            :l10 {:character :player :text "I am not telling you!"}
            :l11 {:character :player :text "I just know. I am Hugo as well"}})

(def dialog [{:type :regular :line :l1}
             {:type :selection :line :l6
             ])




(def dialogs {:dialog1 {
                        :text "Hey, who are you?"
                        :options [{:text "I could ask you the same."
                                   :next-dialog :dialog2}
                                  {:text "I know your name is Hugo!"
                                   :required-information [:name_is_hugo]
                                   :next-dialog :dialog5}
                                  {:text "My name does not matter."
                                   :next-dialog :dialog3}]}
              :dialog2 {
                        :text "I am Hugo. And you...?"
                        :information [:name_is_hugo]
                        :options [{:text "My name does not matter."
                                   :next-dialog :dialog3}
                                  {:text "I am Hugo as well"
                                   :next-dialog :dialog4}]}

              :dialog3 {:text "Fine, be a jerk."}
              :dialog4 {:text "Nice to meet you!"}

              :dialog5 {:text "How do you know this? We have never met!"
                        :options [{:text "I am not telling you!"
                                   :next-dialog :dialog3}
                                  {:text "I just know. I am Hugo as well"
                                   :next-dialog :dialog4}]}})

(defn print-dialog [key]
  (let [{:keys [text options]} (key dialogs)]
    (println text)
    (if (some? options)
      (do
        (doseq [[index option] (map-indexed #(list (inc %1) %2) options)]
          (println (str index " - " (:text option))))
        (println)
        (println "Your answer: ")
        (let [next-dialog-index (dec (Integer. (read-line)))
              {:keys [text next-dialog]} (get options next-dialog-index)]
          (println)
          (println text)
          next-dialog)))))

(defn -main [& args]
  (loop [key (first (:commits @state))]
    (println)
    (if-let [next-key (print-dialog key)]
      (recur next-key))))
