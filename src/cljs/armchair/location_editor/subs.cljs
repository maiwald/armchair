(ns armchair.location-editor.subs
  (:require [re-frame.core :refer [reg-sub]]
            [armchair.util :as u :refer [where filter-map]]))

(reg-sub
  :location-editor/ui
  (fn [db]
    (select-keys (:location-editor db)
                 [:highlight
                  :tool
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
  :location-editor/location
  :<- [:db-locations]
  (fn [locations [_ location-id]]
    (-> (locations location-id)
        (u/update-values
          :connection-triggers
          (fn [id]
            (let [{:keys [connection-triggers dimension display-name]} (locations id)
                  target ((u/reverse-map connection-triggers) location-id)]
              {:id id
               :target target
               :target-normalized (u/rect->0 dimension target)
               :display-name display-name}))))))

(reg-sub
  :location-editor/npcs
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[dialogues characters] [_ location-id]]
    (->> (vals dialogues)
         (where :location-id location-id)
         (map (fn [{dialogue-id :entity/id
                    dialogue-synopsis :synopsis
                    :keys [character-id location-position]}]
                (let [character (characters character-id)]
                  [location-position
                   (merge {:id (:entity/id character)
                           :dialogue-id dialogue-id
                           :dialogue-synopsis dialogue-synopsis}
                          (select-keys character [:texture :display-name]))])))
         (into {}))))

(reg-sub
  :location-editor/available-npcs
  :<- [:db-characters]
  :<- [:db-dialogues]
  (fn [[characters dialogues] _]
    (let [placed-characters (->> (vals dialogues)
                                 (filter #(contains? % :location-id))
                                 (reduce #(conj %1 (:character-id %2)) #{}))]
      (filter-map #(not (contains? placed-characters (:entity/id %)))
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
  :location-editor/connected-locations
  :<- [:db-locations]
  :<- [:db-location-connections]
  (fn [[locations connections] [_ location-id]]
    (let [other-location-ids (for [connection connections
                                   :when (contains? connection location-id)]
                               (first (disj connection location-id)))]
      (select-keys locations other-location-ids))))
