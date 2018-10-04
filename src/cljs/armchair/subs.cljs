(ns armchair.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub subscribe]]
            [clojure.spec.alpha :as s]
            [clojure.set :refer [rename-keys]]
            [datascript.core :as d]
            [armchair.db :as db]
            [armchair.config :as config]
            [armchair.util :refer [where
                                   filter-map
                                   where-map
                                   map-keys
                                   map-values
                                   rect->0
                                   translate-point
                                   translate-positions]]))

(reg-sub :db-characters #(:characters %))
(reg-sub :db-lines #(:lines %))
(reg-sub :db-infos #(:infos %))
(reg-sub :db-locations #(:locations %))
(reg-sub :db-location-connections #(:location-connections %))
(reg-sub :db-dialogues #(:dialogues %))

(reg-sub :db-dragging #(:dragging %))
(reg-sub :db-connecting #(:connecting %))
(reg-sub :db-positions #(:positions %))
(reg-sub :db-cursor #(:cursor %))

(reg-sub :current-page #(:current-page %))
(reg-sub :modal #(:modal %))
(reg-sub :dnd-payload #(:dnd-payload %))
(reg-sub :db-store #(deref (:store %)))

(defn l [item]
  (js/console.log item)
  item)

(reg-sub
  :character-list
  :<- [:db-store]
  (fn [store _]
    (let [counts (->> (d/q '[:find ?n (count ?l)
                             :where
                             [?l :line/npc ?n]]
                           store)
                      (into {}))]
      (->> (d/q '[:find ?n ?name ?color
                  :where
                  [?n :npc/name ?name]
                  [?n :npc/color ?color]]
                store)
           (map (fn [[id name color]]
                  {:id id
                   :display-name name
                   :color color
                   :line-count (get counts id 0)}))))))

(reg-sub
  :line
  :<- [:db-lines]
  :<- [:db-dialogues]
  (fn [[lines dialogues] [_ line-id]]
    (when-let [{:keys [id dialogue-id] :as line} (get lines line-id)]
      (assoc line :initial-line? (= id (get-in dialogues [dialogue-id :initial-line-id]))))))

(reg-sub
  :info-list
  :<- [:db-infos]
  (fn [infos] infos))

(reg-sub
  :info-options
  :<- [:db-infos]
  (fn [infos]
    (map (fn [info] {:label (:description info)
                     :value (:id info)})
         (vals infos))))

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
  :dragged-positions
  :<- [:db-dragging]
  :<- [:db-positions]
  :<- [:db-cursor]
  (fn [[dragging positions cursor]]
    (if-let [{:keys [position-ids cursor-start]} dragging]
      (translate-positions positions position-ids (translate-point cursor cursor-start -))
      positions)))

(reg-sub
  :dialogue-list
  :<- [:db-dialogues]
  :<- [:db-locations]
  :<- [:db-characters]
  (fn [[dialogues locations characters]]
    (map-values (fn [{:keys [character-id location-id] :as dialogue}]
                  (assoc dialogue
                         :character (select-keys (characters character-id) [:id :display-name])
                         :location (select-keys (locations location-id) [:id :display-name])))
                dialogues)))

(reg-sub
  :dialogue
  :<- [:db-lines]
  :<- [:db-dialogues]
  :<- [:db-characters]
  :<- [:dragged-positions]
  (fn [[lines dialogues characters positions] [_ dialogue-id]]
    (let [dialogue (get dialogues dialogue-id)
          dialogue-lines (->> lines
                              (where-map :dialogue-id dialogue-id)
                              (map-values (fn [{:keys [id character-id position-id] :as line}]
                                            (assoc line
                                                   :initial-line? (= (:initial-line-id dialogue) id)
                                                   :position (get positions position-id)
                                                   :character-color (get-in characters [character-id :color])
                                                   :character-name (get-in characters [character-id :display-name])))))
          lines-by-kind (group-by :kind (vals dialogue-lines))]
      {:lines dialogue-lines
       :npc-connections (->> (:npc lines-by-kind)
                             (filter #(s/valid? :armchair.db/line-id (:next-line-id %)))
                             (map #(vector (:id %) (:next-line-id %))))
       :player-connections (reduce
                             (fn [acc {:keys [id options]}]
                               (apply conj acc (->> options
                                                    (map-indexed #(vector id %1 (:next-line-id %2)))
                                                    (filter #(s/valid? :armchair.db/line-id (nth % 2))))))
                             (list)
                             (:player lines-by-kind))})))

(reg-sub
  :location
  :<- [:db-locations]
  :<- [:db-characters]
  (fn [[locations characters] [_ location-id]]
    (-> (locations location-id)
        (update :npcs #(map-values characters %))
        (update :connection-triggers #(map-values locations %)))))

(reg-sub
  :location-options
  :<- [:db-locations]
  (fn [locations _]
    (map-values :display-name locations)))

(reg-sub
  :available-npcs
  :<- [:db-locations]
  :<- [:db-characters]
  (fn [[locations characters] [_ location-id]]
    (let [placed-characters (->> (vals locations)
                                 (map :npcs)
                                 (filter some?)
                                 (apply merge)
                                 vals)]
      (apply dissoc (into [characters] placed-characters)))))

(reg-sub
  :ui/position
  :<- [:db-store]
  (fn [store [_ entity-id]]
    (:ui/position (d/pull store [:ui/position] entity-id))))


(reg-sub
  :location-map
  :<- [:db-store]
  (fn [store _]
    {:location-ids (->> (d/q '[:find ?location-id
                               :where [?location-id :location/name]]
                             store)
                        (map (fn [[id]] id)))
     :connections (d/q '[:find ?start ?end
                         :where
                         [?s :location/connections ?e]
                         [?s :ui/position ?start]
                         [?e :ui/position ?end]]
                       store)}))

(reg-sub
  :location-map/location
  :<- [:db-store]
  (fn [store [_ location-id]]
    (let [res (d/pull store [:location/name
                             {:location/dialogues [:db/id
                                                   {:npc/_dialogues [:npc/name :npc/color]}]}]
                      location-id)]
      {:display-name (:location/name res)
       :dialogues (map (fn [{id :db/id {npc-name :npc/name
                                        npc-color :npc/color} :npc/_dialogues}]
                         {:dialogue-id id
                          :npc-name npc-name
                          :npc-color npc-color})
                       (:location/dialogues res))})))

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
    (let [location (get locations 1)
          normalize-tile (fn [tile] (rect->0 (:dimension location) tile))
          location-dialogues (where-map :location-id 1 dialogues)]
      {:dimension (:dimension location)
       :npcs (into {} (map (fn [[tile npc]]
                             [(normalize-tile tile) (get characters npc)])
                           (:npcs location)))
       :background (map-keys normalize-tile (:background location))
       :walk-set (into #{} (map normalize-tile (:walk-set location)))
       :infos infos
       :lines (filter-map #(location-dialogues (:dialogue-id %)) lines)
       :dialogues (into {} (map (fn [{:keys [character-id initial-line-id]}]
                                  [character-id initial-line-id])
                                (vals location-dialogues)))})))
