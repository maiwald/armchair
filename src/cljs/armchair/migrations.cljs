(ns armchair.migrations
  (:require [clojure.set :refer [rename-keys]]
            [com.rpl.specter
             :refer [collect-one must FIRST LAST ALL MAP-VALS]
             :refer-macros [select setval transform]]
            [armchair.db :refer [db-version]]
            [armchair.util :as u]))

(def migrations
  "Map of migrations. Key is the version we are coming from."
  {1 (fn [db]
       (-> db
           (assoc :player {:location-id (-> db :locations keys first)
                           :location-position [0 0]})
           (u/update-values :dialogues
                            (fn [d]
                              (-> d
                                  (assoc :synopsis (:description d))
                                  (dissoc :description))))))

   2 (fn [db]
       "Extract player options from lines"
       (let [player-lines (u/where-map :kind :player (:lines db))]
         (reduce (fn [new-db [line-id {:keys [options] :as line}]]
                   (let [new-options (mapv #(assoc %
                                                   :entity/id (random-uuid)
                                                   :entity/type :player-option)
                                           options)
                         option-ids (mapv :entity/id new-options)]
                     (-> new-db
                         (update :player-options merge (zipmap option-ids new-options))
                         (assoc-in [:lines line-id :options] option-ids))))
                 db
                 player-lines)))

   3 (fn [db]
       "Remove inline state trigger and information concepts"
       (-> db
           (dissoc :infos)
           (u/update-values :lines
                            #(dissoc % :state-triggers :info-ids))
           (u/update-values :player-options
                            #(dissoc % :state-triggers :required-info-ids))))

   4 (fn [db]
       "Make location connections unidirectional"
       (let [incoming (->> db
                           (select [:locations ALL (collect-one FIRST) LAST :connection-triggers ALL])
                           (reduce (fn [acc [location-id [target-position target-id]]]
                                     (update acc location-id assoc target-id target-position))
                                   {}))]
         (->> (dissoc db :location-connections)
              (transform [:locations ALL (collect-one FIRST) LAST :connection-triggers MAP-VALS]
                         (fn [location-id target-id]
                           [target-id (get-in incoming [target-id location-id])])))))

   5 (fn [db]
       "Store blocked tiles instead of walkable"
       (letfn [(inverse [[[x1 y1] [x2 y2]] tiles]
                 (set (for [x (range x1 (inc x2))
                            y (range y1 (inc y2))
                            :let [tile [x y]]
                            :when (not (contains? tiles tile))]
                        tile)))]
         (transform [:locations MAP-VALS]
           (fn [{:keys [dimension walk-set] :as location}]
             (-> location
                 (dissoc :walk-set)
                 (assoc :blocked (inverse dimension walk-set))))
           db)))

   6 (fn [db]
       "Rename :background to :background1"
       (transform [:locations MAP-VALS]
                  #(-> %
                       (assoc :background2 {}
                              :foreground1 {}
                              :foreground2 {})
                       (rename-keys {:background :background1}))
                  db))
   7 (fn [db]
       "Remove nil as value for line text"
       (setval [:lines MAP-VALS (must :text) nil?] "" db))})

(defn migrate [{:keys [version payload]}]
  (assert (<= version db-version)
          (str "Save file version is invalid: " version ", current: " db-version))
  (loop [version version
         payload payload]
    (if (= version db-version)
      payload
      (recur
        (inc version)
        ((get migrations version) payload)))))
