(ns armchair.views
  (:require [reagent.core :as r]
            [armchair.slds :as slds]
            [armchair.components :as c :refer [icon]]
            [armchair.location-editor.views :refer [location-editor]]
            [armchair.dialogue-editor.views :refer [dialogue-editor]]
            [armchair.location-map.views :refer [location-map]]
            [armchair.components.ressources :refer [ressources]]
            [armchair.components.inspector :refer [inspector]]
            [armchair.modals.views :refer [modal]]
            [armchair.modals.switch-form :as switch-form]
            [armchair.util :as u :refer [<sub >evt]]
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
      [:a {:on-click (fn [] (u/upload-json! #(>evt [:upload-state %])))}
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
    :locations     [location-map]
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
               creation-buttons (<sub [:creation-buttons])
               inspector? (<sub [:ui/inspector?])]
           [:<>
            [modal]
            [:div#page
             [:div#page__header
              [navigation page-name]]
             [:div#page__workspace
              (when-not (= page-name :game)
                [:div#page__workspace__ressources
                 [ressources]])
              [:div#page__workspace__main
               (when (some? creation-buttons)
                 [:div#page__toolbar
                  [toolbar creation-buttons]])
               [:div#page__content
                [content-component page-name page-params]]]
              (when inspector?
                [:div#page__workspace__inspector
                 [inspector]])]]]))})))
