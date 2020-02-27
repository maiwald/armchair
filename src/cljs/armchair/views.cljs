(ns armchair.views
  (:require [clojure.spec.alpha :as s]
            [reagent.core :as r]
            [armchair.slds :as slds]
            [armchair.components :as c :refer [icon drag-canvas connection e->graph-cursor]]
            [armchair.location-editor.views :refer [location-editor]]
            [armchair.dialogue-editor.views :refer [dialogue-editor]]
            [armchair.modals.views :refer [modal]]
            [armchair.modals.dialogue-creation :as dialogue-creation]
            [armchair.modals.switch-form :as switch-form]
            [armchair.math :as m]
            [armchair.util :as u :refer [<sub >evt]]
            [armchair.config :as config]
            [armchair.routes :refer [page-data >navigate]]
            [armchair.game.views :refer [game-view]]))

;; Components

(defn dialogue-management []
  (let [dialogues (<sub [:dialogue-list])]
    [slds/data-table
     {:title "dialogues"
      :columns [:texture :character :synopsis :actions]
      :collection dialogues
      :cell-views {:character (fn [{:keys [id display-name]}]
                                [:a {:on-click #(>evt [:armchair.modals.character-form/open id])}
                                 display-name])
                   :texture (fn [texture]
                              [c/sprite-texture texture])
                   :synopsis (fn [synopsis {id :id}]
                               [:a {:on-click #(>navigate :dialogue-edit :id id)}
                                synopsis])
                   :actions (fn [_ {id :id}]
                              [:div {:class "slds-text-align_right"}
                               [c/button {:icon "trash-alt"
                                          :on-click #(when (js/confirm "Are you sure you want to delete this dialogue?")
                                                       (>evt [:delete-dialogue id]))}]])}}]))

(defn character-management []
  (let [characters (<sub [:character-list])]
    [slds/data-table
     {:title "characters"
      :columns [:texture :display-name :color :line-count :actions]
      :collection characters
      :cell-views {:color (fn [color] [slds/badge color color])
                   :texture (fn [texture]
                              [c/sprite-texture texture])
                   :actions (fn [_ {:keys [id line-count]}]
                              [:div {:class "slds-text-align_right"}
                               (when (zero? line-count)
                                 [c/button {:icon "trash-alt"
                                            :on-click #(when (js/confirm "Are you sure you want to delete this character?")
                                                         (>evt [:delete-character id]))}])
                               [c/button {:icon "edit"
                                          :on-click #(>evt [:armchair.modals.character-form/open id])}]])}}]))

(defn switch-management []
  (let [switches (<sub [:switch-list])]
    [slds/data-table
     {:title "Switches"
      :columns [:display-name :values :actions]
      :collection switches
      :cell-views {:color (fn [color] [slds/badge color color])
                   :actions (fn [_ {:keys [id]}]
                              [:div {:class "slds-text-align_right"}
                               [c/button {:icon "trash-alt"
                                          :on-click #(when (js/confirm "Are you sure you want to delete this switch?")
                                                       (>evt [:delete-switch id]))}]
                               [c/button {:icon "edit"
                                          :on-click #(>evt [::switch-form/open id])}]])}}]))

(defn location-component [location-id]
  (let [{:keys [display-name characters]} (<sub [:location-map/location location-id])]
    [:div.location
     [c/graph-node {:title [:a {:on-click #(>navigate :location-edit :id location-id)
                                :on-mouse-down u/stop-e!}
                            display-name]
                    :item-id location-id
                    :actions [["trash" "Delete"
                               #(when (js/confirm "Are you sure you want to delete this location?")
                                  (>evt [:delete-location location-id]))]]}
      [:ul {:class "location__characters"}
       (for [{:keys [dialogue-id
                     character-id
                     character-name
                     character-color]} characters]
         [:li {:key (str "location-dialogue-" location-id " - " character-id)}
          [:a {:style {:background-color character-color}
               :class (when-not dialogue-id "disabled")
               :on-mouse-down u/stop-e!
               :on-click (when dialogue-id #(>navigate :dialogue-edit :id dialogue-id))}
           character-name]])]]]))

(defn location-connection [start end]
  (let [start-pos (<sub [:ui/position start])
        end-pos (<sub [:ui/position end])]
    [connection {:start (m/translate-point start-pos (/ config/line-width 2) 15)
                 :end (m/translate-point end-pos (/ config/line-width 2) 15)}]))

(defn location-management []
  (let [{:keys [location-ids connections]} (<sub [:location-map])]
    [drag-canvas {:kind "location"
                  :nodes {location-component location-ids}}
     [:svg {:class "graph__connection-container" :version "1.1"
            :baseProfile "full"
            :xmlns "http://www.w3.org/2000/svg"}
      (for [[start end] connections]
        ^{:key (str "location-connection" start "->" end)}
        [location-connection start end])]]))

;; Navigation

(defn navigation [page-name]
  [:header#global-header
   [:div.logo "Armchair"]
   [:nav
    (if (= page-name :game)
      [:ul.navigation
       [:li {:class ["navigation__item"]}
        [:a {:on-click #(>navigate :locations)} "Edit Game"]]]
      [:ul.navigation
       [:li {:class ["navigation__item"]}
        [:a {:on-click #(>navigate :game)} "Play"]]
       (for [[route title] [[:locations "Location Map"]
                            [:dialogues "Dialogues"]
                            [:characters "Characters"]
                            [:switches "Switches"]]]
         [:li {:key (str "navigation-item:" title)
               :class ["navigation__item"
                       (when (= page-name route) "is-active")]}
          [:a {:on-click #(>navigate route)} title]])])
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
      [:a {:on-click (fn [e] (u/upload-json! #(>evt [:upload-state %])))}
       [icon "upload"] "load from file"]]
     [:li [:a {:on-click #(>evt [:reset-db])} "reset"]]]]])

(defn toolbar [buttons]
  [:div#toolbar
   (for [{:keys [title icon event]} buttons]
     ^{:key (str title event)}
     [c/button {:title title
                :icon icon
                :on-click #(>evt event)}])])

;; Page

(defn content-component [page-name page-params]
  (case page-name
    :game          [game-view]
    :locations     [location-management]
    :location-edit [location-editor (uuid (:id page-params))]
    :dialogues     [dialogue-management]
    :dialogue-edit [dialogue-editor (uuid (:id page-params))]
    :characters    [character-management]
    :switches      [switch-management]
    [:div "Page not found"]))

(defn root []
  (letfn [(scrollToTop []
            (-> js/document
                (.getElementById "page__content")
                (.-scrollTop)
                (set! 0)))]
    (r/create-class
      {:component-did-mount scrollToTop
       :component-did-update scrollToTop
       :reagent-render
       (fn []
         (let [{:keys [page-name page-params]} (page-data (<sub [:current-page]))
               creation-buttons (<sub [:creation-buttons])]
           [:<>
            [modal]
            [c/popover]
            [:div#page
             [:div#page__navigation
              [navigation page-name]]
             (if (some? creation-buttons)
               [:div#page__toolbar
                [toolbar creation-buttons]])
             [:div#page__content
              ; [:div#page__inspector]
              [content-component page-name page-params]]]]))})))
