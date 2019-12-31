(ns armchair.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub subscribe]]
            [clojure.string :refer [join]]
            [com.rpl.specter
             :refer [collect-one ALL FIRST LAST MAP-VALS]
             :refer-macros [select]]
            [armchair.routes :refer [page-data]]
            [armchair.math :as m :refer [translate-point point-delta]]
            [armchair.util :as u]))

(reg-sub :db-characters #(:characters %))
(reg-sub :db-lines #(:lines %))
(reg-sub :db-locations #(:locations %))
(reg-sub :db-dialogues #(:dialogues %))
(reg-sub :db-player #(:player %))
(reg-sub :db-player-options #(:player-options %))
(reg-sub :db-triggers #(:triggers %))
(reg-sub :db-switches #(:switches %))
(reg-sub :db-switch-values #(:switch-values %))

(reg-sub :db-dragging #(:dragging %))
(reg-sub :db-connecting #(:connecting %))
(reg-sub :db-cursor #(:cursor %))

(reg-sub :current-page #(:current-page %))
(reg-sub :modal #(:modal %))

(reg-sub :ui/positions #(:ui/positions %))
(reg-sub :ui/inspector #(:ui/inspector %))

(reg-sub :ui/location-preview-cache (fn [db] (:ui/location-preview-cache db)))

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
                            (if (= default value-id) "*")))]
               {:id id
                :display-name display-name
                :values (->> value-ids (map value-name) (join ", "))})))
         (sort-by :display-name))))


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
  :<- [:db-dragging]
  :<- [:db-cursor]
  (fn [[positions {:keys [ids cursor-start]} cursor] [_ id]]
    (let [position (get positions id)]
      (if (contains? ids id)
        (let [[dx dy] (point-delta cursor-start cursor)]
          (translate-point position dx dy))
        position))))

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
  :connector
  :<- [:db-connecting]
  :<- [:db-cursor]
  (fn [[connecting cursor]]
    (when connecting
      {:start (:cursor-start connecting)
       :end cursor
       :kind :connector})))

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
                :texture (:texture character)})))
         (sort-by (fn [{s :synopsis {n :display-name} :character}]
                    [n s])))))

(reg-sub
  :location-options
  :<- [:db-locations]
  (fn [locations _]
    (->> locations
         (u/map-values :display-name)
         (sort-by second))))

(reg-sub
  :location-map
  :<- [:db-locations]
  :<- [:ui/positions]
  (fn [[locations positions] _]
    (let [connections (->> locations
                           (select [ALL (collect-one FIRST) LAST :connection-triggers MAP-VALS FIRST])
                           (map set)
                           distinct
                           (map sort))
          positions (vals (select-keys positions (keys locations)))]
      {:dimensions (m/rect-resize
                     (m/containing-rect positions)
                     {:left 200
                      :right 200
                      :top 200
                      :bottom 200})
       :location-ids (keys locations)
       :connections connections})))

(reg-sub
  :location-map/location
  :<- [:db-locations]
  :<- [:ui/location-preview-cache]
  (fn [[locations preview-cache] [_ location-id]]
    (let [location (get locations location-id)
          preview-image-src (get preview-cache location-id)]
      {:display-name (:display-name location)
       :dimension (:dimension location)
       :preview-image-src preview-image-src})))

(reg-sub
  :creation-buttons
  :<- [:current-page]
  (fn [current-page]
    (let [{:keys [page-name page-params]} (page-data current-page)]
      (case page-name
        :locations [{:title "New Location"
                     :icon "plus"
                     :event [:open-location-creation]}]
        :dialogues [{:title "New Dialogue"
                     :icon "plus"
                     :event [:armchair.modals.dialogue-creation/open]}]
        :characters [{:title "New Character"
                      :icon "plus"
                      :event [:armchair.modals.character-form/open]}]
        :switches [{:title "New Switch"
                    :icon "plus"
                    :event [:armchair.modals.switch-form/open]}]
        :dialogue-edit (let [id (uuid (:id page-params))]
                         [{:title "New Character Line"
                           :icon "plus"
                           :event [:create-npc-line id]}
                          {:title "New Player Line"
                           :icon "plus"
                           :event [:create-player-line id]}
                          {:title "New Trigger Node"
                           :icon "plus"
                           :event [:create-trigger-node id]}
                          {:title "New Switch Node"
                           :icon "plus"
                           :event [:armchair.modals.case-node-creation/open id]}])
        nil))))

(reg-sub
  :ui/inspector?
  :<- [:ui/inspector]
  some?)
