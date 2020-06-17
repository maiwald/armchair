(ns armchair.location-editor.events
  (:require [armchair.events :refer [reg-event-data reg-event-meta]]
            [armchair.location-previews :refer [build-location-preview]]
            [com.rpl.specter
             :refer [multi-path ALL NONE MAP-KEYS MAP-VALS]
             :refer-macros [setval]]
            [armchair.math :refer [rect-resize rect-contains?]]))

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
