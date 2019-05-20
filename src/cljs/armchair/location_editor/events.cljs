(ns armchair.location-editor.events
  (:require [armchair.events :refer [reg-event-data reg-event-meta]]
            [clojure.set :refer [rename-keys]]
            [armchair.undo :refer [record-undo]]
            [armchair.util :as u]))

(reg-event-data
  :location-editor/update-name
  (fn [db [_ id value]]
    (assoc-in db [:locations id :display-name] value)))

(reg-event-meta
  :location-editor/set-active-texture
  (fn [db [_ texture]]
    (assoc-in db [:location-editor :active-texture] texture)))

(reg-event-meta
  :location-editor/set-active-walk-state
  (fn [db [_ value]]
    (assoc-in db [:location-editor :active-walk-state] value)))

(reg-event-meta
  :location-editor/set-highlight
  (fn [db [_ tile]]
    (assoc-in db [:location-editor :highlight] tile)))

(reg-event-meta
  :location-editor/unset-highlight
  (fn [db [_ tile]]
    (update db :location-editor dissoc :highlight)))

(reg-event-meta
  :location-editor/set-active-pane
  (fn [db [_ pane]]
    (assoc-in db [:location-editor :active-pane] pane)))

(reg-event-meta
  :location-editor/set-active-layer
  (fn [db [_ layer-id]]
    (assoc-in db [:location-editor :active-layer] layer-id)))

(reg-event-meta
  :location-editor/toggle-layer-visibility
  (fn [db [_ layer-id]]
    (update-in db [:location-editor :visible-layers]
               (fn [visible-layers]
                 (if (contains? visible-layers layer-id)
                   (disj visible-layers layer-id)
                   (conj visible-layers layer-id))))))

(reg-event-meta
  :location-editor/start-entity-drag
  (fn [db [_ payload]]
    (assoc db :dnd-payload payload)))

(reg-event-meta
  :location-editor/stop-entity-drag
  (fn [db]
    (-> db
      (dissoc :dnd-payload)
      (update :location-editor dissoc :highlight))))

(reg-event-data
  :location-editor/move-player
  (fn [db [_ location-id position]]
    (-> db
       (dissoc :dnd-payload)
       (update :location-editor dissoc :highlight)
       (assoc :player {:location-id location-id
                       :location-position position}))))

(reg-event-data
  :location-editor/move-character
  (fn [db [_ location-id character-id to]]
    (let [new-db (-> db
                     (dissoc :dnd-payload)
                     (update :location-editor dissoc :highlight))
          dialogue-lookup (->> (:dialogues db)
                               vals
                               (reduce (fn [acc {d-id :entity/id
                                                 c-id :character-id}]
                                         (assoc acc c-id d-id))
                                       {}))]
      (if-let [dialogue-id (get dialogue-lookup character-id)]
        (update-in new-db [:dialogues dialogue-id]
                   assoc
                   :location-id location-id
                   :location-position to)
        (assoc-in new-db [:modal :dialogue-creation]
                  {:character-id character-id
                   :location-id location-id
                   :location-position to})))))

(reg-event-data
  :location-editor/remove-character
  (fn [db [_ character-id]]
    (let [dialogue-lookup (->> (:dialogues db)
                               vals
                               (reduce (fn [acc {d-id :entity/id
                                                 c-id :character-id}]
                                         (assoc acc c-id d-id))
                                       {}))]
      (-> db
          (update-in [:dialogues (dialogue-lookup character-id)]
                     dissoc :location-id :location-position)))))

(reg-event-data
  :location-editor/move-trigger
  (fn [db [_ location from to]]
    (let [new-db (-> db
                     (dissoc :dnd-payload)
                     (update :location-editor dissoc :highlight))]
      (if (some? from)
        (update-in new-db
                   [:locations location :connection-triggers]
                   (fn [cts] (rename-keys cts {from to})))
        (assoc-in new-db
                  [:modal :connection-trigger-creation]
                  {:location-id location
                   :location-position to})))))

(reg-event-data
  :location-editor/remove-trigger
  (fn [db [_ location from]]
    (update-in db [:locations location :connection-triggers] #(dissoc % from))))

(reg-event-data
  :location-editor/paint
  (fn [db [_ location-id tile]]
    (let [{:keys [active-texture]} (:location-editor db)]
      (assoc-in db [:locations location-id :background tile] active-texture))))

(reg-event-data
  :location-editor/set-walkable
  (fn [db [_ location-id tile]]
    (let [add (get-in db [:location-editor :active-walk-state])]
      (update-in db [:locations location-id :blocked] (if add disj conj) tile))))

(reg-event-data
  :location-editor/resize-smaller
  (fn [db [_ location-id direction]]
    (let [[shift-index shift-delta] (case direction
                                      :up [0 [0 1]]
                                      :left [0 [1 0]]
                                      :right [1 [-1 0]]
                                      :down [1 [0 -1]])
          new-dimension (update (get-in db [:locations location-id :dimension])
                                shift-index u/translate-point shift-delta)
          in-bounds? (partial u/rect-contains? new-dimension)
          remove-oob (fn [coll] (u/filter-keys in-bounds? coll))]
      (update-in db [:locations location-id]
                 (fn [location]
                   (-> location
                       (assoc :dimension new-dimension)
                       (update :background remove-oob)
                       (update :npcs remove-oob)
                       (update :connection-triggers remove-oob)
                       (update :blocked (comp set #(filter in-bounds? %)))))))))

(reg-event-data
  :location-editor/resize-larger
  (fn [db [_ location-id direction]]
    (let [[shift-index shift-delta] (case direction
                                      :up [0 [0 -1]]
                                      :left [0 [-1 0]]
                                      :right [1 [1 0]]
                                      :down [1 [0 1]])]
      (update-in db [:locations
                     location-id
                     :dimension
                     shift-index]
                 u/translate-point shift-delta))))
