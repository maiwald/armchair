(ns armchair.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [clojure.set :refer [difference subset?]]
            [clojure.spec.alpha :as s]
            [armchair.db :as db]
            [armchair.config :as config]
            [armchair.util :refer [where where-map map-values position-delta translate-position translate-positions]]))

(reg-sub :db-characters #(:characters %))

(reg-sub :db-lines #(:lines %))
(reg-sub :db-line-connections #(:line-connections %))

(reg-sub :db-locations #(:locations %))
(reg-sub :db-location-connections #(:location-connections %))

(reg-sub :db-dialogues #(:dialogues %))

(reg-sub :db-dragging #(:dragging %))
(reg-sub :db-connecting #(:connecting %))
(reg-sub :db-positions #(:positions %))
(reg-sub :db-pointer #(:pointer %))

(reg-sub :current-page #(:current-page %))
(reg-sub :modal #(:modal %))

(reg-sub
  :character-list
  :<- [:db-characters]
  :<- [:db-lines]
  (fn [[characters lines] _]
    (map-values
      (fn [character]
        (let [line-count (db/line-count-for-character lines (:id character))]
          (assoc character :lines line-count)))
      characters)))

(reg-sub
  :character
  :<- [:db-characters]
  (fn [characters [_ character-id]]
    (characters character-id)))

(reg-sub
  :line
  :<- [:db-lines]
  (fn [lines [_ line-id]]
    (lines line-id)))

(reg-sub
  :character-options
  :<- [:db-characters]
  (fn [characters _]
    (map-values :display-name characters)))

(reg-sub
  :dragging?
  :<- [:db-dragging]
  (fn [dragging _]
    (some? dragging)))

(reg-sub
  :dragging-item?
  :<- [:db-dragging]
  (fn [dragging [_ position-id]]
    (= (:position-ids dragging) #{position-id})))

(reg-sub
  :connector
  :<- [:db-connecting]
  :<- [:db-pointer]
  (fn [[connecting pointer]]
    (when connecting
      {:start (:start-position connecting)
       :end pointer
       :kind :connector})))

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
  (fn [[lines connections characters positions] [_ dialogue-id]]
    (let [dialogue-lines (where-map :dialogue-id dialogue-id lines)
          lines-by-type (group-by #(first (s/conform :armchair.db/line-or-response %)) (vals dialogue-lines))]
      {:lines (map-values #(assoc %
                                  :position (get positions (:position-id %))
                                  :character-color (get-in characters [(:character-id %) :color]))
                          dialogue-lines)
       :line-connections (->> (:line lines-by-type)
                              (filter #(s/valid? :armchair.db/line-id (:next-line-id %)))
                              (map #(vector (:id %) (:next-line-id %))))
       :response-connections (reduce
                               (fn [acc {:keys [id options]}]
                                 (apply conj acc (->> options
                                                      (filter #(s/valid? :armchair.db/line-id (:next-line-id %)))
                                                      (map-indexed #(vector id %1 (:next-line-id %2))))))
                               (list)
                               (:response lines-by-type))})))

(reg-sub
  :location
  :<- [:db-locations]
  (fn [locations [_ location-id]]
    (locations location-id)))

(reg-sub
  :location-map
  :<- [:db-locations]
  :<- [:db-dialogues]
  :<- [:db-lines]
  :<- [:db-characters]
  :<- [:db-location-connections]
  :<- [:dragged-positions]
  (fn [[locations dialogues lines characters connections positions] _]
    {:locations (map-values (fn [location]
                              (assoc location
                                     :position (get positions (:position-id location))
                                     :dialogues (->> dialogues
                                                     vals
                                                     (where :location-id (:id location))
                                                     (map #(let [line (get lines (:initial-line-id %))
                                                                 character (get characters (:character-id line))]
                                                             (assoc %
                                                                    :character-name (:display-name character)
                                                                    :character-color (:color character)))))))
                            locations)
     :connections (map sort connections)}))

(reg-sub
  :game-data
  :<- [:db-locations]
  :<- [:db-dialogues]
  :<- [:db-lines]
  :<- [:db-line-connections]
  (fn [[locations dialogues lines connections] _]
    (let [location (get locations 1)
          location-dialogues (->> dialogues vals (where :location-id 1))]
      {:level (:level location)
       :enemies (:enemies location)
       :dialogues (into {} (map (fn [{:keys [id initial-line-id]}]
                                  (let [initial-line (get lines initial-line-id)
                                        dialogue-lines (where-map :dialogue-id id lines)]
                                    [(:character-id initial-line)
                                     {:initial-line-id initial-line-id
                                      :lines dialogue-lines
                                      :connections (filter (fn [[start end]]
                                                            (and (contains? dialogue-lines start)
                                                                 (contains? dialogue-lines end)))
                                                          connections)}]))
                                location-dialogues))})))

