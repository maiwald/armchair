(ns armchair.components.resources
  (:require [re-frame.core :refer [reg-sub]]
            [armchair.events :refer [reg-event-meta]]
            [armchair.config :as config]
            [armchair.math :refer [Point]]
            [armchair.components :as c]
            [armchair.components.sidebar :refer [sidebar]]
            [armchair.textures :refer [sprite-texture]]
            [armchair.routes :refer [>navigate]]
            [armchair.util :as u :refer [<sub >evt e->]]))

;; Events

(reg-event-meta
  ::set-active-resource
  (fn [db [_ resource]]
    (assoc db :ui/active-resource resource)))

(reg-event-meta
  ::unset-active-resource
  (fn [db]
    (dissoc db :ui/active-resource)))

;; Subscriptions

(reg-sub :ui/active-resource #(:ui/active-resource %))

;; Views

(def icon-size 20)

(defn player []
  (let [display-name "Player"
        texture ["hare.png" (Point. 6 0)]]
    [:div.resource {:draggable true
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
      [sprite-texture texture display-name]]
     [:span.resource__drag_handle
      [c/icon "grip-vertical"]]
     [:span.resource__icon
      {:style {:width (u/px 20)}
       :height (u/px 20)}
      [sprite-texture texture display-name (/ 20 config/tile-size)]]
     [:span.resource__label display-name]]))

(defn character [{:keys [id display-name texture line-count]}]
  [:li.resource {:draggable true
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
    [sprite-texture texture display-name]]
   [:span.resource__drag_handle
    [c/icon "grip-vertical"]]
   [:span.resource__icon {:style {:width (u/px icon-size)}
                          :height (u/px icon-size)}
    [sprite-texture texture display-name (/ icon-size config/tile-size)]]
   [:span.resource__label display-name]
   (when (zero? line-count)
     [:span.resource__action
      [c/icon-button {:icon "trash-alt"
                      :on-click (e-> #(>evt [:delete-character id]))}]])])

(defn dialogue [{:keys [id synopsis]}]
  [:li.resource {:on-click #(>navigate :dialogue-edit :id id)}
   [:span.resource__label synopsis]
   [:span.resource__action
    [c/icon-button {:icon "trash-alt"
                    :on-click (e-> #(>evt [:delete-dialogue id]))}]]])

(defn location [{:keys [id display-name]}]
  [:li.resource {:draggable true
                 :on-click #(>navigate :location-edit :id id)
                 :on-drag-end #(>evt [:stop-entity-drag])
                 :on-drag-start (fn [e]
                                  (.setDragImage (.-dataTransfer e) (js/Image.) 0 0)
                                  (>evt [:start-entity-drag [:location id]]))}
   [:span.resource__drag_handle
    [c/icon "grip-vertical"]]
   [:span.resource__label display-name]
   [:span.resource__action
    [c/icon-button {:icon "trash-alt"
                    :on-click (e-> #(>evt [:delete-location id]))}]]])

(defn switch [{:keys [id display-name]}]
  [:li.resource {:on-click #(>evt [:armchair.modals.switch-form/open id])}
   [:span.resource__label display-name]
   [:span.resource__action
    [c/icon-button {:icon "trash-alt"
                    :on-click (e-> #(>evt [:delete-switch id]))}]]])

(defn resources []
  [sidebar
   {:active-panel (<sub [:ui/active-resource])
    :on-panel-change #(>evt [::set-active-resource %])
    :on-panel-close #(>evt [::unset-active-resource %])
    :panels (array-map
              :characters
              {:label "Characters"
               :icon "user-friends"
               :component (let [characters (<sub [:character-list])]
                            [:<>
                             [c/button {:title "New Character"
                                        :icon "plus"
                                        :fill true
                                        :on-click #(>evt [:armchair.modals.character-form/open])}]
                             [:ol.resource_list
                              (for [{:keys [display-name] :as c} characters]
                                ^{:key (str "character-select" display-name)}
                                [character c])]])}

              :dialogues
              {:label "Dialogues"
               :icon "comments"
               :component (let [dialogues (<sub [:dialogue-list])]
                            [:<>
                             [c/button {:title "New Dialogue"
                                        :icon "plus"
                                        :fill true
                                        :on-click #(>evt [:armchair.modals.dialogue-creation/open])}]
                             [:ol.resource_list
                              (for [{:keys [id] :as d} dialogues]
                                ^{:key (str "dialogue-select" id)}
                                [dialogue d])]])}

              :locations
              {:label "Locations"
               :icon "map"
               :component (let [locations (<sub [:location-list])]
                            [:<>
                             [c/button {:title "New Location"
                                        :icon "plus"
                                        :fill true
                                        :on-click #(>evt [:armchair.modals.location-creation/open])}]
                             [:ol.resource_list
                              (for [{:keys [id] :as l} locations]
                                ^{:key (str "location-select" id)}
                                [location l])]])}

              :switches
              {:label "Switches"
               :icon "database"
               :component (let [switches (<sub [:switch-list])]
                            [:<>
                             [c/button {:title "New Switch"
                                        :icon "plus"
                                        :fill true
                                        :on-click #(>evt [:armchair.modals.switch-form/open])}]
                             [:ol.resource_list
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
