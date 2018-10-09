(ns armchair.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub subscribe]]
            [clojure.spec.alpha :as s]
            [armchair.db :as db]
            [armchair.config :as config]
            [armchair.util :refer [where
                                   filter-map
                                   where-map
                                   map-keys
                                   map-values
                                   rect->0
                                   translate-point]]))

(reg-sub :db-characters #(:characters %))
(reg-sub :db-lines #(:lines %))
(reg-sub :db-infos #(:infos %))
(reg-sub :db-locations #(:locations %))
(reg-sub :db-dialogues #(:dialogues %))

(reg-sub :db-location-connections #(:location-connections %))

(reg-sub :db-dragging #(:dragging %))
(reg-sub :db-connecting #(:connecting %))
(reg-sub :db-cursor #(:cursor %))

(reg-sub :current-page #(:current-page %))
(reg-sub :modal #(:modal %))
(reg-sub :dnd-payload #(:dnd-payload %))

(reg-sub
  :character-list
  :<- [:db-characters]
  :<- [:db-lines]
  (fn [[characters lines] _]
    (let [line-counts (->> (vals lines)
                           (group-by :character-id)
                           (map-values count))]
      (map-values (fn [{id :entity/id :as character}]
                    (assoc character
                           :id id
                           :line-count (get line-counts id 0)))
                  characters))))

(reg-sub
  :dialogue/modal-line
  :<- [:db-lines]
  :<- [:db-dialogues]
  (fn [[lines dialogues] [_ line-id]]
    (when-let [{:keys [id dialogue-id] :as line} (get lines line-id)]
      (-> line
        (assoc :initial-line? (= id (get-in dialogues [dialogue-id :initial-line-id])))
        (assoc :option-count (count (:options line)))
        (update :info-ids #(map str %))))))

(reg-sub
  :dialogue/player-line-option
  :<- [:db-lines]
  (fn [lines [_ line-id index]]
    (-> (get-in lines [line-id :options index])
        (update :required-info-ids #(map str %)))))

(reg-sub
  :info-list
  :<- [:db-infos]
  (fn [infos] (map-values (fn [{id :entity/id :as info}]
                            (assoc info :id id))
                          infos)))

(reg-sub
  :info-options
  :<- [:db-infos]
  (fn [infos]
    (map (fn [info] {:label (:description info)
                     :value (str (:entity/id info))})
         (vals infos))))

(reg-sub
  :ui/positions
  (fn [db]
    (:ui/positions db)))

(reg-sub
  :ui/position
  :<- [:ui/positions]
  :<- [:db-dragging]
  :<- [:db-cursor]
  (fn [[positions {:keys [ids cursor-start]} cursor] [_ id]]
    (let [position (get positions id)]
      (if (contains? ids id)
        (let [delta (translate-point cursor cursor-start -)]
          (translate-point position delta))
        position))))

(reg-sub
  :info
  :<- [:db-infos]
  (fn [infos [_ info-id]]
    (infos info-id)))

(reg-sub
  :character-options
  :<- [:db-characters]
  (fn [characters _]
    (map-values :display-name characters)))

(reg-sub
  :character
  :<- [:db-characters]
  (fn [characters [_ character-id]]
    (characters character-id)))

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
  :<- [:db-cursor]
  (fn [[connecting cursor]]
    (when connecting
      {:start (:cursor-start connecting)
       :end cursor
       :kind :connector})))

(reg-sub
  :dialogue-list
  :<- [:db-dialogues]
  :<- [:db-locations]
  :<- [:db-characters]
  (fn [[dialogues locations characters]]
    (map-values (fn [{id :entity/id :keys [character-id location-id] :as dialogue}]
                  (let [character (characters character-id)
                        location (locations location-id)]
                    (assoc dialogue
                           :id id
                           :character (merge {:id character-id} character)
                           :texture (:texture character)
                           :location (merge {:id location-id} location))))
                dialogues)))

(reg-sub
  :dialogue
  :<- [:db-lines]
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[lines dialogues characters positions] [_ dialogue-id]]
    (let [dialogue (get dialogues dialogue-id)
          dialogue-lines (where-map :dialogue-id dialogue-id lines)
          lines-by-kind (group-by :kind (vals dialogue-lines))]
      {:line-ids (keys dialogue-lines)
       :npc-connections (->> (lines-by-kind :npc)
                             (remove #(nil? (:next-line-id %)))
                             (map #(vector (:entity/id %) (:next-line-id %))))
       :player-connections (reduce
                             (fn [acc {start :entity/id :keys [options]}]
                               (apply conj acc (->> options
                                                    (remove #(nil? (:next-line-id %)))
                                                    (map-indexed (fn [index {end :next-line-id}]
                                                                   (vector start index end))))))
                             (list)
                             (lines-by-kind :player))})))

(reg-sub
  :dialogue/line
  :<- [:db-lines]
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[lines dialogues characters] [_ line-id]]
    (let [line (get lines line-id)
          character (get characters (:character-id line))
          dialogue (get dialogues (:dialogue-id line))]
      (merge (select-keys line [:kind :text :options :info-ids])
             {:id line-id
              :initial-line? (= (:initial-line-id dialogue) line-id)
              :character-color (:color character)
              :character-name (:display-name character)}))))

(reg-sub
  :location-editor/location
  :<- [:db-locations]
  :<- [:db-characters]
  (fn [[locations characters] [_ location-id]]
    (-> (locations location-id)
        (update :connection-triggers
                (fn [ct] (map-values #(assoc (locations %) :id %)
                                     ct))))))

(reg-sub
  :location-editor/npcs
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[dialogues characters] [_ location-id]]
    (->> (vals dialogues)
         (where :location-id location-id)
         (map (fn [dialogue]
                (let [character (characters (:character-id dialogue))]
                  [(:location-position dialogue)
                   (merge {:id (:entity/id dialogue)}
                          (select-keys character [:texture :display-name]))])))
         (into {}))))

(reg-sub
  :location-options
  :<- [:db-locations]
  (fn [locations _]
    (map-values :display-name locations)))

(reg-sub
  :location-editor/available-npcs
  :<- [:db-characters]
  :<- [:db-dialogues]
  (fn [[characters dialogues] _]
    (->>  dialogues
         (filter-map #(not (contains? % :location-id)))
         (map-values (fn [dialogue]
                       (let [character (characters (:character-id dialogue))]
                         (select-keys character [:texture :display-name])))))))

(reg-sub
  :location-map
  :<- [:db-locations]
  :<- [:db-location-connections]
  (fn [[locations connections] _]
    {:location-ids (keys locations)
     :connections (map sort connections)}))

(reg-sub
  :location-map/location
  :<- [:db-locations]
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[locations dialogues characters] [_ location-id]]
    (let [location-dialogues (where-map :location-id location-id dialogues)]
      (assoc (get locations location-id)
             :id location-id
             :dialogues (map (fn [[_ d]]
                               (let [character (get characters (:character-id d))]
                                 {:dialogue-id (:entity/id d)
                                  :npc-name (:display-name character)
                                  :npc-color (:color character)}))
                             location-dialogues)))))

(reg-sub
  :connected-locations
  :<- [:db-locations]
  :<- [:db-location-connections]
  (fn [[locations connections] [_ location-id]]
    (let [other-location-ids (for [connection connections
                                   :when (contains? connection location-id)]
                               (first (disj connection location-id)))]
      (select-keys locations other-location-ids))))

(reg-sub
  :location-editor-data
  (fn [db]
    (select-keys (:location-editor db)
                 [:highlight
                  :painting?
                  :tool
                  :active-texture])))

(reg-sub
  :game-data
  :<- [:db-locations]
  :<- [:db-dialogues]
  :<- [:db-lines]
  :<- [:db-characters]
  :<- [:db-infos]
  (fn [[locations dialogues lines characters infos] _]
    (let [location-id (uuid "121fb127-fbc8-44b9-ba62-2ca2517b6995")
          location (get locations location-id)
          normalize-tile (fn [tile] (rect->0 (:dimension location) tile))
          location-dialogues (where-map :location-id location-id dialogues)]
      {:dimension (:dimension location)
       :npcs (into {} (map (fn [{tile :location-position character-id :character-id}]
                             (let [{id :entity/id
                                    texture :texture} (get characters character-id)]
                               [(normalize-tile tile) {:id id
                                                       :texture texture}]))
                           (vals location-dialogues)))
       :background (map-keys normalize-tile (:background location))
       :walk-set (into #{} (map normalize-tile (:walk-set location)))
       :infos infos
       :lines (filter-map #(location-dialogues (:dialogue-id %)) lines)
       :dialogues (into {} (map (fn [{:keys [character-id initial-line-id]}]
                                  [character-id initial-line-id])
                                (vals location-dialogues)))})))
