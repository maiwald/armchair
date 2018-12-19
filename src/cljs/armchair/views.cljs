(ns armchair.views
  (:require [clojure.spec.alpha :as s]
            [reagent.core :as r]
            [armchair.slds :as slds]
            [armchair.components :as c :refer [icon drag-canvas connection e->graph-cursor]]
            [armchair.location-editor.views :refer [location-editor]]
            [armchair.dialogue-editor.views :refer [dialogue-editor]]
            [armchair.util :refer [<sub
                                   >evt
                                   stop-e!
                                   e->
                                   e->left?
                                   e->val
                                   translate-point
                                   upload-json!]]
            [armchair.config :as config]
            [armchair.routes :refer [routes >navigate]]
            [armchair.textures :refer [character-textures texture-path]]
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

(defn info-management []
  (let [infos (<sub [:info-list])]
    [slds/resource-page "Infos"
     {:columns [:description :actions]
      :collection (vals infos)
      :cell-views {:actions (fn [_ {id :id}]
                              [:div {:class "slds-text-align_right"}
                               [slds/symbol-button "trash-alt" {:on-click #(when (js/confirm "Are you sure you want to delete this info?")
                                                                             (>evt [:delete-info id]))}]
                               [slds/symbol-button "edit" {:on-click #(>evt [:open-info-modal id])}]])}
      :new-resource #(>evt [:open-info-modal])}]))

(defn location-component [location-id]
  (let [{:keys [display-name dialogues]} (<sub [:location-map/location location-id])
        connecting? (some? (<sub [:connector]))]
    [:div {:class "location"
           :on-mouse-up (when connecting? #(>evt [:end-connecting-locations location-id]))
           :style {:width (str config/line-width "px")}}
     [:div {:class "location__header"}
      [:p {:class "name"}
       [:a {:on-click #(>navigate :location-edit :id location-id)
            :on-mouse-down stop-e!}
           display-name]]
      [:ul {:class "actions"
            :on-mouse-down stop-e!}
       [:li {:class "action"
             :on-click #(when (js/confirm "Are you sure you want to delete this location?")
                          (>evt [:delete-location location-id]))}
        [icon "trash" "Delete"]]
       [:li {:class "action action_connect"
             :on-mouse-down (e-> #(when (e->left? %)
                                    (>evt [:start-connecting-locations location-id (e->graph-cursor %)])))}
        [icon "project-diagram" "Connect"]]]]
     [:ul {:class "location__characters"}
      (for [{:keys [dialogue-id npc-name npc-color]} dialogues]
        [:li {:key (str "location-dialogue-" location-id " - " dialogue-id)}
         [:a {:style {:background-color npc-color}
              :on-mouse-down stop-e!
              :on-click #(>navigate :dialogue-edit :id dialogue-id)}
          npc-name]])]]))

(defn location-connection [start end]
  (let [start-pos (<sub [:ui/position start])
        end-pos (<sub [:ui/position end])]
    [connection {:start (translate-point start-pos [(/ config/line-width 2) 15])
                 :end (translate-point end-pos [(/ config/line-width 2) 15])}]))

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

;; Modals

(defn dialogue-creation-modal [{:keys [character-id synopsis]}]
  [slds/modal {:title "Create Dialogue"
               :confirm-handler #(>evt [:create-dialogue])
               :close-handler #(>evt [:close-modal])}
   [slds/form
    [slds/input-select {:label "Character *"
                        :on-change #(>evt [:dialogue-creation-update :character-id (uuid (e->val %))])
                        :options (<sub [:dialogue-creation/character-options])
                        :value character-id}]
    [slds/input-textarea {:label "Synopsis *"
                          :on-change #(>evt [:dialogue-creation-update :synopsis (e->val %)])
                          :value synopsis}]]])

(defn dialogue-state-modal [{:keys [line-id description]}]
  [slds/modal {:title "Dialogue State"
               :confirm-handler #(>evt [:create-dialogue-state])
               :close-handler #(>evt [:close-modal])}
   [slds/form
    [slds/input-text {:label "State description"
                      :on-change #(>evt [:dialogue-state-update (e->val %)])
                      :options (<sub [:character-options])
                      :value description}]]])

(defn npc-line-form-modal [line-id]
  (let [line (<sub [:dialogue/modal-line line-id])]
    [slds/modal {:title "NPC Line"
                 :close-handler #(>evt [:close-modal])}
     [slds/form
      [slds/input-select {:label "Character"
                          :disabled (:initial-line? line)
                          :on-change #(>evt [:update-line line-id :character-id (uuid (e->val %))])
                          :options (<sub [:character-options])
                          :value (:character-id line)}]
      [slds/input-textarea {:label "Text"
                            :on-change #(>evt [:update-line line-id :text (e->val %)])
                            :value (:text line)}]
      [slds/multi-select {:label "Infos"
                          :options (clj->js (<sub [:info-options]))
                          :values (:info-ids line)
                          :on-change #(>evt [:set-infos line-id (map uuid %)])}]
      [slds/multi-select {:label "Dialogue State Triggers"
                          :options (clj->js (<sub [:dialogue/state-options line-id]))
                          :values (:state-triggers line)
                          :on-change #(>evt [:set-state-triggers line-id (map uuid %)])}]]]))

(defn player-line-form-modal-option [line-id index total-count]
  (let [{:keys [text required-info-ids state-triggers]} (<sub [:dialogue/player-line-option line-id index])
        info-options (<sub [:info-options])]
    [:div { :class "player-line-form__response"}
     [:div {:class "text"}
      [slds/input-textarea {:label (str "Response " (inc index))
                            :on-change #(>evt [:update-option line-id index (e->val %)])
                            :value text}]
      [slds/multi-select {:label "Required Infos"
                          :options info-options
                          :values required-info-ids
                          :on-change #(>evt [:set-required-info line-id index (map uuid %)])}]
      [slds/multi-select {:label "Dialogue State Triggers"
                          :options (clj->js (<sub [:dialogue/state-options line-id index]))
                          :values state-triggers
                          :on-change #(>evt [:set-state-triggers line-id (map uuid %) index])}]]
     [:ul {:class "actions actions_vertical"}
      [:li {:class "action" :on-click #(when (js/confirm "Are you sure you want to delete this option?")
                                         (>evt [:delete-option line-id index]))}
       [icon "trash" "Delete"]]
      (when-not (= index 0)
        [:li {:class "action" :on-click #(>evt [:move-option line-id index :up])}
         [icon "arrow-up" "Move up"]])
      (when-not (= index (dec total-count))
        [:li {:class "action" :on-click #(>evt [:move-option line-id index :down])}
         [icon "arrow-down" "Move down"]])]]))

(defn player-line-form-modal [line-id]
  (let [{:keys [option-count]} (<sub [:dialogue/modal-line line-id])]
    [slds/modal {:title "Player Line"
                 :close-handler #(>evt [:close-modal])}
     [:div {:class "player-line-form"}
      [slds/form
       (for [index (range option-count)]
         ^{:key (str "option:" line-id ":" index)}
         [player-line-form-modal-option line-id index option-count])
       [slds/add-button "New Option" #(>evt [:add-option line-id])]]]]))

(defn character-form-modal [{:keys [display-name color texture]}]
  (let [update-handler (fn [field] #(>evt [:character-form/update field (e->val %)]))]
    [slds/modal {:title "Character"
                 :close-handler #(>evt [:close-modal])
                 :confirm-handler #(>evt [:character-form/save])}
     [slds/form
      [slds/input-text {:label "Name"
                        :on-change (update-handler :display-name)
                        :value display-name}]
      [slds/input-text {:label "Color"
                        :on-change (update-handler :color)
                        :value color}]
      [:div {:class "color-picker"}
       (for [c config/color-grid]
         [:div {:key (str "color-picker-color:" c)
                :class ["color-picker__color"
                        (when (= c color) "color-picker__color_selected")]
                :on-click #(>evt [:character-form/update :color c])
                :style {:background-color c}}])]
      [slds/input-select {:label "Avatar"
                          :options (mapv #(vector % %) character-textures)
                          :value texture
                          :on-change #(>evt [:character-form/update :texture (keyword (e->val %))])}]
      [:img {:src (texture-path texture)}]]]))

(defn info-form-modal [{:keys [description]}]
  (let [update-description #(>evt [:update-info (e->val %)])]
    [slds/modal {:title "Info"
                 :close-handler #(>evt [:close-modal])
                 :confirm-handler #(>evt [:save-info])}
     [slds/form
      [slds/input-text {:label "Description"
                        :on-change update-description
                        :value description}]]]))

(defn location-creation-modal [display-name]
  (let [update-name #(>evt [:update-location-creation-name (e->val %)])]
    [slds/modal {:title "Create Location"
                 :close-handler #(>evt [:close-modal])
                 :confirm-handler #(>evt [:create-location])}
     [slds/form
      [slds/input-text {:label "Name"
                        :on-change update-name
                        :value display-name}]]]))

(defn modal []
  (if-let [modal (<sub [:modal])]
    (condp #(contains? %2 %1) modal
      :dialogue-creation [dialogue-creation-modal (:dialogue-creation modal)]
      :dialogue-state    [dialogue-state-modal (:dialogue-state modal)]
      :npc-line-id       [npc-line-form-modal (:npc-line-id modal)]
      :player-line-id    [player-line-form-modal (:player-line-id modal)]
      :character-form    [character-form-modal (:character-form modal)]
      :info-form         [info-form-modal (:info-form modal)]
      :location-creation [location-creation-modal (:location-creation modal)])))

;; Navigation

(defn navigation []
  (let [dropdown-open? (r/atom false)]
    (fn []
      (let [{page-name :handler
             page-params :route-params} (match-route routes (<sub [:current-page]))
            select (fn [resource]
                     (>navigate resource)
                     (swap! dropdown-open? not))]
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
          (let [active? (contains? #{:dialogues :characters :infos} page-name)]
            [:div.resources {:class ["navigation__item"
                                     (when active? "is-active")]}
             [:a.resources__title {:on-click (fn [] (swap! dropdown-open? not))}
              [:span.navigation__item__type
               (when active? "Resources")]
              [:span.navigation__item__title
               (condp = page-name
                 :dialogues "Dialogues"
                 :characters "Characters"
                 :infos "Infos"
                 "Resources")]
              [icon "caret-down"]]
             (when @dropdown-open?
               [:ul.resources__dropdown-list
                [:li [:a {:on-click #(select :dialogues)} "Dialogues"]]
                [:li [:a {:on-click #(select :characters)} "Characters"]]
                [:li [:a {:on-click #(select :infos)} "Infos"]]])])
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
            [:a {:on-click #(upload-json! (fn [json] (>evt [:upload-state json])))}
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
        :infos         [info-management]
        [:div "Page not found"])]]))
