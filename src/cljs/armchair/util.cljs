(ns armchair.util
  (:require [clojure.set :refer [subset?]]
            [re-frame.core :as re-frame]
            [armchair.math :refer [clamp round Point Rect]]
            [armchair.config :as config]
            [com.rpl.specter
             :refer [collect-one ALL FIRST LAST MAP-VALS MAP-KEYS]
             :refer-macros [transform]]))

(defn px [v]
  (if (zero? v) v (str (round v) "px")))

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
  (Point. (* config/tile-size x) (* config/tile-size y)))

(defn coord->tile
  ([{:keys [x y]}]
   (Point. (cond-> (quot x config/tile-size) (neg? x) dec)
           (cond-> (quot y config/tile-size) (neg? y) dec)))
  ([{:keys [x y]} zoom-scale]
   (Point. (cond-> (quot (/ x zoom-scale) config/tile-size) (neg? x) dec)
           (cond-> (quot (/ y zoom-scale) config/tile-size) (neg? y) dec))))

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
  (transform [ALL (collect-one FIRST) LAST] #(f %2 %1) m))

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

(defn index-of [v item]
  (let [idx? (fn [i a] (when (= item a) i))]
    (first (keep-indexed idx? v))))

(defn spy [& args]
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
    (if (= (.-type target) "checkbox")
      (.-checked target)
      (.-value target))))

(defn get-rect [elem]
  (let [domrect (.getBoundingClientRect elem)]
    (Rect. (.-left domrect) (.-top domrect)
           (.-width domrect) (.-height domrect))))

(defn relative-cursor [e elem]
  (let [domrect (.getBoundingClientRect elem)]
    (Point. (- (.-clientX e) (.-left domrect))
            (- (.-clientY e) (.-top domrect)))))

(defn e->left? [e]
  (zero? (.-button e)))

(defn e->point [e]
  (Point. (.-clientX e) (.-clientY e)))

(defn e->tile
  ([e]
   (e->tile e 1))
  ([e zoom-scale]
   (let [target (.-currentTarget e)
         {:keys [x y]} (relative-cursor e target)
         max-x (.-offsetWidth target)
         max-y (.-offsetHeight target)]
    (Point. (quot (/ (clamp 0 max-x x) zoom-scale) config/tile-size)
            (quot (/ (clamp 0 max-y y) zoom-scale) config/tile-size)))))

(defn e->sprite [e tile-size gutter offset]
  (let [target (.-currentTarget e)
        {:keys [x y]} (relative-cursor e target)
        max-x (.-offsetWidth target)
        max-y (.-offsetHeight target)
        tile-scale (/ config/tile-size tile-size)
        valid-dim? (fn [dim]
                     (<= (mod (- dim offset) (+ tile-size gutter)) tile-size))]
    (when (and (valid-dim? x) (valid-dim? y))
      (Point. (quot (clamp 0 max-x x) (* tile-scale (+ tile-size gutter)))
              (quot (clamp 0 max-y y) (* tile-scale (+ tile-size gutter)))))))

(defn tile-style
  ([tile] (tile-style tile 1))
  ([{:keys [x y]} zoom-scale]
   {:width (px (* config/tile-size zoom-scale))
    :height (px (* config/tile-size zoom-scale))
    :top (px (* y config/tile-size zoom-scale))
    :left (px (* x config/tile-size zoom-scale))}))

(defn sprite-style [{:keys [x y]} tile-size gutter offset]
  (let [tile-scale (/ config/tile-size tile-size)
        scaled-gutter (* gutter tile-scale)
        scaled-offset (* offset tile-scale)]
    {:width (px config/tile-size)
     :height (px config/tile-size)
     :top (px (+ (* y (+ config/tile-size scaled-gutter)) scaled-offset))
     :left (px (+ (* x (+ config/tile-size scaled-gutter)) scaled-offset))}))

