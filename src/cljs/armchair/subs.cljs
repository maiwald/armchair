(ns armchair.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [clojure.set :refer [difference subset?]]
            [armchair.db :as db]
            [armchair.config :as config]
            [armchair.position :refer [position-delta apply-delta translate-positions]]))

(reg-sub :db-lines #(:lines %))
(reg-sub :db-locations #(:locations %))
(reg-sub :db-characters #(:characters %))
(reg-sub :db-line-connections #(:line-connections %))
(reg-sub :db-dragging #(:dragging %))
(reg-sub :db-connecting #(:connecting %))
(reg-sub :db-positions #(:positions %))
(reg-sub :db-pointer #(:pointer %))
(reg-sub :db-selected-dialogue-id #(:selected-dialogue-id %))

(reg-sub :current-page #(:current-page %))
(reg-sub :modal #(:modal %))

(defn map-values [f m]
  (into {} (for [[k v] m] [k (f v)])))

(reg-sub
  :locations
  :<- [:db-locations]
  :<- [:dragged-positions]
  (fn [[locations positions] _]
    (map-values
      (fn [location]
        (let [position (get positions (:position-id location))]
          (assoc location :position position)))
      locations)))

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
  :dialogue-lines
  :<- [:db-lines]
  :<- [:db-selected-dialogue-id]
  (fn [[lines selected-dialogue-id]]
    (db/lines-for-dialogue lines selected-dialogue-id)))

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
  :lines-with-drag
  :<- [:dialogue-lines]
  :<- [:dragged-positions]
  (fn [[lines positions] _]
    (map-values
      (fn [line]
        (let [position (get positions (:position-id line))]
          (assoc line :position position)))
      lines)))

(reg-sub
  :dialogue-line-connections
  :<- [:db-line-connections]
  :<- [:dialogue-lines]
  (fn [[line-connections lines]]
    (let [line-keys (-> lines keys set)]
      (filter #(subset? (set %) line-keys) line-connections))))

(reg-sub
  :lines
  :<- [:lines-with-drag]
  :<- [:db-characters]
  (fn [[lines-with-drag characters]]
    (map-values
      (fn [line]
        (let [character (get characters (:character-id line))]
          (assoc line :character-color (:color character))))
      lines-with-drag)))

(defn start-offset [position]
  (apply-delta position [(- config/line-width 15) 15]))

(defn end-offset [position]
  (apply-delta position [15 15]))

(reg-sub
  :line-connecting-connection
  :<- [:lines-with-drag]
  :<- [:db-connecting]
  :<- [:db-pointer]
  (fn [[lines connecting pointer]]
    (if-let [start (:line-id connecting)]
      (let [base-position (start-offset (get-in lines [start :position]))
            delta (position-delta (:start-position connecting) pointer)
            end-position (apply-delta base-position delta)]
        {:id (str "connection-" start "-?")
         :kind :drag-connection
         :start base-position
         :end end-position}))))

(reg-sub
  :line-connections
  :<- [:lines-with-drag]
  :<- [:dialogue-line-connections]
  :<- [:line-connecting-connection]
  (fn [[lines connections connecting-connection]]
    (let [connection->positions (fn [[start end]]
                                  {:kind :connection
                                   :id (str "connection-" start "-" end)
                                   :start (start-offset (get-in lines [start :position]))
                                   :end (end-offset (get-in lines [end :position]))})]
      (if (some? connecting-connection)
        (conj (map connection->positions connections) connecting-connection)
        (map connection->positions connections)))))
