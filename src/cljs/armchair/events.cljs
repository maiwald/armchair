(ns armchair.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx after]]
            [com.rpl.specter
             :refer [must ALL NONE MAP-VALS]
             :refer-macros [setval]]
            [clojure.set :refer [difference]]
            [clojure.spec.alpha :as s]
            cljsjs.filesaverjs
            [armchair.local-storage :as ls]
            [armchair.db :as db :refer [default-db
                                        content-data
                                        serialize-db
                                        deserialize-db]]
            [armchair.migrations :refer [migrate]]
            [armchair.undo :refer [record-undo]]
            [armchair.util :as u]))

(def validate
  (after (fn [db]
           (when-not (s/valid? :armchair.db/state db)
             (let [explain (s/explain-data :armchair.db/state db)]
               (js/console.log (:cljs.spec.alpha/problems explain)))))))

(defn reg-event-data [id handler]
  (reg-event-db id
                [validate record-undo ls/store]
                handler))

(defn reg-event-meta [id handler]
  (reg-event-db id [validate] handler))

;; Initializer

(reg-event-db
  :initialize-db
  (fn [_ _] default-db))

(reg-event-data
  :reset-db
  (fn [db]
    (merge db (content-data default-db))))

(reg-event-meta
  :load-storage-state
  (fn [db]
    (if-let [serialized (ls/get-data)]
      (merge db (migrate (deserialize-db serialized)))
      db)))

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

(reg-event-data
  :upload-state
  (fn [db [_ json]]
    (merge db (migrate (deserialize-db json)))))

;; Character CRUD

(reg-event-data
  :delete-character
  (fn [db [_ id]]
    (let [line-count (->> (:lines db)
                          (u/filter-map #(= (:character-id %) id))
                          count)]
      (cond-> db
        (zero? line-count)
        (update :characters dissoc id)))))

;; Location CRUD

(reg-event-data
  :delete-location
  (fn [db [_ id]]
    (let [location-dialogue-ids (->> (:dialogues db)
                                     (u/where-map :location-id id)
                                     keys)
          connected-location-ids (->> (:locations db)
                                      (u/filter-map #(contains? (->> % :connection-triggers vals (map first) set) id))
                                      keys)]
      (-> db
          (update :locations dissoc id)
          (update :ui/positions dissoc id)
          (u/update-in-map :locations connected-location-ids
                           update :connection-triggers (fn [cts] (u/filter-map #(not= id (first %)) cts)))
          (u/update-in-map :dialogues location-dialogue-ids
                           dissoc :location-id :location-position)))))

(reg-event-data
  :delete-switch
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

(reg-event-data
  :delete-dialogue
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

(reg-event-meta
  :show-page
  (fn [db [_ path]]
    (assoc db :current-page path)))

;; Mouse, Drag & Drop

(reg-event-meta
  :move-cursor
  (fn [db [_ cursor]]
    (assoc db :cursor cursor)))

(reg-event-meta
  :abort-connecting
  (fn [db] (dissoc db :connecting)))

(reg-event-meta
  :start-dragging
  (fn [db [_ ids cursor]]
    (cond-> db
      (not (contains? db :dragging)) (assoc :dragging {:ids ids
                                                       :cursor-start cursor}
                                            :cursor cursor))))

(reg-event-data
  :end-dragging
  (fn [{:keys [dragging cursor] :as db}]
    (assert (some? dragging)
            "Attempting to end drag while not in progress!")
    (let [{:keys [cursor-start ids]} dragging
          delta (u/point-delta cursor-start cursor)]
      (-> db
          (u/update-in-map :ui/positions ids u/translate-point delta)
          (dissoc :dragging :cursor)))))

(reg-event-meta
  :open-popover
  (fn [db [_ reference content]]
    (assoc db :popover {:reference reference
                        :content content})))

(reg-event-meta
  :close-popover
  (fn [db] (dissoc db :popover)))
