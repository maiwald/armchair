(ns armchair.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [clojure.set :refer [difference subset?]]
            [armchair.db :as db]
            [armchair.config :as config]
            [armchair.position :refer [apply-delta translate-positions]]))

(reg-sub :db-lines #(:lines %))
(reg-sub :db-characters #(:characters %))
(reg-sub :db-connections #(:connections %))
(reg-sub :db-dragging #(:dragging %))
(reg-sub :db-positions #(:positions %))
(reg-sub :db-pointer #(:pointer %))
(reg-sub :db-selected-dialogue-id #(:selected-dialogue-id %))
(reg-sub :db-selected-line-id #(:selected-line-id %))
(reg-sub :db-selected-character-id #(:selected-character-id %))

(reg-sub :locations #(:locations %))
(reg-sub :current-page #(:current-page %))
(reg-sub :modal #(:modal %))

(reg-sub
  :characters
  :<- [:db-characters]
  :<- [:db-lines]
  (fn [[characters lines]]
    (reduce-kv
      (fn [acc id character]
        (let [line-count (db/line-count-for-character lines id)]
          (assoc acc id (assoc character :lines line-count))))
      {}
      characters)))

(reg-sub
  :selected-line
  :<- [:db-lines]
  :<- [:db-selected-line-id]
  (fn [[lines id]]
    (get lines id)))

(reg-sub
  :selected-character
  :<- [:characters]
  :<- [:db-selected-character-id]
  (fn [[characters id]]
    (get characters id)))

(reg-sub
  :dialogue-lines
  :<- [:db-lines]
  :<- [:db-selected-dialogue-id]
  (fn [[lines selected-dialogue-id]]
    (db/lines-for-dialogue lines selected-dialogue-id)))

(reg-sub
  :dragged-positions
  :<- [:db-dragging]
  :<- [:db-positions]
  (fn [[dragging positions]]
    (if-let [{:keys [position-ids delta]} dragging]
      (translate-positions positions position-ids delta)
      positions)))

(reg-sub
  :lines-with-drag
  :<- [:dialogue-lines]
  :<- [:dragged-positions]
  (fn [[lines positions] _]
    (reduce-kv
      (fn [acc k v]
        (let [position (get positions (:position-id v))]
          (assoc acc k
                 (assoc v :position position))))
      {}
      lines)))

(reg-sub
  :dialogue-connections
  :<- [:db-connections]
  :<- [:dialogue-lines]
  (fn [[connections lines]]
    (let [line-keys (-> lines keys set)]
      (filter #(subset? (set %) line-keys) connections))))

(reg-sub
  :lines
  :<- [:lines-with-drag]
  :<- [:db-characters]
  (fn [[lines-with-drag characters]]
    (reduce-kv
      (fn [acc id line]
        (let [character (get characters (:character-id line))]
          (assoc acc id (assoc line :character-color (:color character)))))
      {}
      lines-with-drag)))

(reg-sub
  :connections
  :<- [:lines-with-drag]
  :<- [:db-dragging]
  :<- [:dialogue-connections]
  (fn [[lines dragging connections]]
    (let [start-offset #(apply-delta % [(- config/line-width 15) 15])
          end-offset #(apply-delta % [15 15])
          connection->positions (fn [[start end]]
                                  {:kind :connection
                                   :id (str "connection-" start "-" end)
                                   :start (start-offset (get-in lines [start :position]))
                                   :end (end-offset (get-in lines [end :position]))})
          view-connections (map connection->positions connections)]
      (if-let [start (:connection-start dragging)]
        (let [base-position (start-offset (get-in lines [start :position]))]
          (conj
            view-connections
            {:id (str "connection-" start "-?")
             :kind :drag-connection
             :start base-position
             :end (apply-delta base-position (:delta dragging))}))
          view-connections))))
