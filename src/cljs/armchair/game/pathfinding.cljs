(ns armchair.game.pathfinding
  (:require [tailrecursion.priority-map :refer [priority-map]]
            [armchair.math :refer [abs translate-point]]))

(defn manhattan-distance [p1 p2]
  (+ (abs (- (:x p2) (:x p1)))
     (abs (- (:y p2) (:y p1)))))

(defn neighbours [point]
  (map #(apply translate-point point %)
       [[-1 0]
        [+1 0]
        [0 -1]
        [0 +1]]))

(defn a-star [walkable? start end]
  (when (walkable? end)
    (loop [frontier (priority-map start 0)
           cost-so-far {start 0}
           came-from {start nil}]
      (let [current (first (peek frontier))]
        (if (or (= current end)
                (empty? frontier))
          (reverse (take-while some? (iterate came-from end)))
          (let [new-cost (inc (cost-so-far current))
                priority #(+ new-cost (manhattan-distance % end))
                candidates (filter #(and (walkable? %)
                                         (or (not (contains? cost-so-far %))
                                             (< new-cost (cost-so-far %))))
                                   (neighbours current))]
            (recur
              (into (pop frontier) (for [c candidates] [c (priority c)]))
              (into cost-so-far (for [c candidates] [c new-cost]))
              (into came-from (for [c candidates] [c current])))))))))
