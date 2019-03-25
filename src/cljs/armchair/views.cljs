(ns armchair.views
  (:require [clojure.spec.alpha :as s]
            [reagent.core :as r]
            [armchair.slds :as slds]
            [armchair.components :as c :refer [icon drag-canvas connection e->graph-cursor]]
            [armchair.location-editor.views :refer [location-editor]]
            [armchair.dialogue-editor.views :refer [dialogue-editor]]
            [armchair.modals.views :refer [modal]]
            [armchair.util :as u :refer [<sub >evt stop-e! e-> e->left? e->val]]
            [armchair.config :as config]
            [armchair.routes :refer [routes >navigate]]
            [armchair.textures :refer [texture-path]]
            [armchair.game.views :refer [game-canvas]]
            [bidi.bidi :refer [match-route]]))

;; Components

(defn dialogue-management []
  (let [dialogues (<sub [:dialogue-list])]
    [slds/resource-page "Dialogues"
     {:columns [:texture :character :synopsis :location :actions]
      :collection (vals dialogues)
      :cell-views {:character (fn [{:keys [id display-name]}]
                                [:a {:on-click #(>evt [:open-character-modal id])}
                                 display-name])
                   :texture (fn [texture]
                              [:img {:src (texture-path texture)}])
                   :synopsis (fn [synopsis {id :id}]
                               [:a {:on-click #(>navigate :dialogue-edit :id id)}
                                synopsis])
                   :location (fn [{:keys [id display-name]}]
                               [:a {:on-click #(>navigate :location-edit :id id)}
                                display-name])
                   :actions (fn [_ {id :id}]
                              [:div {:class "slds-text-align_right"}
                               [slds/symbol-button "trash-alt" {:on-click #(when (js/confirm "Are you sure you want to delete this dialogue?")
                                                                             (>evt [:delete-dialogue id]))}]])}
      :new-resource #(>evt [:open-dialogue-creation-modal])}]))

(defn character-management []
  (let [characters (<sub [:character-list])]
    [slds/resource-page "Characters"
     {:columns [:texture :display-name :color :line-count :actions]
      :collection (vals characters)
      :cell-views {:color (fn [color] [slds/badge color color])
                   :texture (fn [texture]
                              [:img {:src (texture-path texture)}])
                   :actions (fn [_ {:keys [id line-count]}]
                              [:div {:class "slds-text-align_right"}
                               (when (zero? line-count)
                                 [slds/symbol-button "trash-alt" {:on-click #(when (js/confirm "Are you sure you want to delete this character?")
                                                                               (>evt [:delete-character id]))}])
                               [slds/symbol-button "edit" {:on-click #(>evt [:open-character-modal id])}]])}
      :new-resource #(>evt [:open-character-modal])}]))

(defn switch-management []
  (let [switches (<sub [:switch-list])]
    [slds/resource-page "Switches"
     {:columns [:display-name :values :actions]
      :collection switches
      :cell-views {:color (fn [color] [slds/badge color color])
                   :actions (fn [_ {:keys [id]}]
                              [:div {:class "slds-text-align_right"}
                               [slds/symbol-button "trash-alt" {:on-click #(when (js/confirm "Are you sure you want to delete this switch?")
                                                                               (>evt [:delete-switch id]))}]
                               [slds/symbol-button "edit" {:on-click #(>evt [:modal/open-switch-modal id])}]])}
      :new-resource #(>evt [:modal/open-switch-modal])}]))

(defn location-component [location-id]
  (let [{:keys [display-name dialogues]} (<sub [:location-map/location location-id])
        connecting? (some? (<sub [:connector]))]
    [:div.location
     [c/graph-node {:title [:a {:on-click #(>navigate :location-edit :id location-id)}
                            display-name]
                    :item-id location-id
                    :on-connect-end #(>evt [:end-connecting-locations location-id])
                    :actions [["trash" "Delete"
                               #(when (js/confirm "Are you sure you want to delete this location?")
                                  (>evt [:delete-location location-id]))]]}
      [c/action-wrapper {:actions
                         [[:div {:class "action action_connect"
                                 :on-mouse-down (e-> #(when (e->left? %)
                                                        (>evt [:start-connecting-locations location-id (e->graph-cursor %)])))}
                           [icon "project-diagram" "Connect"]]]}
       [:ul {:class "location__characters"}
        (for [{:keys [dialogue-id npc-name npc-color]} dialogues]
          [:li {:key (str "location-dialogue-" location-id " - " dialogue-id)}
           [:a {:style {:background-color npc-color}
                :on-mouse-down stop-e!
                :on-click #(>navigate :dialogue-edit :id dialogue-id)}
            npc-name]])]]]]))

(defn location-connection [start end]
  (let [start-pos (<sub [:ui/position start])
        end-pos (<sub [:ui/position end])]
    [connection {:start (u/translate-point start-pos [(/ config/line-width 2) 15])
                 :end (u/translate-point end-pos [(/ config/line-width 2) 15])}]))

(defn location-management []
  (let [{:keys [location-ids connections]} (<sub [:location-map])]
    [:div {:class "content-wrapper"}
     [:div {:class "new-item-button"}
      [slds/add-button "New" #(>evt [:open-location-creation])]]
     [drag-canvas {:kind "location"
                   :nodes {location-component location-ids}}
      [:svg {:class "graph__connection-container" :version "1.1"
             :baseProfile "full"
             :xmlns "http://www.w3.org/2000/svg"}
       (when-let [connector (<sub [:connector])]
         [connection connector])
       (for [[start end] connections]
         ^{:key (str "location-connection" start "->" end)}
         [location-connection start end])]]]))


;; Navigation

(defn navigation []
  (let [dropdown-open? (r/atom false)
        select (fn [resource]
                 (>navigate resource)
                 (swap! dropdown-open? not))]
    (fn []
      (let [{page-name :handler
             page-params :route-params} (match-route routes (<sub [:current-page]))]
        [:header {:id "global-header"}
         [:div.logo "Armchair"]
         [:nav
          [:ul.main.navigation-container
           [:li {:class ["navigation__item"
                         (when (= page-name :game) "is-active")]}
            [:a {:on-click #(>navigate :game)} "Play"]]
           [:li {:class ["navigation__item"
                         (when (= page-name :locations) "is-active")]}
            [:a {:on-click #(>navigate :locations)}
             (if (= page-name :game) "Edit" "Locations")]]]
          (into [:ol.breadcrumb.navigation-container]
                (let [{:keys [location dialogue]} (<sub [:breadcrumb])]
                  [
                   (when-let [{:keys [id display-name]} location]
                     [:li.navigation__item
                      {:class (when (= :location-edit page-name) "is-active")}
                      [:a {:on-click #(>navigate :location-edit :id id)}
                       [:span.navigation__item__type "Location"]
                       [:span.navigation__item__title display-name]]])
                   (when-let [{:keys [id character-name synopsis]} dialogue]
                     [:li.navigation__item
                      {:class (when (= :dialogue-edit page-name) "is-active")}
                      [:a {:on-click #(>navigate :dialogue-edit :id id)}
                       [:span.navigation__item__type "Dialogue"]
                       [:span.navigation__item__title
                        (str character-name ": " synopsis)]]])]))
          (let [resource-pages {:dialogues "Dialogues"
                                :characters "Characters"
                                :switches "Switches"}
                active? (contains? resource-pages page-name)]
            [:div.resources {:class ["navigation__item"
                                     (when active? "is-active")]}
             [:a.resources__title {:on-click (fn [] (swap! dropdown-open? not))}
              (when active?
                [:span.navigation__item__type "Resources"])
              [:span.navigation__item__title
               (get resource-pages page-name "Resources")]
              [icon "caret-down"]]
             (when @dropdown-open?
               [:ul.resources__dropdown-list
                (for [[route resource] resource-pages]
                  [:li {:key (str "resource-" route)}
                   [:a {:on-click #(select route)} resource]])])])
          [:ul.functions
           [:li
            (if (<sub [:can-undo?])
              [:a {:on-click #(>evt [:undo])} [icon "undo"] "undo"]
              [:span {:class "disabled"} [icon "undo"] "undo"])]
           [:li
            (if (<sub [:can-redo?])
              [:a {:on-click #(>evt [:redo])} [icon "redo"] "redo"]
              [:span {:class "disabled"} [icon "redo"] "redo"])]
           [:li
            [:a {:on-click #(>evt [:download-state])}
             [icon "download"] "save to file"]]
           [:li
            [:a {:on-click #(u/upload-json! (fn [json] (>evt [:upload-state json])))}
             [icon "upload"] "load from file"]]
           (when config/debug?
             [:li [:a {:on-click #(>evt [:reset-db])} "reset"]])
           (when-not config/debug?
             [:li [:a {:href "https://github.com/maiwald/armchair"}]
              :target "_blank" [icon "code-branch"] "source"])]]]))))

;; Root

(defn root []
  (let [{page-name :handler
         page-params :route-params} (match-route routes (<sub [:current-page]))]
    [:div {:id "page"}
     [modal]
     [navigation]
     [:div {:id "content"}
      (case page-name
        :game          [game-canvas]
        :locations     [location-management]
        :location-edit [location-editor (uuid (:id page-params))]
        :dialogues     [dialogue-management]
        :dialogue-edit [dialogue-editor (uuid (:id page-params))]
        :characters    [character-management]
        :switches      [switch-management]
        [:div "Page not found"])]]))
