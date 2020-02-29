(ns armchair.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx after]]
            [com.rpl.specter
             :refer [must ALL NONE MAP-VALS]
             :refer-macros [select setval]]
            [clojure.spec.alpha :as s]
            cljsjs.filesaverjs
            [armchair.config :refer [debug?]]
            [armchair.local-storage :as ls]
            [armchair.db :as db :refer [default-db
                                        content-data
                                        serialize-db
                                        deserialize-db]]
            [armchair.migrations :refer [migrate]]
            [armchair.undo :refer [record-undo]]
            [armchair.math :as m]
            [armchair.util :as u]
            [goog.functions :refer [debounce]]))

(when debug?
  (def validate
    (after (debounce
             (fn [db event]
               (when-not (s/valid? :armchair.db/state db)
                 (let [explain (s/explain-data :armchair.db/state db)]
                   (apply js/console.log
                          "Invalid state after:" event "\n"
                          (:cljs.spec.alpha/problems explain)))))
             200))))

(defn reg-event-data
  ([id handler]
   (reg-event-db id
                 [(when debug? validate)
                  record-undo
                  ls/store]
                 handler))
  ([id interceptors handler]
   (reg-event-db id
                 [(when debug? validate)
                  record-undo
                  interceptors
                  ls/store]
                 handler)))

(defn reg-event-meta [id handler]
  (reg-event-db id [(when debug? validate)] handler))

;; Initializer

(reg-event-db
  :initialize-db
  (fn [_ _] default-db))

(reg-event-data
  :reset-db
  (fn [db]
    (let [new-db (merge db (content-data default-db))]
      (dispatch [:armchair.location-previews/regenerate-all])
      new-db)))

(reg-event-meta
  :load-storage-state
  (fn [db]
    (let [new-db (if-let [serialized (ls/get-data)]
                   (merge db (migrate (deserialize-db serialized)))
                   db)]
      (dispatch [:armchair.location-previews/regenerate-all])
      new-db)))

(reg-event-data
  :upload-state
  (fn [db [_ json]]
    (let [new-db (merge db (migrate (deserialize-db json)))]
      (dispatch [:armchair.location-previews/regenerate-all])
      new-db)))

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

;; Character CRUD

(reg-event-data
  :delete-character
  (fn [db [_ character-id]]
    (let [line-count (->> (:lines db)
                          (u/filter-map #(= (:character-id %) character-id))
                          count)]
      (if (zero? line-count)
        (-> (setval [:locations MAP-VALS
                     :placements ALL
                     (fn [[_ {c-id :character-id}]] (= c-id character-id))]
                    NONE db)
            (update :characters dissoc character-id))
        db))))

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
          case-node-ids (select [:lines MAP-VALS belongs-to-switch? :entity/id] db)
          trigger-ids (->> (:triggers db)
                           (u/filter-map belongs-to-switch?)
                           keys
                           set)]
      (->>
        (-> (reduce db/delete-node-with-references db case-node-ids)
            (update :switches dissoc switch-id)
            (update :switch-values #(apply dissoc % switch-value-ids))
            (update :triggers #(apply dissoc % trigger-ids)))
        (setval [:player-options MAP-VALS (must :condition) :terms ALL belongs-to-switch?] NONE)
        (setval [:player-options MAP-VALS (must :condition) #(empty? (:terms %))] NONE)
        (setval [:lines MAP-VALS (must :trigger-ids) ALL #(contains? trigger-ids %)] NONE)))))

;; Dialogue CRUD

(reg-event-data
  :delete-dialogue
  (fn [db [_ dialogue-id]]
    (let [in-dialogue? (fn [node] (= dialogue-id (:dialogue-id node)))
          line-ids (select [:lines MAP-VALS in-dialogue? :entity/id] db)]

      (-> (loop [new-db (setval [:locations MAP-VALS
                                 :placements MAP-VALS
                                 :dialogue-id #(= dialogue-id %)]
                                NONE db)
                 line-ids line-ids]
            (if (empty? line-ids)
              new-db
              (recur (db/delete-node-with-references new-db (first line-ids))
                     (rest line-ids))))
          (update :dialogues dissoc dialogue-id)))))

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
          [dx dy] (m/point-delta cursor-start cursor)]
      (-> db
          (u/update-in-map :ui/positions ids m/translate-point dx dy)
          (dissoc :dragging :cursor)))))

(reg-event-meta
  :cancel-dragging
  (fn [db] (dissoc db :dragging :cursor)))

(reg-event-meta
  :inspect
  (fn [db [_ inspector-type & inspector-data]]
    (assoc db :ui/inspector
           [inspector-type {:location-id (first inspector-data)
                            :location-position (second inspector-data)}])))

(reg-event-meta
  :close-inspector
  (fn [db] (dissoc db :ui/inspector)))
