(ns armchair.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx after]]
            [clojure.set :refer [difference]]
            [clojure.spec.alpha :as s]
            [datascript.core :as d]
            [armchair.db :as db]
            [armchair.routes :refer [routes]]
            [armchair.datascript :refer [conn]]
            [armchair.util :refer [filter-map
                                   filter-keys
                                   map-keys
                                   map-values
                                   translate-point
                                   translate-positions
                                   rect-contains?]]))

(def spec-interceptor (after (fn [db])))
                               ; (when-not (s/valid? :armchair.db/state db)
                               ;   (let [explain (s/explain-data :armchair.db/state db)]
                               ;     (js/console.log (:cljs.spec.alpha/problems explain)))))))

(reg-event-db
  :initialize-db
  [spec-interceptor]
  (fn [_ _]
    db/default-db))

(reg-event-db
  :reset-db
  [spec-interceptor]
  (fn [db]
    (merge db (select-keys db/default-db [:positions
                                          :characters
                                          :dialogues
                                          :locations
                                          :location-connections
                                          :lines]))))

;; Resources

(defn new-id [db resource-type]
  (let [items (get db resource-type)]
    (+ 1 (reduce max 0 (keys items)))))

(defn generate-position [db]
  (let [position-id (new-id db :positions)]
    [(assoc-in db [:positions position-id] [20 20]) position-id]))

;; Character CRUD

(reg-event-db
  :create-character
  [spec-interceptor]
  (fn [db]
    (let [id (new-id db :characters)
          new-character {:id id :color "black" :display-name (str "Character #" id)}]
      (update db :characters assoc id new-character))))

(reg-event-db
  :characters/delete
  [spec-interceptor]
  (fn [db [_ id]]
    (d/transact! conn [[:db/retractEntity id]])
    (assoc db :store @conn)))

(reg-event-db
  :update-character
  [spec-interceptor]
  (fn [db [_ id field value]]
    (assoc-in db [:characters id field] value)))

(reg-event-db
  :location/create
  [spec-interceptor]
  (fn [db]
    (d/transact! conn [{:db/id -1
                        :location/name "New Location"
                        :ui/position [20 20]
                        :location/dimension [[0 0] [2 2]]
                        :location/background #{}
                        :location/walk-set #{}
                        :location/connection-triggers #{}}])
    (assoc db :store @conn)))

;; Location CRUD

(reg-event-db
  :delete-location
  [spec-interceptor]
  (fn [db [_ id]]
    (let [location-connections (filter #(contains? % id)
                                       (:location-connections db))]
      (cond-> db
        (empty? location-connections)
        (-> (update :locations dissoc id)
            (update :location-connections difference location-connections))))))

(reg-event-db
  :location-editor/update-name
  [spec-interceptor]
  (fn [db [_ location-id value]]
    (d/transact! conn [[:db/add location-id :location/name value]])
    (assoc db :store @conn)))

;; Line CRUD

(reg-event-db
  :create-npc-line
  [spec-interceptor]
  (fn [db [_ dialogue-id]]
    (let [[new-db position-id] (generate-position db)
          id (new-id db :lines)]
      (assoc-in new-db [:lines id] {:id id
                                    :kind :npc
                                    :character-id nil
                                    :dialogue-id dialogue-id
                                    :position-id position-id
                                    :text nil
                                    :next-line-id nil}))))

(reg-event-db
  :create-player-line
  [spec-interceptor]
  (fn [db [_ dialogue-id]]
    (let [[new-db position-id] (generate-position db)
          id (new-id db :lines)]
      (assoc-in new-db [:lines id] {:id id
                                    :kind :player
                                    :dialogue-id dialogue-id
                                    :position-id position-id
                                    :options []}))))

(defn initial-line? [db line-id]
  (let [dialogue-id (get-in db [:lines line-id :dialogue-id])]
    (= line-id (get-in db [:dialogues dialogue-id :initial-line-id]))))

(reg-event-db
  :update-line
  [spec-interceptor]
  (fn [db [_ id field value]]
    (assert (not (and (= field :character-id)
                      (initial-line? db id)))
            "Cannot modify initial line's character!")
    (assoc-in db [:lines id field] value)))

(reg-event-db
  :delete-line
  [spec-interceptor]
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
  [spec-interceptor]
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
  [spec-interceptor]
  (fn [db [_ line-id]]
    (update-in db [:lines line-id :options] conj {:text ""
                                                  :next-line-id nil})))

(reg-event-db
  :update-option
  [spec-interceptor]
  (fn [db [_ line-id index text]]
    (assoc-in db [:lines line-id :options index :text] text)))

(reg-event-db
  :delete-option
  [spec-interceptor]
  (fn [db [_ line-id index]]
    (update-in db [:lines line-id :options] (fn [v] (vec (concat (take index v)
                                                                 (drop (inc index) v)))))))

(reg-event-db
  :set-infos
  [spec-interceptor]
  (fn [db [_ line-id info-ids]]
    (assert (= :npc (get-in db [:lines line-id :kind]))
            "Infos can only be set on NPC lines!")
    (assoc-in db [:lines line-id :info-ids] (set info-ids))))

(reg-event-db
  :set-required-info
  [spec-interceptor]
  (fn [db [_ line-id index info-ids]]
    (assert (= :player (get-in db [:lines line-id :kind]))
            "Required infos can only be set on player options!")
    (assoc-in db [:lines line-id :options index :required-info-ids] (set info-ids))))

;; Info CRUD

(reg-event-db
  :create-info
  [spec-interceptor]
  (fn [db]
    (let [id (new-id db :infos)]
      (update db :infos assoc id {:id id :text ""}))))

(reg-event-db
  :delete-info
  [spec-interceptor]
  (fn [db [_ id]]
    (update db :infos dissoc id)))

(reg-event-db
  :update-info
  [spec-interceptor]
  (fn [db [_ id text]]
    (assoc-in db [:infos id :description] text)))

;; Location Editor

(reg-event-db
  :set-tool
  [spec-interceptor]
  (fn [db [_ tool]]
    (assoc-in db [:location-editor :tool] tool)))

(reg-event-db
  :set-active-texture
  [spec-interceptor]
  (fn [db [_ texture]]
    (assoc-in db [:location-editor :active-texture] texture)))

(reg-event-db
  :set-highlight
  [spec-interceptor]
  (fn [db [_ tile]]
    (assoc-in db [:location-editor :highlight] tile)))

(reg-event-db
  :unset-highlight
  [spec-interceptor]
  (fn [db]
    (update db :location-editor dissoc :highlight)))

(reg-event-db
  :start-entity-drag
  [spec-interceptor]
  (fn [db [_ payload]]
    (assoc db :dnd-payload payload)))

(reg-event-db
  :stop-entity-drag
  [spec-interceptor]
  (fn [db]
    (dissoc db :dnd-payload)))

(reg-event-db
  :move-entity
  [spec-interceptor]
  (fn [db [_ location-id dialogue-id position]]
    (d/transact! conn [[:db/add dialogue-id :dialogue/position position]
                       [:db/add location-id :location/dialogues dialogue-id]])
    (-> db
        (dissoc :dnd-payload)
        (update :location-editor dissoc :highlight)
        (assoc :store @conn))))

(reg-event-db
  :remove-entity
  [spec-interceptor]
  (fn [{store :store :as db} [_ location-id dialogue-id position]]
    (let [pos (d/q '[:find ?p .
                     :in $ ?d
                     :where [?d :dialogue/position ?p]]
                   store
                   dialogue-id)]
      (d/transact! conn [[:db/retract dialogue-id :dialogue/position pos]
                         [:db/retract location-id :location/dialogues dialogue-id]]))
    (-> db
        (dissoc :dnd-payload)
        (update :location-editor dissoc :highlight)
        (assoc :store @conn))))

(reg-event-db
  :move-trigger
  [spec-interceptor]
  (fn [db [_ location target to]]
    (-> db
        (dissoc :dnd-payload)
        (update :location-editor dissoc :highlight)
        (update-in [:locations location :connection-triggers] #(as-> % new-db
                                                                 (filter-map (fn [v] (not= v target)) new-db)
                                                                 (assoc new-db to target))))))

(reg-event-db
  :start-painting
  [spec-interceptor]
  (fn [db [_ location-id tile]]
    (let [texture (get-in db [:location-editor :active-texture])]
      (-> db
          (assoc-in [:location-editor :painting?] true)
          (assoc-in [:locations location-id :background tile] texture)))))

(reg-event-db
  :paint
  [spec-interceptor]
  (fn [db [_ location-id tile]]
    (let [{:keys [painting? active-texture]} (:location-editor db)]
      (cond-> db
        painting? (assoc-in [:locations location-id :background tile] active-texture)))))

(reg-event-db
  :stop-painting
  [spec-interceptor]
  (fn [db]
    (assoc-in db [:location-editor :painting?] false)))

(reg-event-db
  :set-walkable
  [spec-interceptor]
  (fn [{store :store :as db} [_ location-id tile value]]
    (let [old-value (-> (d/pull store [:location/walk-set] location-id)
                        :location/walk-set)]
      (d/transact! conn [[:db/add location-id :location/walk-set (if value
                                                                   (conj old-value tile)
                                                                   (disj old-value tile))]])
      (assoc db :store @conn))))

(reg-event-db
  :resize-smaller
  [spec-interceptor]
  (fn [{store :store :as db} [_ location-id direction]]
    (let [[shift-index shift-delta] (case direction
                                      :up [0 [0 1]]
                                      :left [0 [1 0]]
                                      :right [1 [-1 0]]
                                      :down [1 [0 -1]])
          old-value (-> (d/pull store [:location/dimension] location-id)
                        :location/dimension)]
      (d/transact! conn [[:db/add location-id :location/dimension
                          (update old-value shift-index translate-point shift-delta)]])
      (assoc db :store @conn))))

(reg-event-db
  :resize-larger
  [spec-interceptor]
  (fn [{store :store :as db} [_ location-id direction]]
    (let [[shift-index shift-delta] (case direction
                                      :up [0 [0 -1]]
                                      :left [0 [-1 0]]
                                      :right [1 [1 0]]
                                      :down [1 [0 1]])
          old-value (-> (d/pull store [:location/dimension] location-id)
                        :location/dimension)]
      (d/transact! conn [[:db/add location-id :location/dimension
                          (update old-value shift-index translate-point shift-delta)]])
      (assoc db :store @conn))))

;; Modal

(defn assert-no-open-modal [db]
  (assert (not (contains? db :modal))
          "Attempting to open a modal while modal is open!"))

(reg-event-db
  :open-character-modal
  [spec-interceptor]
  (fn [db [_ payload]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :character-id] payload)))

(reg-event-db
  :open-info-modal
  [spec-interceptor]
  (fn [db [_ payload]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :info-id] payload)))

(reg-event-db
  :open-npc-line-modal
  [spec-interceptor]
  (fn [db [_ payload]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :npc-line-id] payload)))

(reg-event-db
  :open-player-line-modal
  [spec-interceptor]
  (fn [db [_ payload]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :player-line-id] payload)))

(reg-event-db
  :open-dialogue-creation-modal
  [spec-interceptor]
  (fn [db [_ payload]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :dialogue-creation] {:character-id nil
                                              :location-id nil
                                              :description nil})))

;; Dialogue CRUD

(defn assert-dialogue-creation-modal [db]
  (assert (contains? (:modal db) :dialogue-creation)
          "No dialogue creation initiated. Cannot set value!"))

(reg-event-db
  :dialogue-creation-update
  [spec-interceptor]
  (fn [db [_ field value]]
    (assert-dialogue-creation-modal db)
    (assoc-in db [:modal :dialogue-creation field] value)))

(reg-event-db
  :create-dialogue
  [spec-interceptor]
  (fn [db]
    (let [[new-db position-id] (generate-position db)
          dialogue-id (new-id db :dialogues)
          line-id (new-id db :lines)
          modal-data (get-in db [:modal :dialogue-creation])]
      (-> new-db
          (assoc-in [:dialogues dialogue-id] (merge {:id dialogue-id
                                                     :initial-line-id line-id}
                                                    (select-keys modal-data [:location-id
                                                                             :character-id
                                                                             :description])))
          (assoc-in [:lines line-id] {:id line-id
                                      :kind :npc
                                      :character-id (:character-id modal-data)
                                      :dialogue-id dialogue-id
                                      :position-id position-id
                                      :text nil
                                      :next-line-id nil})
          (dissoc :modal)))))

(reg-event-db
  :delete-dialogue
  [spec-interceptor]
  (fn [db [_ dialogue-id]]
    (-> db
        (update :dialogues dissoc dialogue-id)
        (update :lines #(filter-map (fn [{id :dialogue-id}] (not= id dialogue-id)) %))
        ; (update :locations (fn [locations]
        ;                      (map-values (fn [{npcs :npcs}]
        ;                                    (filter-map #(not= % dialogue-id) npcs))
        ;                                  locations)))
        )))

;; Page

(reg-event-db
  :close-modal
  [spec-interceptor]
  (fn [db [_ modal-fn | args]]
    (dissoc db :modal)))

(reg-event-db
  :show-page
  [spec-interceptor]
  (fn [db [_ path]]
    (assoc db :current-page path)))

;; Mouse, Drag & Drop

(reg-event-db
  :move-cursor
  [spec-interceptor]
  (fn [db [_ cursor]]
    (assoc db :cursor cursor)))

(reg-event-db
  :start-connecting-lines
  [spec-interceptor]
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
  [spec-interceptor]
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
  [spec-interceptor]
  (fn [db [_ location-id cursor]]
    (assert (not (contains? db :connecting))
            "Attempting to start connecting locations while already in progress!")
    (assoc db
           :connecting {:cursor-start cursor
                        :location-id location-id}
           :cursor cursor)))

(reg-event-db
  :end-connecting-locations
  [spec-interceptor]
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
  [spec-interceptor]
  (fn [db] (dissoc db :connecting)))

(reg-event-db
  :start-dragging
  [spec-interceptor]
  (fn [db [_ ids cursor]]
    (cond-> db
      (not (contains? db :dragging)) (assoc :dragging {:ids ids
                                                       :cursor-start cursor}
                                            :cursor cursor))))

(reg-event-db
  :end-dragging
  [spec-interceptor]
  (fn [{:keys [store dragging cursor] :as db}]
    (assert (some? dragging)
            "Attempting to end drag while not in progress!")
    (let [{:keys [cursor-start ids]} dragging
          delta (translate-point cursor cursor-start -)
          old-positions (d/q '[:find ?id ?position
                               :in $ [?id ...]
                               :where
                               [?id :ui/position ?position]]
                             store
                             ids)]
      (d/transact! conn
                   (map (fn [[id position]]
                          [:db/add id :ui/position (translate-point position delta)])
                        old-positions))
      (-> db
          (assoc :store @conn)
          (dissoc :dragging :cursor)))))
