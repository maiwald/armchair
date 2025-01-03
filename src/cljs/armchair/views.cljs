(ns armchair.views
  (:require [armchair.components :as c :refer [icon]]
            [armchair.location-editor.views :refer [location-editor location-editor-header]]
            [armchair.dialogue-editor.views :refer [dialogue-editor dialogue-editor-header]]
            [armchair.location-map.views :refer [location-map location-map-header]]
            [armchair.components.resources :refer [resource-sidebar]]
            [armchair.components.inspector :refer [inspector]]
            [armchair.modals.views :refer [modal]]
            [armchair.util :as u :refer [<sub >evt]]
            [armchair.routes :refer [page-data >navigate]]
            [armchair.game.views :refer [game-view]]))

;; Navigation

(defn menu-bar [page-name]
  [:ul#menu-bar
   [:li
    (if (not= page-name :game)
      [:a {:on-click #(>navigate :game)}
       [icon "play"] "Play"]
      [:a {:on-click #(>navigate :locations)}
       [icon "edit"] "Edit"])]
   (when (not= page-name :game)
     [:<>
      [:li
       (if (<sub [:can-undo?])
         [:a {:on-click #(>evt [:undo])} [icon "undo"] "Undo"]
         [:span {:class "disabled"} [icon "undo"] "Undo"])]
      [:li
       (if (<sub [:can-redo?])
         [:a {:on-click #(>evt [:redo])} [icon "redo"] "Redo"]
         [:span {:class "disabled"} [icon "redo"] "Redo"])]
      [:li
       [:a {:on-click #(>evt [:download-state])}
        [icon "download"] "Save To File"]]
      [:li
       [:a {:on-click (fn [] (u/upload-json! #(>evt [:upload-state %])))}
        [icon "upload"] "Load From File"]]
      [:li
       [:a {:on-click #(>evt [:reset-db])}
        [icon "snowplow"] "Reset All Data"]]])])


;; Page

(defn page-header [page-name page-params]
  (case page-name
    :locations     [location-map-header]
    :location-edit [location-editor-header (uuid (:id page-params))]
    :dialogue-edit [dialogue-editor-header (uuid (:id page-params))]))

(defn content-component [page-name page-params]
  (case page-name
    :locations     [location-map]
    :location-edit [location-editor (uuid (:id page-params))]
    :dialogue-edit [dialogue-editor (uuid (:id page-params))]
    [:div "Page not found"]))

(defn root []
  (let [{:keys [page-name page-params]} (page-data (<sub [:current-page]))]
    [:<>
     [modal]
     [:div#default-dnd-ghost [c/icon "map-marker"]]
     [:div#page {:class "flex flex-col w-screen h-screen"}
      [menu-bar page-name]
      (if (= page-name :game)
        [game-view]
        [:div#workspace {:class "grow flex flex-row overflow-hidden"}
         [:div {:class "flex"} [resource-sidebar]]
         [:div {:class "grow flex flex-col overflow-hidden"}
          [page-header page-name page-params]
          [:div#main {:class "grow overflow-hidden"}
           [content-component page-name page-params]]]
         [:div {:class "flex"}
          [inspector page-name page-params]]])]]))
