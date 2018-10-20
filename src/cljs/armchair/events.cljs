(ns armchair.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx after]]
            [clojure.set :refer [difference]]
            [clojure.spec.alpha :as s]
            cljsjs.filesaverjs
            [armchair.db :refer [default-db content-data serialize-db deserialize-db]]
            [armchair.undo :refer [record-undo]]
            [armchair.routes :refer [routes]]
            [armchair.util :refer [filter-map
                                   filter-keys
                                   map-values
                                   where-map
                                   update-in-map
                                   translate-point
                                   rect-contains?]]))

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
    (merge db (:payload (deserialize-db json)))))

;; Resources

(def default-ui-position [20 20])

;; Character CRUD

(reg-event-db
  :create-character
  [validate
   record-undo]
  (fn [db]
    (let [id (random-uuid)
          new-character {:entity/id id
                         :entity/type :character
                         :color "black"
                         :display-name (str "Character #" id)}]
      (assoc-in db [:characters id] new-character))))

(reg-event-db
  :delete-character
  [validate
   record-undo]
  (fn [db [_ id]]
    (let [line-count (->> (:lines db)
                          (filter-map #(= (:character-id %) id))
                          count)]
      (cond-> db
        (zero? line-count)
        (update :characters dissoc id)))))

(reg-event-db
  :update-character
  [validate
   record-undo]
  (fn [db [_ id field value]]
    (assoc-in db [:characters id field] value)))

(reg-event-db
  :location/create
  [validate
   record-undo]
  (fn [db]
    (let [id (random-uuid)]
      (-> db
          (assoc-in [:ui/positions id] default-ui-position)
          (assoc-in [:locations id] {:entity/id id
                                     :entity/type :location
                                     :dimension [[0 0] [2 2]]
                                     :background {}
                                     :walk-set #{}
                                     :connection-triggers {}
                                     :display-name "New Location"})))))

;; Location CRUD

(reg-event-db
  :delete-location
  [validate
   record-undo]
  (fn [db [_ id]]
    (let [location-connections (filter #(contains? % id)
                                       (:location-connections db))
          location-dialogue-ids (->> (:dialogues db)
                                     (where-map :location-id id)
                                     keys)
          connected-location-ids (->> (:locations db)
                                      (filter-map #(contains? (-> % :connection-triggers vals set) id))
                                      keys)]
      (-> db
          (update :locations dissoc id)
          (update :location-connections difference location-connections)
          (update-in-map :locations connected-location-ids update :connection-triggers (fn [cts] (filter-map #(not= id %) cts)))
          (update-in-map :dialogues location-dialogue-ids dissoc :location-id :location-position)))))

;; Line CRUD

(reg-event-fx
  :create-npc-line
  [validate
   record-undo]
  (fn [{db :db} [_ dialogue-id]]
    (let [id (random-uuid)
          character-id (get-in db [:dialogues dialogue-id :character-id])]
      {:db (-> db
               (assoc-in [:ui/positions id] default-ui-position)
               (assoc-in [:lines id] {:entity/id id
                                      :entity/type :line
                                      :kind :npc
                                      :character-id character-id
                                      :dialogue-id dialogue-id
                                      :text nil
                                      :next-line-id nil}))
       :dispatch [:open-npc-line-modal id]})))

(reg-event-fx
  :create-player-line
  [validate
   record-undo]
  (fn [{db :db} [_ dialogue-id]]
    (let [id (random-uuid)]
      {:db (-> db
               (assoc-in [:ui/positions id] default-ui-position)
               (assoc-in [:lines id] {:entity/id id
                                      :entity/type :line
                                      :kind :player
                                      :dialogue-id dialogue-id
                                      :options []}))
       :dispatch [:open-player-line-modal id]})))

(defn initial-line? [db line-id]
  (let [dialogue-id (get-in db [:lines line-id :dialogue-id])]
    (= line-id (get-in db [:dialogues dialogue-id :initial-line-id]))))

(reg-event-db
  :update-npc-line
  [validate
   record-undo]
  (fn [db [_ field value]]
    (assert (not (and (= field :character-id)
                      (initial-line? db (get-in db [:modal :npc-line :entity/id]))))
            "Cannot modify initial line's character!")
    (assoc-in db [:modal :npc-line field] value)))

(reg-event-db
  :delete-line
  [validate
   record-undo]
  (fn [db [_ id]]
    (assert (not (initial-line? db id))
            "Initial lines cannot be deleted!")
    (letfn [(clear-line [line]
              (update line :next-line-id #(if (= id %) nil %)))
            (clear-options [line]
              (update line :options #(mapv clear-line %)))]
      (update db :lines #(map-values (fn [line]
                                       (case (:kind line)
                                         :npc (clear-line line)
                                         :player (clear-options line)))
                                     (dissoc % id))))))

(reg-event-db
  :move-option
  [validate
   record-undo]
  (fn [db [_ line-id s-index direction]]
    (let [t-index (case direction
                    :up (dec s-index)
                    :down (inc s-index))
          swap-option (fn [options]
                        (replace {(get options s-index) (get options t-index)
                                  (get options t-index) (get options s-index)}
                                 options))]
      (update-in db [:lines line-id :options] swap-option))))

(reg-event-db
  :add-option
  [validate
   record-undo]
  (fn [db [_ line-id]]
    (update-in db [:lines line-id :options] conj {:text ""
                                                  :next-line-id nil})))

(reg-event-db
  :update-option
  [validate
   record-undo]
  (fn [db [_ line-id index text]]
    (assoc-in db [:lines line-id :options index :text] text)))

(reg-event-db
  :delete-option
  [validate
   record-undo]
  (fn [db [_ line-id index]]
    (update-in db [:lines line-id :options] (fn [v] (vec (concat (take index v)
                                                                 (drop (inc index) v)))))))

(reg-event-db
  :set-required-info
  [validate
   record-undo]
  (fn [db [_ line-id index info-ids]]
    (assert (= :player (get-in db [:lines line-id :kind]))
            "Required infos can only be set on player options!")
    (assoc-in db [:lines line-id :options index :required-info-ids] (set info-ids))))

;; Info CRUD

(reg-event-db
  :create-info
  [validate
   record-undo]
  (fn [db]
    (let [id (random-uuid)]
      (update db :infos assoc id {:entity/id id
                                  :entity/type :info
                                  :text ""}))))

(reg-event-db
  :delete-info
  [validate
   record-undo]
  (fn [db [_ id]]
    (update db :infos dissoc id)))

(reg-event-db
  :update-info
  [validate
   record-undo]
  (fn [db [_ id text]]
    (assoc-in db [:infos id :description] text)))

;; Location Editor


;; Modal

(defn assert-no-open-modal [db]
  (assert (not (contains? db :modal))
          "Attempting to open a modal while modal is open!"))

(reg-event-db
  :open-character-modal
  [validate]
  (fn [db [_ payload]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :character-id] payload)))

(reg-event-db
  :open-info-modal
  [validate]
  (fn [db [_ payload]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :info-id] payload)))

(reg-event-db
  :modal/npc-line-edit
  (fn [db [_ line-id]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :npc-line] (get-in db [:lines line-id]))))

(reg-event-db
  :modal/npc-line-create
  [validate]
  (fn [db [_ dialogue-id character-id]]
    (assert-no-open-modal db)
    (assoc-in db [:ui/modal :modal/npc-line] {:entity/id (random-uuid)
                                              :dialogue-id dialogue-id
                                              ::character-id character-id
                                              ::text ""})))

(reg-event-db
  :open-player-line-modal
  [validate]
  (fn [db [_ payload]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :player-line-id] payload)))

(reg-event-db
  :open-dialogue-creation-modal
  [validate]
  (fn [db [_ payload]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :dialogue-creation] {:character-id nil
                                              :description nil})))

;; Dialogue CRUD

(defn assert-dialogue-creation-modal [db]
  (assert (contains? (:modal db) :dialogue-creation)
          "No dialogue creation initiated. Cannot set value!"))

(reg-event-db
  :dialogue-creation-update
  [validate]
  (fn [db [_ field value]]
    (assert-dialogue-creation-modal db)
    (assoc-in db [:modal :dialogue-creation field] value)))

(reg-event-db
  :create-dialogue
  [validate
   record-undo]
  (fn [db]
    (let [dialogue-id (random-uuid)
          line-id (random-uuid)
          modal-data (get-in db [:modal :dialogue-creation])]
      (-> db
          (assoc-in [:dialogues dialogue-id] (merge {:entity/id dialogue-id
                                                     :entity/type :dialogue
                                                     :initial-line-id line-id}
                                                    (select-keys modal-data [:location-id
                                                                             :character-id
                                                                             :description])))
          (assoc-in [:ui/positions line-id] default-ui-position)
          (assoc-in [:lines line-id] {:entity/id line-id
                                      :entity/type :line
                                      :kind :npc
                                      :character-id (:character-id modal-data)
                                      :dialogue-id dialogue-id
                                      :text nil
                                      :next-line-id nil})
          (dissoc :modal)))))

(reg-event-db
  :delete-dialogue
  [validate
   record-undo]
  (fn [db [_ dialogue-id]]
    (-> db
        (update :dialogues dissoc dialogue-id)
        (update :lines #(filter-map (fn [{id :dialogue-id}] (not= id dialogue-id)) %)))))

;; Page

(reg-event-db
  :close-modal
  [validate]
  (fn [db [_ modal-fn | args]]
    (dissoc db :modal)))

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
  :start-connecting-lines
  [validate]
  (fn [db [_ line-id cursor index]]
    (assert (not (contains? db :connecting))
            "Attempting to start connecting lines while already in progress!")
    (assoc db
           :connecting (cond-> {:cursor-start cursor
                                :line-id line-id}
                         (some? index) (assoc :index index))
           :cursor cursor)))

(reg-event-db
  :end-connecting-lines
  [validate
   record-undo]
  (fn [db [_ end-id]]
    (assert (s/valid? :armchair.db/connecting-lines (:connecting db))
            "Attempting to end connecting with missing or invalid state!")
    (let [start-id (get-in db [:connecting :line-id])
          id-path (if-let [index (get-in db [:connecting :index])]
                    [:lines start-id :options index :next-line-id]
                    [:lines start-id :next-line-id])]
      (cond-> (dissoc db :connecting :cursor)
        (not= start-id end-id) (assoc-in id-path end-id)))))

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
          delta (translate-point cursor cursor-start -)]
      (-> db
          (update-in-map :ui/positions ids translate-point delta)
          (dissoc :dragging :cursor)))))
