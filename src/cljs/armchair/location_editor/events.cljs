(ns armchair.location-editor.events
  (:require [re-frame.core :refer [reg-event-db]]
            [armchair.events :refer [validate]]
            [armchair.undo :refer [record-undo]]
            [armchair.util :refer [translate-point rect-contains? filter-keys filter-map]]))

(reg-event-db
  :location-editor/update-name
  [validate
   record-undo]
  (fn [db [_ id value]]
    (assoc-in db [:locations id :display-name] value)))

(reg-event-db
  :location-editor/set-tool
  [validate]
  (fn [db [_ tool]]
    (assoc-in db [:location-editor :tool] tool)))

(reg-event-db
  :location-editor/set-active-texture
  [validate]
  (fn [db [_ texture]]
    (assoc-in db [:location-editor :active-texture] texture)))

(reg-event-db
  :location-editor/set-highlight
  [validate]
  (fn [db [_ tile]]
    (assoc-in db [:location-editor :highlight] tile)))

(reg-event-db
  :location-editor/unset-highlight
  [validate]
  (fn [db]
    (update db :location-editor dissoc :highlight)))

(reg-event-db
  :location-editor/start-entity-drag
  [validate]
  (fn [db [_ payload]]
    (assoc db :dnd-payload payload)))

(reg-event-db
  :location-editor/stop-entity-drag
  [validate]
  (fn [db]
    (dissoc db :dnd-payload)))

(reg-event-db
  :location-editor/move-dialogue
  [validate
   record-undo]
  (fn [db [_ location-id dialogue-id to]]
    (-> db
        (dissoc :dnd-payload)
        (update :location-editor dissoc :highlight)
        (update-in [:dialogues dialogue-id]
                   assoc
                   :location-id location-id
                   :location-position to))))

(reg-event-db
  :location-editor/remove-dialogue
  [validate
   record-undo]
  (fn [db [_ dialogue-id]]
    (-> db
        (dissoc :dnd-payload)
        (update-in [:dialogues dialogue-id] dissoc :location-id :location-position))))

(reg-event-db
  :location-editor/move-trigger
  [validate
   record-undo]
  (fn [db [_ location target to]]
    (-> db
        (dissoc :dnd-payload)
        (update :location-editor dissoc :highlight)
        (update-in [:locations location :connection-triggers] #(as-> % new-db
                                                                 (filter-map (fn [v] (not= v target)) new-db)
                                                                 (assoc new-db to target))))))

(reg-event-db
  :location-editor/start-painting
  [validate
   record-undo]
  (fn [db [_ location-id tile]]
    (let [texture (get-in db [:location-editor :active-texture])]
      (-> db
          (assoc-in [:location-editor :painting?] true)
          (assoc-in [:locations location-id :background tile] texture)))))

(reg-event-db
  :location-editor/paint
  [validate
   record-undo]
  (fn [db [_ location-id tile]]
    (let [{:keys [painting? active-texture]} (:location-editor db)]
      (cond-> db
        painting? (assoc-in [:locations location-id :background tile] active-texture)))))

(reg-event-db
  :location-editor/stop-painting
  [validate]
  (fn [db]
    (assoc-in db [:location-editor :painting?] false)))

(reg-event-db
  :location-editor/flip-walkable
  [validate
   record-undo]
  (fn [db [_ location-id tile]]
    (update-in db [:locations location-id :walk-set] (fn [walk-set]
                                                       (if (contains? walk-set tile)
                                                         (disj walk-set tile)
                                                         (conj walk-set tile))))))

(reg-event-db
  :location-editor/resize-smaller
  [validate
   record-undo]
  (fn [db [_ location-id direction]]
    (let [[shift-index shift-delta] (case direction
                                      :up [0 [0 1]]
                                      :left [0 [1 0]]
                                      :right [1 [-1 0]]
                                      :down [1 [0 -1]])
          new-dimension (update (get-in db [:locations location-id :dimension])
                                shift-index translate-point shift-delta)
          in-bounds? (partial rect-contains? new-dimension)
          remove-oob (fn [coll] (filter-keys in-bounds? coll))]
      (update-in db [:locations location-id]
                 (fn [location]
                   (-> location
                       (assoc :dimension new-dimension)
                       (update :background remove-oob)
                       (update :npcs remove-oob)
                       (update :connection-triggers remove-oob)
                       (update :walk-set (comp set #(filter in-bounds? %)))))))))

(reg-event-db
  :location-editor/resize-larger
  [validate
   record-undo]
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
                 translate-point shift-delta))))
