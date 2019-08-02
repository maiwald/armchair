(ns armchair.util
  (:require [clojure.set :refer [intersection subset?]]
            [re-frame.core :as re-frame]
            [armchair.math :refer [Point]]
            [armchair.config :refer [tile-size]]
            [com.rpl.specter
             :refer [must ALL NONE MAP-VALS MAP-KEYS]
             :refer-macros [transform]]))

(defn px [v] (str v "px"))

(defn truncate [s n]
  (if (< (count s) n)
    s
    (str (subs s 0 n) "…")))

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

(defn tile->coord [{:keys [x y]}]
  (Point. (* tile-size x) (* tile-size y)))

(defn coord->tile [{:keys [x y]}]
  (Point. (quot x tile-size) (quot y tile-size)))

(defn normalize-to-tile [coord]
  (-> coord coord->tile tile->coord))

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

(defn update-values
  ([m k f] (transform [k MAP-VALS] f m))
  ([m k f & args] (transform [k MAP-VALS] #(apply f % args) m)))

(defn submap? [a-map b-map]
  (subset? (set a-map) (set b-map)))

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

(defn prevent-e! [e]
  (.preventDefault e)
  e)

(defn e-> [handler]
  (fn [e]
    (stop-e! e)
    (prevent-e! e)
    (handler e)
    nil))

(defn e->val [e]
  (let [target (.-target e)]
    (case (.-type target)
      "checkbox" (.-checked target)
      (.-value target))))

(defn get-rect [elem]
  (let [rect (.getBoundingClientRect elem)]
    {:top (.-top rect)
     :left (.-left rect)
     :bottom (.-bottom rect)
     :right (.-right rect)
     :width (.-width rect)
     :height (.-height rect)}))

(defn relative-cursor [e elem]
  (let [rect (.getBoundingClientRect elem)]
    (Point. (- (.-clientX e) (.-left rect))
            (- (.-clientY e) (.-top rect)))))

(defn e->left? [e]
  (zero? (.-button e)))
