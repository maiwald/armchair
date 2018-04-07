(ns armchair.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [clojure.set :refer [difference subset?]]
            [armchair.db :as db]
            [armchair.config :as config]
            [armchair.position :refer [position-delta apply-delta translate-positions]]))

(reg-sub :db-characters #(:characters %))

(reg-sub :db-lines #(:lines %))
(reg-sub :db-line-connections #(:line-connections %))

(reg-sub :locations #(:locations %))
(reg-sub :db-location-connections #(:location-connections %))

(reg-sub :db-dragging #(:dragging %))
(reg-sub :db-connecting #(:connecting %))
(reg-sub :db-positions #(:positions %))
(reg-sub :db-pointer #(:pointer %))

(reg-sub :current-page #(:current-page %))
(reg-sub :modal #(:modal %))

(defn map-values [f m]
  (into {} (for [[k v] m] [k (f v)])))

(reg-sub
  :characters
  :<- [:db-characters]
  :<- [:db-lines]
  (fn [[characters lines]]
    (map-values
      (fn [character]
        (let [line-count (db/line-count-for-character lines (:id character))]
          (assoc character :lines line-count)))
      characters)))

(reg-sub
  :dragging?
  :<- [:db-dragging]
  (fn [dragging] (some? dragging)))

(reg-sub
  :connecting?
  :<- [:db-connecting]
  (fn [connecting] (some? connecting)))

(reg-sub
  :dragged-positions
  :<- [:db-dragging]
  :<- [:db-positions]
  :<- [:db-pointer]
  (fn [[dragging positions pointer]]
    (if-let [{:keys [position-ids start-position]} dragging]
      (translate-positions positions position-ids (position-delta start-position pointer))
      positions)))

(reg-sub
  :dialogue
  :<- [:db-lines]
  :<- [:db-line-connections]
  :<- [:db-characters]
  :<- [:dragged-positions]
  (fn [[lines connections characters positions]]
    (let [position-map (map-values #(positions (:position-id %)) lines)]
      {:lines (map-values #(assoc %
                                  :position (get positions (:position-id %))
                                  :character-color (get-in characters [(:character-id %) :color]))
                          lines)
       :connections connections})))

(reg-sub
  :location-map
  :<- [:locations]
  :<- [:db-location-connections]
  :<- [:dragged-positions]
  (fn [[locations connections positions]]
    (let [position-map (map-values #(positions (:position-id %)) locations)]
      {:locations (map-values #(assoc % :position (get positions (:position-id %)))
                              locations)
       :connections (map sort connections)})))

(reg-sub
  :lines
  :<- [:db-lines]
  :<- [:db-characters]
  (fn [[lines characters]]
    (map-values #(assoc % :character-color (get-in characters [(:character-id %) :color]))
                lines)))
