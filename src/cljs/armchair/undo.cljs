(ns armchair.undo
  (:require [re-frame.core :refer [->interceptor reg-event-db reg-sub]]
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
  (->interceptor
    :id ::record-undo
    :after (fn [context]
             (let [new-state (content-data (get-in context [:effects :db]))
                   undo-checkpoint (content-data (get-in context [:coeffects :db]))]
               (when-not (= new-state undo-checkpoint)
                 (reset! redo-list [])
                 (swap! undo-list conj undo-checkpoint)))
             context)))

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
