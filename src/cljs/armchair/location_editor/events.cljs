(ns armchair.location-editor.events
  (:require [armchair.events :refer [reg-event-data reg-event-meta]]
            [clojure.set :refer [rename-keys]]
            [com.rpl.specter
             :refer [multi-path ALL NONE MAP-KEYS MAP-VALS]
             :refer-macros [setval transform]]
            [armchair.undo :refer [record-undo]]
            [armchair.math :refer [rect-resize rect-contains?]]
            [armchair.util :as u]))

(reg-event-data
  :location-editor/update-name
  (fn [db [_ id value]]
    (assoc-in db [:locations id :display-name] value)))

(reg-event-meta
  :location-editor/set-active-texture
  (fn [db [_ texture]]
    (update db :location-editor
               merge {:active-texture texture
                      :active-tool :brush})))

(reg-event-meta
  :location-editor/set-active-tool
  (fn [db [_ tool]]
    (assoc-in db [:location-editor :active-tool] tool)))

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
    (update-in db [:locations location :connection-triggers] dissoc from)))

(reg-event-data
  :location-editor/paint
  (fn [db [_ location-id layer-id tile]]
    (let [{:keys [active-tool active-texture]} (:location-editor db)]
      (case active-tool
        :brush (assoc-in db [:locations location-id layer-id tile] active-texture)
        :eraser (update-in db [:locations location-id layer-id] dissoc tile)))))

(reg-event-data
  :location-editor/set-walkable
  (fn [db [_ location-id tile]]
    (let [add (get-in db [:location-editor :active-walk-state])]
      (update-in db [:locations location-id :blocked] (if add disj conj) tile))))

(reg-event-data
  :location-editor/resize-smaller
  (fn [db [_ location-id direction]]
    (let [dimension (get-in db [:locations location-id :dimension])
          side (case direction
                 :up :top
                 :down :bottom
                 :left :left
                 :right :right)
          new-dimension (rect-resize dimension {side -1})
          out-of-bounds? (fn [point] (not (rect-contains? new-dimension point)))
          loc-and-out-of-bounds? (fn [id point] (and (= location-id id)
                                                     (out-of-bounds? point)))]
      (->> (update-in db [:locations location-id]
                      (fn [location]
                        (->> (assoc location :dimension new-dimension)
                             (setval [(multi-path :background1
                                                  :background2
                                                  :foreground1
                                                  :foreground2
                                                  :connection-triggers) MAP-KEYS out-of-bounds?] NONE)
                             (setval [:blocked ALL out-of-bounds?] NONE))))

           ;; remove OOB dialogues
           (transform [:dialogues
                       MAP-VALS
                       #(loc-and-out-of-bounds? (:location-id %) (:location-position %))]
                      #(dissoc % :location-id :location-position))

           ;; remove incoming OOB connections
           (setval [:locations MAP-VALS :connection-triggers ALL
                    (fn [[_ [target-id target-position]]]
                      (loc-and-out-of-bounds? target-id target-position))]
                   NONE)))))

(reg-event-data
  :location-editor/resize-larger
  (fn [db [_ location-id direction]]
    (let [side (case direction
                 :up :top
                 :down :bottom
                 :left :left
                 :right :right)]
      (update-in db [:locations location-id :dimension]
                 (fn [dimension]
                   (rect-resize dimension {side 1}))))))
