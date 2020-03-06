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
            [armchair.game.views :refer [game-view]]
            [goog.functions :refer [debounce]]))

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

(def map-scale 0.5)

(defn location-component [location-id]
  (let [{:keys [dimension
                display-name
                preview-image-src]} (<sub [:location-map/location location-id])]
    [:div.location
     [c/graph-node {:title [:a {:on-click #(>navigate :location-edit :id location-id)
                                :on-mouse-down u/stop-e!}
                            display-name]
                    :item-id location-id
                    :width nil
                    :actions [["trash" "Delete"
                               #(when (js/confirm "Are you sure you want to delete this location?")
                                  (>evt [:delete-location location-id]))]]}
      (if (some? preview-image-src)
        [:img {:src preview-image-src
               :on-click #(>evt [:inspect :location location-id])
               :style {:width (u/px (* config/tile-size (:w dimension) map-scale))
                       :height (u/px (* config/tile-size (:h dimension) map-scale))}}])]]))

(defn location-connection [dimensions start end]
  (let [start-pos (m/global-point (<sub [:ui/position start]) dimensions)
        end-pos (m/global-point (<sub [:ui/position end]) dimensions)]
    [connection {:start (m/translate-point start-pos (/ config/line-width 2) 15)
                 :end (m/translate-point end-pos (/ config/line-width 2) 15)}]))

(defn location-map []
  (let [update-offset (debounce #(>evt [:update-location-map-offset %]) 200)
        on-scroll (fn [e]
                    (let [target (.-currentTarget e)
                          offset (m/Point. (.-scrollLeft target) (.-scrollTop target))]
                      (update-offset offset)))]
    (fn []
      (let [{:keys [dimensions scroll-offset location-ids connections]} (<sub [:location-map map-scale])]
        [drag-canvas {:kind "location"
                      :dimensions dimensions
                      :scroll-offset scroll-offset
                      :on-scroll on-scroll
                      :nodes {location-component location-ids}}
         [:svg {:class "graph__connection-container" :version "1.1"
                :baseProfile "full"
                :xmlns "http://www.w3.org/2000/svg"}
          (for [[start end] connections]
            ^{:key (str "location-connection" start "->" end)}
            [location-connection dimensions start end])]]))))

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

(defn inspector []
  (let [[inspector-type {:keys [location-id location-position]}] (<sub [:ui/inspector])]
    (case inspector-type
      :placement [armchair.location-editor.views/placement-inspector location-id location-position]
      :exit [armchair.location-editor.views/trigger-inspector location-id location-position]
      :location [armchair.location-editor.views/location-inspector location-id])))

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
             [:div#page__navigation
              [navigation page-name]]
             (if (some? creation-buttons)
               [:div#page__toolbar
                [toolbar creation-buttons]])
             [:div#page__content
              [content-component page-name page-params]
              (if inspector? [:div#page__inspector [inspector]])]]]))})))
