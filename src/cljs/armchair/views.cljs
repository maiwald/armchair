(ns armchair.views
  (:require [reagent.core :as r]
            [armchair.components :as c :refer [icon]]
            [armchair.location-editor.views :refer [location-editor]]
            [armchair.dialogue-editor.views :refer [dialogue-editor]]
            [armchair.location-map.views :refer [location-map]]
            [armchair.components.ressources :refer [ressources]]
            [armchair.components.inspector :refer [inspector]]
            [armchair.modals.views :refer [modal]]
            [armchair.util :as u :refer [<sub >evt]]
            [armchair.routes :refer [page-data >navigate]]
            [armchair.game.views :refer [game-view]]))

;; Navigation

(defn navigation [page-name]
  [:header#global-header
   [:div.logo "Armchair"]
   [:nav
    (if (= page-name :game)
      [:ul.navigation
       [:li {:class ["navigation__item"]}
        [:a {:on-click #(>navigate :locations)} "Edit"]]]
      [:ul.navigation
       [:li {:class ["navigation__item"]}
        [:a {:on-click #(>navigate :game)} "Play"]]
       [:li {:class ["navigation__item"]}
        [:a {:on-click #(>navigate :locations)} "World"]]])
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
    :dialogue-edit [dialogue-editor (uuid (:id page-params))]
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
