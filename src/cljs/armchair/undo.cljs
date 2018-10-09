(ns armchair.undo
  (:require [re-frame.core :refer [after reg-event-db]]
            [re-frame.db :refer [app-db]]
            [armchair.db :refer [content-data]]))

(def undo-list (atom []))
(def redo-list (atom []))

(defn reset-undos! []
  (reset! undo-list [])
  (reset! redo-list []))

(def record-undo
  (after (fn [db]
           (let [new-state (content-data db)
                 undo-checkpoint (content-data @app-db)]
             (when-not (= new-state undo-checkpoint)
               (reset! redo-list [])
               (swap! undo-list conj undo-checkpoint))))))

(reg-event-db
  :undo
  (fn [db]
    (if (not-empty @undo-list)
      (let [prev-db (peek @undo-list)]
        (swap! undo-list pop)
        (swap! redo-list conj (content-data db))
        (merge db prev-db))
      db)))

(reg-event-db
  :redo
  (fn [db]
    (if (not-empty @redo-list)
      (let [next-db (peek @redo-list)]
        (swap! undo-list conj (content-data db))
        (swap! redo-list pop)
        (merge db next-db))
      db)))
