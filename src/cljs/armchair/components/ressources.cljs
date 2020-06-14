(ns armchair.components.ressources
  (:require [armchair.config :as config]
            [armchair.components :as c]
            [armchair.components.sidebar :refer [sidebar]]
            [armchair.routes :refer [>navigate]]
            [armchair.util :as u :refer [<sub >evt]]))

(def icon-size 20)

(defn character [{:keys [id display-name texture]}]
  [:li.ressource {:draggable true
                  :on-click #(>evt [:armchair.modals.character-form/open id])
                  :on-drag-end #(>evt [:stop-entity-drag])
                  :on-drag-start (fn [e]
                                   (let [offset (/ config/tile-size 2)
                                         ghost (-> e
                                                   (.-currentTarget)
                                                   (.getElementsByClassName "drag-ghost")
                                                   (aget 0))]
                                     (.setDragImage (.-dataTransfer e) ghost offset offset))
                                   (>evt [:start-entity-drag [:character id]]))}
   [:div.drag-ghost
    [c/sprite-texture texture display-name]]
   [:span.ressource__drag_handle
    [c/icon "grip-vertical"]]
   [:span.ressource__icon {:style {:width (u/px icon-size)
                                   :height (u/px icon-size)}}
    [c/sprite-texture texture display-name (/ icon-size config/tile-size)]]
   [:span.ressource__label display-name]])

(defn dialogue [{:keys [id synopsis]}]
  [:li.ressource {:on-click #(>navigate :dialogue-edit :id id)}
   [:span.ressource__drag_handle
    [c/icon "grip-vertical"]]
   [:span.ressource__label synopsis]])

(defn location [{:keys [id display-name]}]
  [:li.ressource {:on-click #(>navigate :location-edit :id id)}
   [:span.ressource__drag_handle
    [c/icon "grip-vertical"]]
   [:span.ressource__label display-name]])

(defn switch [{:keys [id display-name]}]
  [:li.ressource {:on-click #(>evt [:armchair.modals.switch-form/open id])}
   [:span.ressource__drag_handle
    [c/icon "grip-vertical"]]
   [:span.ressource__label display-name]])

(defn ressources []
  [sidebar
   {:panels (array-map
              :characters
              {:label "Characters"
               :icon "user-friends"
               :component (let [characters (<sub [:character-list])]
                            [:ol.ressource_list
                             (for [{:keys [display-name] :as c} characters]
                               ^{:key (str "character-select" display-name)}
                               [character c])])}

              :dialogues
              {:label "Dialogues"
               :icon "comments"
               :component (let [dialogues (<sub [:dialogue-list])]
                            [:ol.ressource_list
                             (for [{:keys [id] :as d} dialogues]
                               ^{:key (str "dialogue-select" id)}
                               [dialogue d])])}

              :locations
              {:label "Locations"
               :icon "map"
               :component (let [locations (<sub [:location-list])]
                            [:ol.ressource_list
                             (for [{:keys [id] :as l} locations]
                               ^{:key (str "location-select" id)}
                               [location l])])}

              :switches
              {:label "Switches"
               :icon "database"
               :component (let [switches (<sub [:switch-list])]
                            [:ol.ressource_list
                             (for [{:keys [id] :as s} switches]
                               ^{:key (str "switch-select" id)}
                               [switch s])])}

              :settings
              {:label "Settings"
               :icon "cog"
               :bottom? true
               :component "Settings"})}])
