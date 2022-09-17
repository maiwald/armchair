(ns armchair.components.resources
  (:require [re-frame.core :refer [reg-sub]]
            [herb.core :refer [<class]]
            [armchair.events :refer [reg-event-meta]]
            [armchair.config :as config]
            [armchair.math :refer [Point]]
            [armchair.components :as c]
            [armchair.sprites :refer [Sprite]]
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

(defn Player []
  (let [display-name "Player"
        sprite ["hare.png" (Point. 6 0)]]
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
      [Sprite sprite display-name]]
     [:span.resource__drag_handle
      [c/icon "grip-vertical"]]
     [:span.resource__icon
      {:style {:width (u/px 20)
               :height (u/px 20)}}
      [Sprite sprite display-name (/ 20 config/tile-size)]]
     [:span.resource__label display-name]]))

(defn Character [{:keys [id display-name sprite line-count]}]
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
    [Sprite sprite display-name]]
   [:span.resource__drag_handle
    [c/icon "grip-vertical"]]
   [:span.resource__icon {:style {:width (u/px icon-size)}
                          :height (u/px icon-size)}
    [Sprite sprite display-name (/ icon-size config/tile-size)]]
   [:span.resource__label display-name]
   (when (zero? line-count)
     [:span.resource__action
      [c/icon-button {:icon "trash-alt"
                      :on-click (e-> #(>evt [:delete-character id]))}]])])

(defn Dialogue [{:keys [id synopsis]}]
  [:li.resource {:on-click #(>navigate :dialogue-edit :id id)}
   [:span.resource__label synopsis]
   [:span.resource__action
    [c/icon-button {:icon "trash-alt"
                    :on-click (e-> #(>evt [:delete-dialogue id]))}]]])

(defn Location [{:keys [id display-name]}]
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

(defn Switch [{:keys [id display-name]}]
  [:li.resource {:on-click #(>evt [:armchair.modals.switch-form/open id])}
   [:span.resource__label display-name]
   [:span.resource__action
    [c/icon-button {:icon "trash-alt"
                    :on-click (e-> #(>evt [:delete-switch id]))}]]])

(def resource-categories
  [{:label "Characters"
    :new-resource #(>evt [:armchair.modals.character-form/open])
    :list-sub :character-list
    :item-component Character}
   {:label "Dialogues"
    :new-resource #(>evt [:armchair.modals.dialogue-creation/open])
    :list-sub :dialogue-list
    :item-component Dialogue}
   {:label "Locations"
    :new-resource #(>evt [:armchair.modals.location-creation/open])
    :list-sub :location-list
    :item-component Location}
   {:label "Switches"
    :new-resource #(>evt [:armchair.modals.switch-form/open])
    :list-sub :switch-list
    :item-component Switch}
   {:label "Player"
    :component Player}])

(defn- header-css []
  ^{:combinators {[:> :span] {:flex-grow 1
                              :font-size (u/px 20)}}}
  {:display :flex
   :flex-directin "column"
   :align-items "center"
   :height (u/px 40)})

(defn Resources []
  (into
    [:ul]
    (for [{:keys [label
                  new-resource
                  list-sub
                  component
                  item-component]} resource-categories]
      [:li
       [:header {:class (<class header-css)}
        [:span label]
        (when (some? new-resource)
          [c/icon-button {:icon "plus" :on-click new-resource}])]
       (if (some? list-sub)
         [:ol
          (for [resource (<sub [list-sub])]
            ^{:key (:id resource)}
            [item-component resource])]
         [component])])))
