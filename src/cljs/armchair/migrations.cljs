(ns armchair.migrations
  (:require [clojure.set :refer [rename-keys]]
            [com.rpl.specter
             :refer [collect-one
                     multi-path
                     nthpath
                     must
                     NONE
                     FIRST LAST ALL
                     MAP-VALS MAP-KEYS]
             :refer-macros [select setval transform]]
            [armchair.config :as config]
            [armchair.math :as math :refer [Point Rect]]
            [armchair.util :as u]
            [armchair.migrations.textures :refer [texture-lookup]]))

(def db-version 18)

(def migrations
  "Map of migrations. Key is the version we are coming from."
  {1 (fn [db]
       (-> db
           (assoc :player {:location-id (-> db :locations keys first)
                           :location-position [0 0]})
           (u/update-values :dialogues rename-keys {:description :synopsis})))

   ; Extract player options from lines
   2 (fn [db]
       (let [player-lines (u/where-map :kind :player (:lines db))]
         (reduce (fn [new-db [line-id {:keys [options]}]]
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

   ; Remove inline state trigger and information concepts
   3 (fn [db]
       (-> db
           (dissoc :infos)
           (u/update-values :lines dissoc :state-triggers :info-ids)
           (u/update-values :player-options dissoc :state-triggers :required-info-ids)))

   ; Make location connections unidirectional
   4 (fn [db]
       (let [incoming (->> db
                           (select [:locations ALL (collect-one FIRST) LAST :connection-triggers ALL])
                           (reduce (fn [acc [location-id [target-position target-id]]]
                                     (update acc location-id assoc target-id target-position))
                                   {}))]
         (->> (dissoc db :location-connections)
              (transform [:locations ALL (collect-one FIRST) LAST :connection-triggers MAP-VALS]
                         (fn [location-id target-id]
                           [target-id (get-in incoming [target-id location-id])])))))

   ; Store blocked tiles instead of walkable
   5 (fn [db]
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

   ; Migrate to multiple location layers
   6 (fn [db]
       (transform [:locations MAP-VALS]
                  #(-> %
                       (assoc :background2 {}
                              :foreground1 {}
                              :foreground2 {})
                       (rename-keys {:background :background1}))
                  db))
   ; Remove nil as value for line text
   7 (fn [db]
       (setval [:lines MAP-VALS (must :text) nil?] "" db))

   ; Remove next-line-id key instead of storing nil
   8 (fn [db]
       (->> db
            (setval [:lines MAP-VALS #(nil? (:next-line-id %)) :next-line-id] NONE)
            (setval [:player-options MAP-VALS #(nil? (:next-line-id %)) :next-line-id] NONE)))

   ; Migrate to Point and Rect records
   9 (fn [db]
       (letfn [(to-point [[x y]]
                 (Point. x y))
               (to-rect [[[x1 y1] [x2 y2]]]
                 (Rect. x1 y1 (inc (- x2 x1)) (inc (- y2 y1))))]
         (->> db
           (transform [:locations MAP-VALS :dimension] to-rect)
           (transform [:locations MAP-VALS :blocked ALL] to-point)
           (transform [:locations MAP-VALS :connection-triggers MAP-VALS (nthpath 1)]
                      to-point)
           (transform [:locations MAP-VALS
                       (multi-path :background1
                                   :background2
                                   :foreground1
                                   :foreground2
                                   :connection-triggers) MAP-KEYS]
                      to-point)
           (transform [:player :location-position] to-point)
           (transform [:dialogues MAP-VALS (must :location-position)] to-point)
           (transform [:ui/positions MAP-VALS] to-point))))

   ; Remove triggers for dialogue states
   10 (fn [db]
        (let [ds-trigger? (fn [t] (= (:switch-kind t) :dialogue-state))
              trigger-ids (set (select [:triggers MAP-VALS ds-trigger? :entity/id] db))]
          (->> db
               (setval [:triggers MAP-VALS ds-trigger?] NONE)
               (setval [:triggers MAP-VALS :switch-kind] NONE)
               (setval [:lines MAP-VALS (must :trigger-ids) ALL #(contains? trigger-ids %)] NONE))))

   ; Introduce nodes for initial dialogue lines
   11 (fn [db]
        (let [initial-line-positions (u/map-values
                                       #(math/translate-point
                                          (get-in db [:ui/positions (:initial-line-id %)])
                                          (- (+ config/line-width 50)) 0)
                                       (:dialogues db))]
          (update db :ui/positions merge initial-line-positions)))

   ; Move character/dialogue combination into location placements
   12 (fn [db]
        (let [placements (->> db :dialogues vals
                              (remove #(nil? (:location-position %)))
                              (group-by :location-id)
                              (u/map-values
                                (fn [ds]
                                  (->> ds
                                       (group-by :location-position)
                                       (u/map-values
                                         (fn [[d]]
                                           {:character-id (:character-id d)
                                            :dialogue-id (:entity/id d)}))))))]
          (->> db
               (transform [:locations MAP-VALS]
                          (fn [l]
                            (assoc l :placements (get placements (:entity/id l) {}))))
               (transform [:dialogues MAP-VALS]
                          (fn [d]
                            (dissoc d :location-id :location-position))))))

   ; Remove nil dialogue states
   13 (fn [db]
        (setval [:dialogues MAP-VALS :states empty?] NONE db))

   ; Migrate to file based sprite format
   14 (fn [db]
        (->> db
             (transform [:locations MAP-VALS
                         (multi-path :background1
                                     :background2
                                     :foreground1
                                     :foreground2) MAP-VALS]
                        texture-lookup)
             (transform [:characters MAP-VALS :texture] texture-lookup)))

   ; Rename location dimensions to bounds
   15 (fn [db]
        (u/update-values db :locations rename-keys {:dimension :bounds}))

   ; Rename character terture to sprite
   16 (fn [db]
        (u/update-values db :characters rename-keys {:texture :sprite}))

   ; Remove dialogue states
   17 (fn [db]
        (u/update-values db :dialogues dissoc :states))})



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
