(ns armchair.undo
  (:require [re-frame.core :refer [after reg-event-db reg-sub]]
            [re-frame.db :refer [app-db]]
            [armchair.db :refer [content-data]]
            [armchair.local-storage :as ls]))

(defonce undo-list (atom []))
(defonce redo-list (atom []))

(defn can-undo? []
  (not (empty? @undo-list)))

(defn can-redo? []
  (not (empty? @redo-list)))

(reg-sub :can-undo? can-undo?)
(reg-sub :can-redo? can-redo?)

(def record-undo
  (after (fn [db]
           (let [new-state (content-data db)
                 undo-checkpoint (content-data @app-db)]
             (when-not (= new-state undo-checkpoint)
               (reset! redo-list [])
               (swap! undo-list conj undo-checkpoint))))))

(reg-event-db
  :undo
  [ls/store]
  (fn [db]
    (if (can-undo?)
      (let [prev-db (peek @undo-list)]
        (swap! undo-list pop)
        (swap! redo-list conj (content-data db))
        (merge db prev-db))
      db)))

(reg-event-db
  :redo
  [ls/store]
  (fn [db]
    (if (can-redo?)
      (let [next-db (peek @redo-list)]
        (swap! undo-list conj (content-data db))
        (swap! redo-list pop)
        (merge db next-db))
      db)))
