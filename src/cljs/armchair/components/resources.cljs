(ns armchair.components.resources
  (:require [reagent.core :as r]
            [armchair.config :as config]
            [armchair.math :refer [Point]]
            [armchair.components :as c]
            [armchair.sprites :refer [Sprite]]
            [armchair.routes :refer [>navigate]]
            [armchair.util :as u :refer [>evt e->]]))

;; Views

(def icon-size 20)

(defn player []
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
      {:style {:width (u/px 20)}
       :height (u/px 20)}
      [Sprite sprite display-name (/ 20 config/tile-size)]]
     [:span.resource__label display-name]]))

(defn character [{:keys [id display-name sprite line-count]}]
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

(def resources
  (array-map
   :characters
   {:label "Characters"
    :label-singular "Character"
    :icon "user-friends"
    :sub [:character-list]
    :item-component character}

   :dialogues
   {:label "Dialogues"
    :label-singular "Dialogue"
    :icon "comments"
    :sub [:dialogue-list]
    :item-component dialogue}

   :locations
   {:label "Locations"
    :label-singular "Location"
    :icon "map"
    :sub [:location-list]
    :item-component location}

   :switches
   {:label "Switches"
    :label-singular "Switch"
    :icon "database"
    :sub [:switch-list]
    :item-component switch}

   :player
   {:label "Player"
    :icon "user"
    :component player}))

(defn resource-sidebar []
  (let [active-panel (r/atom nil)
        toggle-panel #(swap! active-panel (fn [panel-key] (if (= panel-key %) nil %)))]
    (fn []
      [:aside {:class "flex grow border-r border-solid border-zinc-700 bg-zinc-200"}
       [:ul {:class "flex flex-col bg-zinc-300"}
        (doall (for [[panel-key {:keys [icon label]}] resources]
                [:li {:key panel-key
                      :class ["w-14 h-14 flex items-center justify-center"
                              "cursor-pointer"
                              (if (= panel-key @active-panel)
                                "text-white bg-zinc-700"
                                "hover:text-white hover:bg-zinc-700")]
                      :on-click #(toggle-panel panel-key)}
                 [c/icon icon label {:fixed? true}]]))]
       (when-let [panel-key @active-panel]
         (let [{:keys [label]} (resources panel-key)]
           [:div {:class ["flex w-72 flex-col overflow-y-hidden"]}
            [:h2 {:class "text-lg p-2"} label]
            (if-let [component (get-in resources [panel-key :component])]
              [component]
              (let [{:keys [sub item-component]} (resources panel-key)]
                [:ul {:class "overflow-y-auto"}
                 (for [item (u/<sub sub)]
                   ^{:key (:id item)}
                   [item-component item])]))]))])))
