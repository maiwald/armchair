(ns armchair.pathfinding
  (:require [tailrecursion.priority-map :refer [priority-map]]))

(defn ^boolean walkable? [level coord]
  (= (get-in level coord) 1))

(defn abs [x]
  (.abs js/Math x))

(defn manhattan-distance [[x1 y1] [x2 y2]]
  (+ (abs (- x2 x1)) (abs (- y2 y1))))

(defn neighbours [level [x y]]
  (filter #(walkable? level %)
          [[(+ x 1) y]
           [(- x 1) y]
           [x (+ y 1)]
           [x (- y 1)]
           ]))

(defn a-star [level start end]
  (when (walkable? level end)
    (loop [frontier (priority-map start 0)
           cost-so-far {start 0}
           came-from {start nil}]
      (let [current (first (peek frontier))]
        (if (or (= current end)
                (empty? frontier))
          (reverse (take-while some? (iterate came-from end)))
          (let [new-cost (inc (cost-so-far current))
                priority #(+ new-cost (manhattan-distance % end))
                candidates (filter #(or (not (contains? cost-so-far %))
                                        (< new-cost (cost-so-far %)))
                                   (neighbours level current))]
            (recur
              (into (pop frontier) (for [c candidates] [c (priority c)]))
              (into cost-so-far (for [c candidates] [c new-cost]))
              (into came-from (for [c candidates] [c current])))))))))
