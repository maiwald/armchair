(ns armchair.ressources
  (:require [armchair.config :as config]
            [armchair.events :refer [reg-event-meta]]
            [re-frame.core :refer [reg-sub]]
            [armchair.components :as c]
            [armchair.routes :refer [>navigate]]
            [armchair.util :as u :refer [<sub >evt]]))

(reg-event-meta
  ::set-tab
  (fn [db [_ ressource]]
    (if (= (:ui/active-ressource db) ressource)
      (dissoc db :ui/active-ressource)
      (assoc db :ui/active-ressource ressource))))

(reg-sub
  ::active-ressource
  (fn [db] (:ui/active-ressource db)))

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
  (let [active-ressource (<sub [::active-ressource])]
    [:aside#ressources
     [:ul.ressource_selectors
      (for [[ressource [icon label]] (array-map
                                       :characters ["user-friends" "Characters"]
                                       :dialogues ["comments" "Dialogues"]
                                       :locations ["map" "Locations"]
                                       :switches ["database" "Switches"])]
        ^{:key (str "ressource-selector-" ressource)}
        [:li.ressource_selector
         {:class (when (= ressource active-ressource) "is-active")
          :on-click #(>evt [::set-tab ressource])}
         [c/icon icon label {:fixed? true}]])
      [:li.ressource_selector.ressource_selector__settings
       [c/icon "cog" "Settings" {:fixed? true}]]]
     (when (some? active-ressource)
       [:div.ressource_panel
        (case active-ressource
          :characters
          (let [characters (<sub [:character-list])]
            [:ol.ressource_list
             (for [{:keys [display-name] :as c} characters]
              ^{:key (str "character-select" display-name)}
              [character c])])
          :dialogues
          (let [dialogues (<sub [:dialogue-list])]
            [:ol.ressource_list
             (for [{:keys [id] :as d} dialogues]
              ^{:key (str "dialogue-select" id)}
              [dialogue d])])
          :locations
          (let [locations (<sub [:location-list])]
            [:ol.ressource_list
             (for [{:keys [id] :as l} locations]
              ^{:key (str "location-select" id)}
              [location l])])
          :switches
          (let [switches (<sub [:switch-list])]
            [:ol.ressource_list
             (for [{:keys [id] :as s} switches]
              ^{:key (str "switch-select" id)}
              [switch s])])
          nil)])]))
