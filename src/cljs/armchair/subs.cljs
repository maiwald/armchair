(ns armchair.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [clojure.string :refer [join]]
            [armchair.math :as m :refer [translate-point point-delta global-point]]
            [armchair.util :as u]))

(defn by-id [resources [_ resource-id]]
  (get resources resource-id))

(reg-sub :db-characters #(:characters %))
(reg-sub :db-lines #(:lines %))
(reg-sub :db-locations #(:locations %))
(reg-sub :db/location :<- [:db-locations] by-id)

(reg-sub :db-dialogues #(:dialogues %))
(reg-sub :db/dialogue :<- [:db-dialogues] by-id)

(reg-sub :db-player #(:player %))
(reg-sub :db-player-options #(:player-options %))
(reg-sub :db-triggers #(:triggers %))
(reg-sub :db-switches #(:switches %))
(reg-sub :db-switch-values #(:switch-values %))

(reg-sub :db-dragging #(:dragging %))
(reg-sub :db-connecting #(:connecting %))
(reg-sub :db-cursor #(:cursor %))

(reg-sub :current-mode #(:mode %))
(reg-sub :current-page (fn [{:keys [current-page open-pages]}] (get open-pages current-page)))
(reg-sub :open-pages #(:open-pages %))
(reg-sub :modal #(:modal %))

(reg-sub :ui/positions #(:ui/positions %))
(reg-sub :ui/inspector #(:ui/inspector %))

(reg-sub :ui/location-preview-cache-background #(:ui/location-preview-cache-background %))
(reg-sub :ui/location-preview-cache-foreground #(:ui/location-preview-cache-foreground %))
(reg-sub :ui/location-map-scroll-center #(:ui/location-map-scroll-center %))
(reg-sub :ui/location-map-zoom-scale #(:ui/location-map-zoom-scale %))

(reg-sub :ui/dnd #(:ui/dnd %))
(reg-sub :ui/dnd-preview #(:ui/dnd-preview %))

(reg-sub
  :character-list
  :<- [:db-characters]
  :<- [:db-lines]
  (fn [[characters lines] _]
    (let [line-counts (->> (vals lines)
                           (group-by :character-id)
                           (u/map-values count))]
      (->> (vals characters)
           (map (fn [{id :entity/id :as character}]
                  (assoc character
                         :id id
                         :line-count (get line-counts id 0))))
           (sort-by :display-name)))))

(reg-sub
  :switch-list
  :<- [:db-switches]
  :<- [:db-switch-values]
  (fn [[switches switch-values] _]
    (->> switches
         (map
           (fn [[id {:keys [display-name value-ids default]}]]
             (letfn [(value-name [value-id]
                       (str (get-in switch-values [value-id :display-name])
                            (when (= default value-id) "*")))]
               {:id id
                :display-name display-name
                :values (->> value-ids (map value-name) (join ", "))})))
         (sort-by :display-name))))

(reg-sub
  :location-list
  :<- [:db-locations]
  (fn [locations _]
    (->> locations
         (map (fn [[id {:keys [display-name]}]]
                {:id id
                 :display-name display-name}))
         (sort-by :display-name))))

(reg-sub
  :location/occupied-tiles
  (fn [[_ location-id]]
    [(subscribe [:db/location location-id])
     (subscribe [:db-player])])
  (fn [[{:keys [connection-triggers placements bounds]} player] [_ location-id]]
    (let [player (when (= location-id (:location-id player))
                   {(:location-position player) [:player]})]
      (u/map-keys
        (fn [tile] (global-point tile bounds))
        (merge
          (u/map-values (fn [_ k] [:connection-trigger location-id k]) connection-triggers)
          (u/map-values (fn [_ k] [:placement location-id k]) placements)
          player)))))

(reg-sub
  :dialogue/player-line-option
  :<- [:db-lines]
  :<- [:db-player-options]
  (fn [[lines options] [_ line-id index]]
    (let [option-id (get-in lines [line-id :options index])]
      (get-in options [option-id :text]))))


(reg-sub
  :ui/position
  :<- [:ui/positions]
  :<- [:db-cursor]
  :<- [:db-dragging]
  (fn [[positions cursor {:keys [ids cursor-start scale]}] [_ id]]
    (let [position (get positions id)]
      (if (contains? ids id)
        (let [[dx dy] (point-delta cursor-start cursor)]
          (translate-point position (/ dx scale) (/ dy scale)))
        position))))

(reg-sub
  :ui/connector
  :<- [:db-connecting]
  :<- [:db-cursor]
  (fn [[connecting cursor]]
    (when connecting
      {:start (:cursor-start connecting)
       :end cursor})))

(reg-sub
  :character-options
  :<- [:db-characters]
  (fn [characters _]
    (->> characters
         (u/map-values :display-name)
         (sort-by second))))

(reg-sub
  :character
  :<- [:db-characters]
  (fn [characters [_ character-id]]
    (characters character-id)))

(reg-sub
  :dragging?
  :<- [:db-dragging]
  (fn [dragging _]
    (some? dragging)))

(reg-sub
  :dragging-item?
  :<- [:db-dragging]
  (fn [dragging [_ position-id]]
    (= (:ids dragging) #{position-id})))

(reg-sub
  :dialogue-list
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[dialogues characters]]
    (->> (vals dialogues)
         (map
           (fn [{id :entity/id :keys [synopsis character-id]}]
             (let [character (characters character-id)]
               {:id id
                :synopsis synopsis
                :character (merge {:id character-id} character)
                :sprite (:sprite character)})))
         (sort-by (fn [{s :synopsis {n :display-name} :character}]
                    [n s])))))
