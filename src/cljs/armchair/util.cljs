(ns armchair.util
  (:require [clojure.set :refer [intersection]]
            [re-frame.core :as re-frame]))

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

(defn rect-width [[[x1 _] [x2 _]]]
  (inc (- x2 x1)))

(defn rect-height [[[_ y1] [_ y2]]]
  (inc (- y2 y1)))

(defn rect-contains? [[[x1 y1] [x2 y2]] [x y]]
  (and (<= x1 x x2)
       (<= y1 y y2)))

(defn translate-point
  ([point delta] (translate-point point delta +))
  ([point delta f] (if (= delta [0 0])
                        point
                        (mapv f point delta))))

(defn rect->0
  "Normalize a point relative to a rect to a 0,0 based rect"
  [[top-left _] point]
  (translate-point point top-left -))

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

(defn map-values [f m]
  (into {} (for [[k v] m] [k (f v)])))

(defn map-keys [f m]
  (into {} (for [[k v] m] [(f k) v])))

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

(defn where-map
  ([property value coll]
   (filter-map #(= (property %) value)
               coll))
  ([property-map coll]
   (filter-map #(every? (fn [[p v]] (= (p %) v))
                        property-map)
               coll)))

(defn once [f]
  (let [called (atom false)]
    (fn [& args]
      (when-not @called
        (reset! called true)
        (apply f args)))))

(defn log [& args]
  (apply js/console.log args)
  (first args))

;; View Helpers

(def <sub (comp deref re-frame/subscribe))
(def >evt re-frame/dispatch)

(defn stop-e! [e]
  (.preventDefault e)
  (.stopPropagation e)
  e)

(defn e-> [handler]
  (comp handler stop-e!))

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
