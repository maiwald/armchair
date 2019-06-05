(ns armchair.location-editor.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [com.rpl.specter
             :refer [keypath ALL MAP-KEYS MAP-VALS]
             :refer-macros [select select-one!]]
            [armchair.util :as u]))

(reg-sub
  :location-editor/ui
  (fn [db]
    (select-keys (:location-editor db)
                 [:highlight
                  :active-pane
                  :active-tool
                  :visible-layers
                  :active-layer
                  :active-walk-state
                  :active-texture])))

(reg-sub
  :location-editor/player-position
  (fn [{{:keys [location-id location-position]} :player} [_ current-location]]
    (when (= location-id current-location) location-position)))

(reg-sub
  :location-editor/location-exists?
  :<- [:db-locations]
  (fn [locations [_ location-id]]
    (contains? locations location-id)))

(reg-sub
  :location-editor/npcs
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[dialogues characters] [_ location-id]]
    (->> (vals dialogues)
         (u/where :location-id location-id)
         (map (fn [{:keys [character-id location-position]}]
                (let [character (characters character-id)]
                  [location-position
                   (merge {:id character-id}
                          (select-keys character [:texture :display-name]))])))
         (into {}))))

(reg-sub
  :location-editor/location
  (fn [[_ location-id]]
    [(subscribe [:db-locations])
     (subscribe [:location-editor/npcs location-id])
     (subscribe [:location-editor/player-position location-id])])
  (fn [[locations npcs player-position] [_ location-id]]
    (-> (locations location-id)
        (cond->
          (some? player-position)
          (assoc :player-position player-position))
        (assoc :npcs npcs)
        (u/update-values
          :connection-triggers
          (fn [[target-id _]]
            (get-in locations [target-id :display-name]))))))

(reg-sub
  :location-editor/available-npcs
  :<- [:db-characters]
  :<- [:db-dialogues]
  (fn [[characters dialogues] _]
    (let [placed-characters (->> (vals dialogues)
                                 (filter #(contains? % :location-id))
                                 (reduce #(conj %1 (:character-id %2)) #{}))]
      (u/filter-map #(not (contains? placed-characters (:entity/id %)))
                    characters))))

(reg-sub
  :location-editor/display-name
  :<- [:db-locations]
  (fn [locations [_ location-id]]
    (get-in locations [location-id :display-name])))

(reg-sub
  :location-editor/dimensions
  :<- [:db-locations]
  (fn [locations [_ location-id]]
    (let [dimension (get-in locations [location-id :dimension])]
      {:width (u/rect-width dimension)
       :height (u/rect-height dimension)})))

(reg-sub
  :location-editor/npc-popover
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[dialogues characters] [_ location-id tile]]
    (let [{:keys [character-id]
           dialogue-id :entity/id
           dialogue-synopsis :synopsis}
          (select-one! [MAP-VALS #(u/submap? {:location-id location-id :location-position tile} %)]
                       dialogues)
          character (characters character-id)]
      (merge {:id (:entity/id character)
              :dialogue-id dialogue-id
              :dialogue-synopsis dialogue-synopsis}
             (select-keys character [:texture :display-name])))))

(reg-sub
  :location-editor/trigger-popover
  :<- [:db-locations]
  (fn [locations [_ location-id tile]]
    (let [[target-id position] (get-in locations [location-id :connection-triggers tile])
          {:keys [dimension display-name]} (locations target-id)]
      {:id target-id
       :position position
       :position-normalized (u/rect->0 dimension position)
       :display-name display-name})))

(reg-sub
  :location-editor/occupied-tiles
  :<- [:db-locations]
  :<- [:db-dialogues]
  :<- [:db-player]
  (fn [[locations dialogues player] [_ location-id]]
    (let [triggers (select [(keypath location-id)
                            :connection-triggers
                            MAP-KEYS]
                           locations)
          npcs (select [MAP-VALS
                        #(= location-id (:location-id %))
                        :location-position]
                       dialogues)
          player (when (= location-id (:location-id player))
                   [(:location-position player)])]
      (set (concat triggers npcs player)))))

(reg-sub
  :location-editor/physically-occupied-tiles
  :<- [:db-locations]
  :<- [:db-dialogues]
  :<- [:db-player]
  (fn [[locations dialogues player] [_ location-id]]
    (let [blocked (get-in locations [location-id :blocked])
          npcs (select [MAP-VALS
                        #(= location-id (:location-id %))
                        :location-position]
                       dialogues)
          player (when (= location-id (:location-id player))
                   [(:location-position player)])]
      (set (concat blocked npcs player)))))
