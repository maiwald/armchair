(ns armchair.location-editor.subs
  (:require [re-frame.core :refer [reg-sub]]
            [armchair.util :refer [where map-values filter-map]]))

(reg-sub
  :location-editor/ui
  (fn [db]
    (select-keys (:location-editor db)
                 [:highlight
                  :painting?
                  :tool
                  :active-texture])))

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
  :location-editor/connected-locations
  :<- [:db-locations]
  :<- [:db-location-connections]
  (fn [[locations connections] [_ location-id]]
    (let [other-location-ids (for [connection connections
                                   :when (contains? connection location-id)]
                               (first (disj connection location-id)))]
      (select-keys locations other-location-ids))))

