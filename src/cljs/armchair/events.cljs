(ns armchair.events
  (:require [re-frame.core :as re-frame]
            [armchair.db :as db]))

(re-frame/reg-event-db
  :initialize-db
  (fn  [_ _]
    db/default-db))

(re-frame/reg-event-db
  :start-drag
  (fn [db [_ drag]]
    (assoc db :drag drag)))

(re-frame/reg-event-db
  :end-drag
  (fn  [db _]
    (dissoc db :drag)))

(re-frame/reg-event-db
  :drag
  (fn [db [_ drag]]
    (let [{:keys [mouse-x mouse-y]} drag]
      (if-let [id (get-in db [:drag :id])]
        (let [delta-x (- (get-in db [:drag :mouse-x]) mouse-x)
              delta-y (- (get-in db [:drag :mouse-y]) mouse-y)]
          (-> db
              (assoc-in [:lines id :x] (- mouse-x 20));- delta-x)
              (assoc-in [:lines id :y] (- mouse-y 20));- delta-y)
              (update :drag assoc { :mouse-x mouse-x :mouse-y mouse-y })))
        db))))
