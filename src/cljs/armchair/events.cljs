(ns armchair.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx after]]
            [com.rpl.specter
             :refer [must ALL NONE MAP-VALS]
             :refer-macros [setval]]
            [clojure.set :refer [difference]]
            [clojure.spec.alpha :as s]
            cljsjs.filesaverjs
            [armchair.db :as db :refer [default-db
                                        content-data
                                        serialize-db
                                        deserialize-db
                                        migrate]]
            [armchair.undo :refer [record-undo]]
            [armchair.util :as u]))

(def validate
  (after (fn [db]
           (when-not (s/valid? :armchair.db/state db)
             (let [explain (s/explain-data :armchair.db/state db)]
               (js/console.log (:cljs.spec.alpha/problems explain)))))))

;; Initializer

(reg-event-db
  :initialize-db
  [validate]
  (fn [_ _] default-db))

(reg-event-db
  :reset-db
  [validate
   record-undo]
  (fn [db]
    (merge db (content-data default-db))))

(reg-event-fx
  :download-state
  (fn [{db :db}]
    (let [mime-type (str "application/json;charset="
                         (.-characterSet js/document))
          blob (new js/Blob
                    (clj->js [(serialize-db db)])
                    (clj->js {:type mime-type}))
          filename (str "armchair-save-"
                        (.toISOString (new js/Date))
                        ".json")]
      (js/saveAs blob filename))
    {}))

(reg-event-db
  :upload-state
  [validate
   record-undo]
  (fn [db [_ json]]
    (merge db (migrate (deserialize-db json)))))

;; Character CRUD

(reg-event-db
  :delete-character
  [validate
   record-undo]
  (fn [db [_ id]]
    (let [line-count (->> (:lines db)
                          (u/filter-map #(= (:character-id %) id))
                          count)]
      (cond-> db
        (zero? line-count)
        (update :characters dissoc id)))))

;; Location CRUD

(reg-event-db
  :delete-location
  [validate
   record-undo]
  (fn [db [_ id]]
    (let [location-connections (filter #(contains? % id)
                                       (:location-connections db))
          location-dialogue-ids (->> (:dialogues db)
                                     (u/where-map :location-id id)
                                     keys)
          connected-location-ids (->> (:locations db)
                                      (u/filter-map #(contains? (-> % :connection-triggers vals set) id))
                                      keys)]
      (-> db
          (update :locations dissoc id)
          (update :location-connections difference location-connections)
          (u/update-in-map :locations connected-location-ids
                           update :connection-triggers (fn [cts] (u/filter-map #(not= id %) cts)))
          (u/update-in-map :dialogues location-dialogue-ids
                           dissoc :location-id :location-position)))))

(reg-event-db
  :delete-switch
  [validate
   record-undo]
  (fn [db [_ switch-id]]
    (let [switch-value-ids (get-in db [:switches switch-id :value-ids])
          belongs-to-switch? (fn [i] (= (:switch-id i) switch-id))
          trigger-ids (->> (:triggers db)
                           (u/filter-map belongs-to-switch?)
                           keys
                           set)]
      (->>
        (-> db
            (update :switches dissoc switch-id)
            (update :switch-values #(apply dissoc % switch-value-ids))
            (update :triggers #(apply dissoc % trigger-ids)))
        (setval [:player-options MAP-VALS (must :condition) :terms ALL belongs-to-switch?] NONE)
        (setval [:player-options MAP-VALS (must :condition) #(empty? (:terms %))] NONE)
        (setval [:lines MAP-VALS (must :trigger-ids) ALL #(contains? trigger-ids %)] NONE)
        u/log))))

;; Dialogue CRUD

(reg-event-db
  :delete-dialogue
  [validate
   record-undo]
  (fn [db [_ dialogue-id]]
    (let [dialogue-states (get-in db [:dialogues dialogue-id :states])]
      (-> (loop [db db
                 [state-id & state-ids] (keys dialogue-states)]
            (if (nil? state-id)
              db
              (recur (db/clear-dialogue-state db state-id)
                     state-ids)))
          (update :dialogues dissoc dialogue-id)
          (update :lines #(u/filter-map (fn [{id :dialogue-id}] (not= id dialogue-id)) %))))))

;; Page

(reg-event-db
  :show-page
  [validate]
  (fn [db [_ path]]
    (assoc db :current-page path)))

;; Mouse, Drag & Drop

(reg-event-db
  :move-cursor
  [validate]
  (fn [db [_ cursor]]
    (assoc db :cursor cursor)))

(reg-event-db
  :start-connecting-locations
  [validate]
  (fn [db [_ location-id cursor]]
    (assert (not (contains? db :connecting))
            "Attempting to start connecting locations while already in progress!")
    (assoc db
           :connecting {:cursor-start cursor
                        :location-id location-id}
           :cursor cursor)))

(reg-event-db
  :end-connecting-locations
  [validate
   record-undo]
  (fn [db [_ end-id]]
    (assert (some? (:connecting db))
            "Attempting to end connecting while not in progress!")
    (let [start-id (get-in db [:connecting :location-id])
          new-db (dissoc db :connecting :cursor)]
      (if-not (= start-id end-id)
        (update new-db :location-connections conj #{start-id end-id})
        new-db))))


(reg-event-db
  :abort-connecting
  [validate]
  (fn [db] (dissoc db :connecting)))

(reg-event-db
  :start-dragging
  [validate]
  (fn [db [_ ids cursor]]
    (cond-> db
      (not (contains? db :dragging)) (assoc :dragging {:ids ids
                                                       :cursor-start cursor}
                                            :cursor cursor))))

(reg-event-db
  :end-dragging
  [validate
   record-undo]
  (fn [{:keys [dragging cursor] :as db}]
    (assert (some? dragging)
            "Attempting to end drag while not in progress!")
    (let [{:keys [cursor-start ids]} dragging
          delta (u/point-delta cursor-start cursor)]
      (-> db
          (u/update-in-map :ui/positions ids u/translate-point delta)
          (dissoc :dragging :cursor)))))
