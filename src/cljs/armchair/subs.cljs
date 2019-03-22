(ns armchair.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub subscribe]]
            [clojure.string :refer [join]]
            [armchair.routes :refer [routes]]
            [armchair.config :as config]
            [bidi.bidi :refer [match-route]]
            [armchair.util :as u]))

(reg-sub :db-characters #(:characters %))
(reg-sub :db-lines #(:lines %))
(reg-sub :db-locations #(:locations %))
(reg-sub :db-dialogues #(:dialogues %))
(reg-sub :db-player #(:player %))
(reg-sub :db-player-options #(:player-options %))
(reg-sub :db-triggers #(:triggers %))
(reg-sub :db-switches #(:switches %))
(reg-sub :db-switch-values #(:switch-values %))

(reg-sub :db-location-connections #(:location-connections %))

(reg-sub :db-dragging #(:dragging %))
(reg-sub :db-connecting #(:connecting %))
(reg-sub :db-cursor #(:cursor %))

(reg-sub :current-page #(:current-page %))
(reg-sub :modal #(:modal %))

(reg-sub :dnd-payload #(:dnd-payload %))

(reg-sub
  :modal/switch-form
  :<- [:modal]
  (fn [modal]
    (if-let [{:keys [display-name values]} (:switch-form modal)]
      {:display-name display-name
       :values (for [[index v] (zipmap (range (count values)) values)
                     :when (not (contains? v :deleted))]
                 [index (:display-name v)])})))

(reg-sub
  :modal/conditions-form
  :<- [:modal]
  :<- [:db-switches]
  :<- [:db-switch-values]
  (fn [[modal switches switch-values]]
    (if-let [{:keys [terms conjunction]} (:conditions-form modal)]
      (let [switch-options (u/map-values :display-name switches)
            used-switches (set (map :switch-id terms))
            operator-options (u/map-values :display-name config/condition-operators)]
        {:conjunction conjunction
         :terms
         (map-indexed
           (fn [index {:keys [switch-id] :as c}]
             (let [value-options (->> switch-id
                                      switches
                                      :value-ids
                                      (map (fn [id]
                                             [id (-> id
                                                     switch-values
                                                     :display-name)])))]
               [index (assoc c
                             :switch-options (apply dissoc switch-options
                                                    (disj used-switches switch-id))
                             :operator-options operator-options
                             :switch-value-options value-options)]))
           terms)}))))

(reg-sub
  :character-list
  :<- [:db-characters]
  :<- [:db-lines]
  (fn [[characters lines] _]
    (let [line-counts (->> (vals lines)
                           (u/where :kind :npc)
                           (group-by :character-id)
                           (u/map-values count))]
      (u/map-values (fn [{id :entity/id :as character}]
                      (assoc character
                             :id id
                             :line-count (get line-counts id 0)))
                    characters))))

(reg-sub
  :switch-list
  :<- [:db-switches]
  :<- [:db-switch-values]
  (fn [[switches switch-values] _]
    (let [value-name (comp :display-name switch-values)]
      (map
        (fn [[id {:keys [display-name value-ids]}]]
          {:id id
           :display-name display-name
           :values (->> value-ids (map value-name) (join ", "))})
        switches))))

(reg-sub
  :dialogue/modal-line
  :<- [:db-lines]
  :<- [:db-dialogues]
  (fn [[lines dialogues] [_ line-id]]
    (when-let [{id :entity/id :keys [dialogue-id] :as line} (get lines line-id)]
      (-> line
          (assoc :initial-line? (= id (get-in dialogues [dialogue-id :initial-line-id])))
          (assoc :option-count (count (:options line)))))))

(reg-sub
  :dialogue/player-line-option
  :<- [:db-lines]
  :<- [:db-player-options]
  (fn [[lines options] [_ line-id index]]
    (let [option-id (get-in lines [line-id :options index])]
      (get-in options [option-id :text]))))

(reg-sub
  :trigger-creation/switch-options
  :<- [:modal]
  :<- [:db-triggers]
  :<- [:db-lines]
  :<- [:db-switches]
  :<- [:db-switch-values]
  (fn [[{{:keys [trigger-node-id switch-id]} :trigger-creation} triggers lines switches switch-values]]
    (let [used-switches (->> (get-in lines [trigger-node-id :trigger-ids])
                             (map #(get-in triggers [% :switch-id]))
                             set)]
      {:switch-options (->> switches
                            (u/filter-map (fn [{id :entity/id}]
                                            (not (contains? used-switches id))))
                            (u/map-values :display-name))
       :value-options (when switch-id
                        (map
                          (fn [id]
                            [id (get-in switch-values [id :display-name])])
                          (get-in switches [switch-id :value-ids])))})))


(reg-sub
  :trigger-creation/dialogue-state-options
  :<- [:modal]
  :<- [:db-triggers]
  :<- [:db-lines]
  :<- [:db-dialogues]
  (fn [[{{:keys [trigger-node-id switch-id]} :trigger-creation} triggers lines dialogues]]
    (let [used-switches (->> (get-in lines [trigger-node-id :trigger-ids])
                             (map #(get-in triggers [% :switch-id]))
                             set)]
      {:switch-options (->> dialogues
                            (u/filter-map (fn [{states :states id :entity/id}]
                                            (and (not (contains? used-switches id))
                                                 (seq states))))
                            (u/map-values :synopsis))
       :value-options (when switch-id
                        (let [{:keys [states initital-line-id]} (dialogues switch-id)]
                          (conj (seq states)
                                [initital-line-id "Initial Line"])))})))

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
        (let [delta (u/point-delta cursor-start cursor)]
          (u/translate-point position delta))
        position))))

(reg-sub
  :character-options
  :<- [:db-characters]
  (fn [characters _]
    (u/map-values :display-name characters)))

(reg-sub
  :dialogue-creation/character-options
  :<- [:db-characters]
  :<- [:db-dialogues]
  (fn [[characters dialogues] _]
    (let [with-dialogue (reduce
                          #(conj %1 (:character-id %2))
                          #{}
                          (vals dialogues))]
      (->> characters
           (u/filter-map #(not (contains? with-dialogue (:entity/id %))))
           (u/map-values :display-name)))))

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
    (= (:ids dragging) #{position-id})))

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
    (u/map-values
      (fn [{id :entity/id :keys [synopsis character-id location-id]}]
        (let [character (characters character-id)
              location (locations location-id)]
          {:id id
           :synopsis synopsis
           :character (merge {:id character-id} character)
           :texture (:texture character)
           :location (merge {:id location-id} location)}))
      dialogues)))

(reg-sub
  :location-options
  :<- [:db-locations]
  (fn [locations _]
    (u/map-values :display-name locations)))

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
    (let [location-dialogues (u/where-map :location-id location-id dialogues)]
      (assoc (get locations location-id)
             :id location-id
             :dialogues (map (fn [[_ d]]
                               (let [character (get characters (:character-id d))]
                                 {:dialogue-id (:entity/id d)
                                  :npc-name (:display-name character)
                                  :npc-color (:color character)}))
                             location-dialogues)))))

(reg-sub
  :breadcrumb
  :<- [:current-page]
  :<- [:db-locations]
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[current-page locations dialogues characters]]
    (let [{page-name :handler
           page-params :route-params} (match-route routes current-page)
          id (when (:id page-params) (uuid (:id page-params)))]
      (condp = page-name
        :location-edit {:location {:id id
                                   :display-name
                                   (get-in locations [id :display-name])}}
        :dialogue-edit (let [{:keys [character-id location-id synopsis]}
                             (get dialogues id)
                             character-name
                             (get-in characters [character-id :display-name])]
                         {:location {:id location-id
                                     :display-name
                                     (get-in locations [location-id :display-name])}
                          :dialogue {:id id
                                     :character-name character-name
                                     :synopsis synopsis}})
        nil))))
