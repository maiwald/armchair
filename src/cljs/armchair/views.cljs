(ns armchair.views
  (:require [armchair.components :as c :refer [icon]]
            [armchair.location-editor.views :refer [location-editor]]
            [armchair.dialogue-editor.views :refer [dialogue-editor]]
            [armchair.location-map.views :refer [location-map]]
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

(defn content-component [page-name page-params]
  (case page-name
    :locations     [location-map]
    :location-edit [location-editor (uuid (:id page-params))]
    :dialogue-edit [dialogue-editor (uuid (:id page-params))]
    [:div "Page not found"]))

(defn tabs []
  (let [pages (<sub [:open-pages])
        current-page (<sub [:current-page])]
    [:ul#tabs {:class "shrink-0 mt-1 ms-1 flex gap-1"}
     (for [page pages
           :let [{:keys [page-name page-params]} (page-data page)
                 active? (= page current-page)]]
       ^{:key page}
       [:li {:class ["flex gap-1 rounded-t items-top"
                     (if active?
                       "text-white bg-sky-800"
                       "text-slate-800 bg-slate-50 hover:bg-sky-100")]}
        [:a {:class "py-2 px-4 hover:no-underline"
             :on-click #(>navigate page-name page-params)}
         (case page-name
           :locations     [:span {:class "flex gap-2 items-baseline"}
                           [c/icon "globe"] "World"]
           :location-edit [:span {:class "flex gap-2 items-baseline"}
                           [c/icon "map"]
                           (str "Location " (u/truncate (:id page-params) 8))]
           :dialogue-edit [:span {:class "flex gap-2 items-baseline"}
                           [c/icon "comments"]
                           (str "Dialogue " (u/truncate (:id page-params) 8))])]
        (when-not (= page-name :locations) ;; You cannot close the world tab
          [:div {:class ["pr-2 py-2" (if active? "text-white" "text-gray-800")]}
           [c/icon-button {:icon "times"
                           :title "Close Paage"
                           :on-click #(>evt [:close-page page])}]])])]))

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
         [:div#main {:class "grow flex flex-col overflow-hidden"}
          [tabs]
          [:div {:class "grow flex flex-row overflow-hidden"}
           [:div#content {:class "flex flex-col grow overflow-hidden"}
            [content-component page-name page-params]]
           [inspector page-name page-params]]]])]]))
