(ns armchair.util
  (:require [clojure.set :refer [intersection]]
            [re-frame.core :as re-frame]
            [armchair.config :refer [tile-size]]
            [com.rpl.specter
             :refer [must ALL NONE MAP-VALS MAP-KEYS]
             :refer-macros [transform]]))

(defn px [v]
  (str v "px"))

(defn upload-json! [callback]
  (let [file-input (doto (js/document.createElement "input")
                     (.setAttribute "type" "file")
                     (.setAttribute "multiple" false)
                     (.setAttribute "accept" "application/json"))
        file-reader (new js/FileReader)]
    (set! (.-onchange file-input)
          #(if-let [file (-> file-input .-files (aget 0))]
             (.readAsText file-reader file)))
    (set! (.-onloadend file-reader)
          #(if-let [content (.-result file-reader)]
             (callback content)))
    (.click file-input)))

;; Conversion Helpers

(defn tile->coord [[tx ty]]
  [(* tile-size tx) (* tile-size ty)])

(defn coord->tile [[cx cy]]
  [(quot cx tile-size) (quot cy tile-size)])

(defn normalize-to-tile [coord]
  (-> coord coord->tile tile->coord))

(defn rect-width [[[x1 _] [x2 _]]]
  (inc (- x2 x1)))

(defn rect-height [[[_ y1] [_ y2]]]
  (inc (- y2 y1)))

(defn rect-contains? [[[x1 y1] [x2 y2]] [x y]]
  (and (<= x1 x x2)
       (<= y1 y y2)))

(defn rect-intersects? [[[left1 top1] [right1 bottom1]]
                        [[left2 top2] [right2 bottom2]]]
  (and (<= left1 right2)
       (<= left2 right1)
       (<= top1 bottom2)
       (<= top2 bottom1)))

(defn point-delta [start end]
  (mapv - end start))

(defn translate-point [point & deltas]
  (apply mapv + point deltas))

(defn rect->0
  "Normalize a point relative to a rect to a 0,0 based rect"
  [[top-left _] point]
  (point-delta top-left point))

(defn update-in-map
  "Updates specific map keys in a nested data structure"
  [m path map-ks f & args]
  (let [wrapped-f #(apply f % args)
        path-ks (if (vector? path) path [path])]
    (loop [m m
           [k & ks] map-ks]
      (if (nil? k)
        m
        (recur (update-in m (conj path-ks k) wrapped-f)
               ks)))))

(defn reverse-map [m]
  (reduce-kv (fn [acc k v] (assoc acc v k)) {} m))

(defn map-values [f m]
  (transform [MAP-VALS] f m))

(defn map-keys [f m]
  (transform [MAP-KEYS] f m))

(defn update-values [m k f]
  (transform [k MAP-VALS] f m))

(defn where
  ([property value coll]
   (filter #(= (property %) value)
           coll))
  ([property-map coll]
   (filter #(every? (fn [[p v]] (= (p %) v))
                    property-map)
           coll)))

(defn filter-map [pred? coll]
  (into {} (filter #(pred? (second %)) coll)))

(defn filter-keys [pred? coll]
  (into {} (filter #(pred? (first %)) coll)))

(defn where-map [property value coll]
  (filter-map #(= (property %) value) coll))

(defn removev [v idx]
  (vec (concat (take idx v)
               (drop (inc idx) v))))

(defn log [& args]
  (apply js/console.log args)
  (first args))

;; View Helpers

(def <sub (comp deref re-frame/subscribe))
(def >evt re-frame/dispatch)

(defn stop-e! [e]
  (.stopPropagation e)
  e)

(defn e-> [handler]
  (fn [e]
    (.stopPropagation e)
    (.preventDefault e)
    (handler e)
    nil))

(defn e->val [e]
  (let [target (.-target e)]
    (case (.-type target)
      "checkbox" (.-checked target)
      (.-value target))))

(defn relative-cursor [e elem]
  (let [rect (.getBoundingClientRect elem)]
    [(- (.-clientX e) (.-left rect))
     (- (.-clientY e) (.-top rect))]))

(defn e->left? [e]
  (zero? (.-button e)))
