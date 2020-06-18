(ns armchair.components.ressources
  (:require [armchair.config :as config]
            [armchair.math :refer [Point]]
            [armchair.components :as c]
            [armchair.components.sidebar :refer [sidebar]]
            [armchair.routes :refer [>navigate]]
            [armchair.util :as u :refer [<sub >evt e->]]))

(def icon-size 20)

(defn player []
  (let [display-name "Player"
        texture ["hare.png" (Point. 6 0)]]
    [:div.ressource {:draggable true
                     :on-drag-end #(>evt [:stop-entity-drag])
                     :on-drag-start (fn [e]
                                      (let [offset (/ config/tile-size 2)
                                            ghost (-> e
                                                      (.-currentTarget)
                                                      (.getElementsByClassName "drag-ghost")
                                                      (aget 0))]
                                        (.setDragImage (.-dataTransfer e) ghost offset offset))
                                      (>evt [:start-entity-drag [:player]]))}
     [:div.drag-ghost
      [c/sprite-texture texture display-name]]
     [:span.ressource__drag_handle
      [c/icon "grip-vertical"]]
     [:span.ressource__icon
      {:style {:width (u/px 20)}
       :height (u/px 20)}
      [c/sprite-texture texture display-name (/ 20 config/tile-size)]]
     [:span.ressource__label display-name]]))

(defn character [{:keys [id display-name texture line-count]}]
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
   [:span.ressource__label display-name]
   (when (zero? line-count)
     [:span.ressource__action
      [c/icon-button {:icon "trash-alt"
                      :on-click (e-> #(>evt [:delete-character id]))}]])])

(defn dialogue [{:keys [id synopsis]}]
  [:li.ressource {:on-click #(>navigate :dialogue-edit :id id)}
   [:span.ressource__drag_handle
    [c/icon "grip-vertical"]]
   [:span.ressource__label synopsis]
   [:span.ressource__action
    [c/icon-button {:icon "trash-alt"
                    :on-click (e-> #(>evt [:delete-dialogue id]))}]]])

(defn location [{:keys [id display-name]}]
  [:li.ressource {:draggable true
                  :on-click #(>navigate :location-edit :id id)
                  :on-drag-end #(>evt [:stop-entity-drag])
                  :on-drag-start (fn [e]
                                   (.setDragImage (.-dataTransfer e) (js/Image.) 0 0)
                                   (>evt [:start-entity-drag [:location id]]))}
   [:span.ressource__drag_handle
    [c/icon "grip-vertical"]]
   [:span.ressource__label display-name]
   [:span.ressource__action
    [c/icon-button {:icon "trash-alt"
                    :on-click (e-> #(>evt [:delete-location id]))}]]])

(defn switch [{:keys [id display-name]}]
  [:li.ressource {:on-click #(>evt [:armchair.modals.switch-form/open id])}
   [:span.ressource__drag_handle
    [c/icon "grip-vertical"]]
   [:span.ressource__label display-name]
   [:span.ressource__action
    [c/icon-button {:icon "trash-alt"
                    :on-click (e-> #(>evt [:delete-switch id]))}]]])

(defn ressources []
  [sidebar
   {:panels (array-map
              :characters
              {:label "Characters"
               :icon "user-friends"
               :component (let [characters (<sub [:character-list])]
                            [:div
                             [c/button {:title "New Character"
                                        :icon "plus"
                                        :fill true
                                        :on-click #(>evt [:armchair.modals.character-form/open])}]
                             [:ol.ressource_list
                              (for [{:keys [display-name] :as c} characters]
                                ^{:key (str "character-select" display-name)}
                                [character c])]])}

              :dialogues
              {:label "Dialogues"
               :icon "comments"
               :component (let [dialogues (<sub [:dialogue-list])]
                            [:div
                             [c/button {:title "New Dialogue"
                                        :icon "plus"
                                        :fill true
                                        :on-click #(>evt [:armchair.modals.dialogue-creation/open])}]
                             [:ol.ressource_list
                              (for [{:keys [id] :as d} dialogues]
                                ^{:key (str "dialogue-select" id)}
                                [dialogue d])]])}

              :locations
              {:label "Locations"
               :icon "map"
               :component (let [locations (<sub [:location-list])]
                            [:div
                             [c/button {:title "New Location"
                                        :icon "plus"
                                        :fill true
                                        :on-click #(>evt [:armchair.modals.location-creation/open])}]
                             [:ol.ressource_list
                              (for [{:keys [id] :as l} locations]
                                ^{:key (str "location-select" id)}
                                [location l])]])}

              :switches
              {:label "Switches"
               :icon "database"
               :component (let [switches (<sub [:switch-list])]
                            [:div
                             [c/button {:title "New Switch"
                                        :icon "plus"
                                        :fill true
                                        :on-click #(>evt [:armchair.modals.switch-form/open])}]
                             [:ol.ressource_list
                              (for [{:keys [id] :as s} switches]
                                ^{:key (str "switch-select" id)}
                                [switch s])]])}

              :player
              {:label "Player"
               :icon "user"
               :component [player]}

              :settings
              {:label "Settings"
               :icon "cog"
               :bottom? true
               :component "Settings"})}])
