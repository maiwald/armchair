(ns armchair.views.location-editor
  (:require [armchair.slds :as slds]
            [armchair.config :as config]
            [armchair.util :refer [once]]
            [armchair.textures :refer [texture-path background-textures]]))

;; Helpers

(def <sub (comp deref re-frame.core/subscribe))
(def >evt re-frame.core/dispatch)

(defn stop-e! [e]
  (.preventDefault e)
  (.stopPropagation e)
  e)

(defn e-> [handler]
  (comp handler stop-e!))

(defn e->val [e]
  (let [target (.-target e)]
    (case (.-type target)
      "checkbox" (.-checked target)
      (.-value target))))

;; Components

(defn icon [glyph title]
  [:i {:class (str "fas fa-" glyph)
       :title title}])

;; Location Editor

(defn set-drag-texture! [e texture]
  (let [offset (/ config/tile-size 2)
        image (js/Image.)]
    (set! (.-src image) (texture-path texture))
    (-> e .-dataTransfer (.setDragImage image offset offset))))

(defn location-editor-sidebar-paint []
  (let [{active-texture :active-texture} (<sub [:location-editor-data])]
    [slds/label "Background Textures"
     [:ul {:class "tile-grid"}
      (for [texture background-textures]
        [:li {:key (str "texture-select:" texture)
              :title texture
              :class ["tile-grid__item"
                      (when (= texture active-texture) "tile-grid__item_active")]}
         [:a {:on-click #(>evt [:set-active-texture texture])}
          [:img {:src (texture-path texture)}]]])]]))

(defn location-editor-sidebar-npcs [location-id]
  (let [available-npcs (<sub [:available-npcs location-id])
        dnd-entity (:entity (<sub [:dnd-payload]))]
    [slds/label "Available NPCs"
     [:ul {:class "tile-list"}
      (for [[_ {character-id :id :keys [display-name texture]}] available-npcs]
        [:li {:key (str "character-select" display-name)
              :class "tile-list__item"
              :draggable true
              :on-drag-start (fn [e]
                               (set-drag-texture! e texture)
                               (>evt [:start-entity-drag {:entity character-id}]))}
         [:span {:class "tile-list__item__image"
                 :style {:width (str config/tile-size "px")
                         :height (str config/tile-size "px")}}
           [:img {:title display-name :src (texture-path texture)}]]
         [:span {:class "tile-list__item__label"} display-name]])
      (when (and (some? dnd-entity)
                 (not (contains? available-npcs dnd-entity)))
        [:li {:class "tile-list__item tile-list__item_dropzone"
              :on-drag-over stop-e!
              :on-drop #(>evt [:remove-entity location-id dnd-entity])}
         [:span {:class "tile-list__item__image"
                 :style {:width (str config/tile-size "px")
                         :height (str config/tile-size "px")}}
          [icon "trash" "Drop here to remove."]]
         [:span {:class "tile-list__item__label"} "Drop here to remove."]])]]))

(defn location-editor-sidebar-connections [id]
  [slds/label "Available NPCs"
   [:ul {:class "tile-list"}
    (for [[_ {target-id :id :keys [display-name]}] (<sub [:connected-locations id])]
      [:li {:key (str "connection-select" display-name)
            :class "tile-list__item"
            :draggable true
            :on-drag-start (fn [e]
                             (set-drag-texture! e :marker)
                             (>evt [:start-entity-drag {:connection-trigger target-id}]))}
       [:img {:title display-name :src (texture-path :marker)}]
       [:span display-name]])]])

(defn location-editor-sidebar [{tool :tool} {:keys [id display-name]}]
  (letfn [(update-display-name [e]
            (>evt [:update-location id :display-name (e->val e)]))]
    [slds/form
     [slds/input-text {:label "Name"
                       :on-change update-display-name
                       :value display-name}]
     [slds/radio-button-group {:label "Tools"
                               :options [[:paint [icon "layer-group" "Background"]]
                                         [:collision [icon "walking" "Collision"]]
                                         [:select [icon "user" "NPCs"]]
                                         [:connections [icon "external-link-alt" "Connections"]]]
                               :active tool
                               :on-change #(>evt [:set-tool %])}]
     (case tool
       :paint [location-editor-sidebar-paint]
       :select [location-editor-sidebar-npcs id]
       :connections [location-editor-sidebar-connections id]
       nil)]))

(defn location-editor-content [{:keys [highlight tool painting?]} {:keys [id level npcs walk-set connection-triggers]}]
  (let [level-width (count level)
        level-height (count (first level))
        dnd-payload (<sub [:dnd-payload])]
    [:div {:class "level"
           :on-mouse-leave #(>evt [:unset-highlight])
           :style {:width (str (* config/tile-size level-width) "px")
                   :height (str (* config/tile-size level-height) "px")}}
     (for [x (range level-width)
           y (range level-height)
           :let [tile [x y]
                 texture (get-in level tile)]]
       [:div (merge {:key (str "location" id ":" tile)
                     :class "level__tile"
                     :style {:width (str config/tile-size "px")
                             :height (str config/tile-size "px")}}
                    (case tool
                      :select
                      (when-let [entity (:entity dnd-payload)]
                        {:on-drag-over (e-> (once #(>evt [:set-highlight tile])))
                         :on-drop #(>evt [:move-entity id entity tile])})
                      :paint
                      {:on-mouse-down (e-> #(>evt [:start-painting id tile]))
                       :on-mouse-over (e-> #(when painting? (>evt [:paint id tile])))
                       :on-mouse-up (e-> #(when painting? (>evt [:stop-painting])))}
                      :collision
                      {:on-click #(>evt [:flip-walkable id tile])}
                      :connections
                      (when-let [target (:connection-trigger dnd-payload)]
                        {:on-drag-over (e-> (once #(>evt [:set-highlight tile])))
                         :on-drop #(>evt [:move-trigger id target tile])})))
        [:img {:class "background"
               :src (texture-path texture)}]
        (when-let [{character-id :id npc-texture :texture display-name :display-name} (get npcs tile)]
          [:img {:src (texture-path npc-texture)
                 :title display-name
                 :draggable true
                 :on-drag-start (fn [e]
                                  (set-drag-texture! e npc-texture)
                                  (>evt [:start-entity-drag {:entity character-id}]))}])
        (when-let [{connected-location :id display-name :display-name} (get connection-triggers tile)]
          [:img {:src (texture-path :marker)
                 :title (str "to " display-name)
                 :draggable true
                 :on-drag-start (fn [e]
                                  (set-drag-texture! e :marker)
                                  (>evt [:start-entity-drag {:connection-trigger connected-location}]))}])
        (case tool
          :collision
          [:div {:class ["highlight" (if (contains? walk-set tile)
                                       "highlight_walkable"
                                       "highlight_not-walkable")]}]
          (:select :connections)
          (when (= tile highlight)
            [:div {:class "highlight"}])
          nil)])]))

(defn location-editor [location-id]
  (let [location (<sub [:location location-id])
        editor-options (<sub [:location-editor-data])]
    [:div {:class "location-editor"
           :on-drag-end #(>evt [:stop-entity-drag])}
     [:div {:class "location-editor__sidebar"}
      [location-editor-sidebar editor-options location]]
     [:div {:class "location-editor__content"}
      [location-editor-content editor-options location]]]))
