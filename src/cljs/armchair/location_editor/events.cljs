(ns armchair.location-editor.events
  (:require [armchair.events :refer [reg-event-data reg-event-meta]]
            [armchair.location-previews :refer [build-location-preview]]
            [clojure.set :refer [rename-keys]]
            [com.rpl.specter
             :refer [multi-path ALL NONE MAP-KEYS MAP-VALS]
             :refer-macros [setval transform]]
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
    (assoc-in db [:location-editor :dnd-payload] payload)))

(reg-event-meta
  :location-editor/stop-entity-drag
  (fn [db]
    (update db :location-editor dissoc :highlight :dnd-payload)))

(reg-event-data
  :location-editor/move-player
  (fn [db [_ location-id position]]
    (-> db
       (update :location-editor dissoc :highlight :dnd-payload)
       (assoc :player {:location-id location-id
                       :location-position position}))))

(reg-event-data
  :location-editor/move-placement
  (fn [db [_ location-id from to]]
    (-> db
        (cond-> (let [[inspector-type {inspector-id :location-id
                                       inspector-position :location-position}] (:ui/inspector db)]
                  (and (= inspector-type :placement)
                       (= inspector-id location-id)
                       (= inspector-position from)))
          (assoc-in [:ui/inspector 1 :location-position] to))
        (update :location-editor dissoc :highlight :dnd-payload)
        (update-in [:locations location-id :placements] rename-keys {from to}))))

(reg-event-data
  :location-editor/place-character
  (fn [db [_ location-id character-id to]]
    (-> db
        (update :location-editor dissoc :highlight :dnd-payload)
        (assoc-in [:locations location-id :placements to]
                  {:character-id character-id}))))

(reg-event-data
  :location-editor/remove-placement
  (fn [db [_ location-id tile]]
    (-> db
        (cond-> (let [[inspector-type {inspector-location-id :location-id
                                       inspector-location-position :location-position}] (:ui/inspector db)]
                  (and (= inspector-type :placement)
                       (= inspector-location-id location-id)
                       (= inspector-location-position tile)))
          (dissoc :ui/inspector))
        (update-in [:locations location-id :placements] dissoc tile))))

(reg-event-data
  :location-editor/set-placement-character
  (fn [db [_ location-id tile character-id]]
    (assoc-in db [:locations location-id :placements tile]
              {:character-id character-id})))

(reg-event-data
  :location-editor/set-placement-dialogue
  (fn [db [_ location-id tile dialogue-id]]
    (if (some? dialogue-id)
      (assoc-in db [:locations location-id :placements tile :dialogue-id] dialogue-id)
      (update-in db [:locations location-id :placements tile] dissoc :dialogue-id))))

(reg-event-data
  :location-editor/move-trigger
  (fn [db [_ location-id from to]]
    (let [new-db (update db :location-editor dissoc :highlight :dnd-payload)]
      (if (some? from)
        (-> new-db
            (cond-> (let [[inspector-type {inspector-location-id :location-id
                                           inspector-location-position :location-position}] (:ui/inspector db)]
                      (and (= inspector-type :exit)
                           (= inspector-location-id location-id)
                           (= inspector-location-position from)))
              (assoc-in [:ui/inspector 1 :location-position] to))
          (update-in [:locations location-id :connection-triggers]
                     rename-keys {from to}))
        (assoc-in new-db
                  [:modal :connection-trigger-creation]
                  {:location-id location-id
                   :location-position to})))))

(reg-event-data
  :location-editor/remove-trigger
  (fn [db [_ location-id tile]]
    (-> db
        (cond-> (let [[inspector-type {inspector-location-id :location-id
                                       inspector-location-position :location-position}] (:ui/inspector db)]
                  (and (= inspector-type :exit)
                       (= inspector-location-id location-id)
                       (= inspector-location-position tile)))
          (dissoc :ui/inspector))
      (update-in [:locations location-id :connection-triggers] dissoc tile))))

(reg-event-data
  :location-editor/paint
  [build-location-preview]
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
  [build-location-preview]
  (fn [db [_ location-id direction]]
    (let [bounds (get-in db [:locations location-id :bounds])
          side (case direction
                 :up :top
                 :down :bottom
                 :left :left
                 :right :right)
          new-bounds (rect-resize bounds {side -1})
          out-of-bounds? (fn [point] (not (rect-contains? new-bounds point)))
          loc-and-out-of-bounds? (fn [id point] (and (= location-id id)
                                                     (out-of-bounds? point)))]
      (->> (update-in db [:locations location-id]
                      (fn [location]
                        (->> (assoc location :bounds new-bounds)
                             (setval [(multi-path :background1
                                                  :background2
                                                  :foreground1
                                                  :foreground2
                                                  :connection-triggers
                                                  :placements)
                                      MAP-KEYS out-of-bounds?] NONE)
                             (setval [:blocked ALL out-of-bounds?] NONE))))

           ;; remove incoming OOB connections
           (setval [:locations MAP-VALS :connection-triggers ALL
                    (fn [[_ [target-id target-position]]]
                      (loc-and-out-of-bounds? target-id target-position))]
                   NONE)))))

(reg-event-data
  :location-editor/resize-larger
  [build-location-preview]
  (fn [db [_ location-id direction]]
    (let [side (case direction
                 :up :top
                 :down :bottom
                 :left :left
                 :right :right)]
      (update-in db [:locations location-id :bounds]
                 (fn [bounds]
                   (rect-resize bounds {side 1}))))))
