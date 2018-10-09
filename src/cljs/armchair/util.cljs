(ns armchair.util
  (:require [clojure.set :refer [intersection]]))

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

(defn rect->0 [[top-left _] point]
  "Normalize a point relative to a rect to a 0,0 based rect"
  (translate-point point top-left -))

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

(def <sub (comp deref re-frame.core/subscribe))
(def >evt re-frame.core/dispatch)

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

(def left-button? #(zero? (.-button %)))
