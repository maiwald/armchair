(ns armchair.views
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [armchair.slds :as slds]
            [armchair.config :as config]))

;; Helpers

(defn cursor-position [e]
  [(.-pageX e) (.-pageY e)])

(defn event-only [handler]
  (fn [e]
    (.preventDefault e)
    (.stopPropagation e)
    (handler e)))

(defn mousedown
  "Only handle left button mousedown event"
  [handler]
  (fn [e]
    (when (zero? (.-button e))
      ((event-only handler) e))))

(defn start-drag-handler [position-ids]
  (mousedown #(dispatch [:start-drag position-ids (cursor-position %)])))

(defn record-update-handler [record-type id field]
  (let [record-event (keyword (str "update-" (name record-type)))]
    (fn [event]
      (dispatch [record-event id field (-> event .-target .-value)]))))

;; Graph (drag & drop)

(defn graph-item [{:keys [position position-id]} component]
  (let [dragging? @(subscribe [:dragging?])]
    [:div {:class "graph__item"
           :on-mouse-down (start-drag-handler #{position-id})
           :on-mouse-up (when dragging? (event-only #(dispatch [:end-drag])))
           :style {:left (first position)
                   :top (second position)}}
     component]))

(defn graph-connection [{:keys [id kind start end]}]
  [:line {:class (str "graph__connection "
                      (when (= kind :drag-connection) "graph__connection_is-drag"))
          :key id
          :x1 (first start)
          :y1 (second start)
          :x2 (first end)
          :y2 (second end)}])

(defn graph [{:keys [items kind item-component connections] :or {connections '()}}]
  (let [position-ids (->> items vals (map :position-id) set)
        dragging? @(subscribe [:dragging?])]
    [:div {:class (str "graph " (when dragging? "graph_is-dragging"))
           :on-mouse-down (start-drag-handler position-ids)
           :on-mouse-move (when dragging? #(dispatch [:move-pointer (cursor-position %)]))
           :on-mouse-up (when dragging? (event-only #(dispatch [:end-drag])))}
     [:svg {:class "graph__connection-container" :version "1.1"
            :baseProfile "full"
            :xmlns "http://www.w3.org/2000/svg"}
      (for [connection connections]
        ^{:key (:id connection)} [graph-connection connection])]
     (for [[id item] items]
       ^{:key (str kind id)} [graph-item item [item-component item]])]))

;; Components

(defn line-component [{:keys [id text character-color] :as line}]
  [:div {:class "line"
         :on-mouse-up #(dispatch [:end-connection id])
         :style {:border-color character-color
                 :width (str config/line-width "px")}}
   [:p text]
   [:div {:class "edit-action fas fa-trash"
          :on-click #(dispatch [:delete-line id])}]
   [:div {:class "edit-action fas fa-edit"
          :on-click #(dispatch [:open-line-modal id])}]
   [:div {:class "connection-handle fas fa-link"
          :on-mouse-down (mousedown #(dispatch [:start-connection id (cursor-position %)]))}]])

(defn line-form-modal []
  (let [{:keys [line-id]} @(subscribe [:modal])
        update-handler (partial record-update-handler :line line-id)]
    (if-let [line (get @(subscribe [:lines]) line-id)]
      (let [{:keys [id text character-id color]} line
            characters @(subscribe [:characters])]
        [slds/modal {:title (str "Line #" id)
                     :close-handler #(dispatch [:close-modal])
                     :content [slds/form
                               [slds/input-select {:label "Character"
                                                   :on-change (update-handler :character-id)
                                                   :options (map (fn [[k c]] [k (:display-name c)]) characters)
                                                   :value character-id}]
                               [slds/input-textarea {:label "Text"
                                                     :on-change (update-handler :text)
                                                     :value text}]]}]))))

(defn dialogue-component []
  (let [lines @(subscribe [:lines])
        line-connections @(subscribe [:line-connections])]
    [:div {:class "full-page"}
     [:div {:class "new-item-button"}
      [slds/add-button "New" #(dispatch [:create-line])]]
     [graph {:kind "line"
             :items lines
             :connections line-connections
             :item-component line-component}]]))

(defn character-form-modal []
  (let [{:keys [character-id]} @(subscribe [:modal])
        update-handler (partial record-update-handler :character character-id)]
    (if-let [character (get @(subscribe [:characters]) character-id)]
      (let [{:keys [display-name color]} character]
        [slds/modal {:title display-name
                     :close-handler #(dispatch [:close-modal])
                     :content [slds/form
                               [slds/input-text {:label "Name"
                                                 :on-change (update-handler :display-name)
                                                 :value display-name}]
                               [slds/input-text {:label "Color"
                                                 :on-change (update-handler :color)
                                                 :value color}]]}]))))

(defn character-management []
  (let [characters @(subscribe [:characters])]
    [slds/resource-page "Characters"
     {:columns [:id :display-name :color :lines :actions]
      :collection (vals characters)
      :cell-views {:color slds/color-cell
                   :actions (fn [{:keys [id lines]} _]
                              [:div {:class "slds-text-align_right"}
                               (when (zero? lines)
                                 [slds/symbol-button "trash-alt" {:on-click #(dispatch [:delete-character id])}])
                               [slds/symbol-button "edit" {:on-click #(dispatch [:open-character-modal id])}]])}
      :new-resource #(dispatch [:create-character])}]))

(defn location-form-modal []
  (let [{:keys [location-id]} @(subscribe [:modal])
        update-handler (partial record-update-handler :location location-id)]
    (if-let [location (get @(subscribe [:locations]) location-id)]
      (let [display-name (:display-name location)]
        [slds/modal {:title display-name
                     :close-handler #(dispatch [:close-modal])
                     :content [slds/form
                               [slds/input-text {:label "Name"
                                                 :on-change (update-handler :display-name)
                                                 :value display-name}]]}]))))

(defn location-component [{:keys [id display-name] :as location}]
  [:div {:class "location"}
   [:p {:class "name"} display-name]
   [:div {:class "delete-action fas fa-trash"
          :on-click #(dispatch [:delete-location id])}]
   [:div {:class "edit-action fas fa-edit"
          :on-click #(dispatch [:open-location-modal id])}]])

(defn location-management []
  (let [locations @(subscribe [:locations])]
    [:div {:class "full-page"}
     [:div {:class "new-item-button"}
      [slds/add-button "New" #(dispatch [:create-location])]]
     [graph {:kind "location"
             :items locations
             :item-component location-component}]]))

(defn root []
  (let [current-page @(subscribe [:current-page])
        pages (array-map
                "Dialogue" [dialogue-component]
                "Characters" [character-management]
                "Locations" [location-management])
        link-map (map
                   (fn [name] [name #(dispatch [:show-page name])])
                   (keys pages))]
    [:div {:id "page"}
     [line-form-modal]
     [character-form-modal]
     [location-form-modal]
     [:a {:id "reset"
          :on-click #(dispatch [:reset-db])} "reset"]
     [:div {:id "navigation"}
      [slds/global-navigation link-map current-page]]
     [:div {:id "content"}
      (get pages current-page [:div "Nothing"])]]))
