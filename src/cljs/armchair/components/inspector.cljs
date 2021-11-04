(ns armchair.components.inspector
  (:require [reagent.core :as r]
            [re-frame.core :refer [reg-sub subscribe]]
            [armchair.events :refer [reg-event-meta]]
            [armchair.config :as config]
            [armchair.util :as u :refer [<sub >evt e->val e->]]
            [armchair.components :as c]
            [armchair.sprites :refer [Sprite]]
            [armchair.routes :refer [>navigate]]
            [armchair.math :refer [global-point]]
            [armchair.input :as input]
            [armchair.location-editor.views :refer [location-preview]]))



;; Events

(reg-event-meta
  :inspect
  (fn [db [_ inspector-type & inspector-data]]
    (assoc db :ui/inspector
           (apply vector inspector-type inspector-data))))

(reg-event-meta
  :close-inspector
  (fn [db] (dissoc db :ui/inspector)))

;; Subscriptions

(reg-sub
  ::placement-inspector
  (fn [[_ location-id]]
    [(subscribe [:db/location location-id])
     (subscribe [:db-dialogues])
     (subscribe [:db-characters])])
  (fn [[location dialogues characters] [_ _location-id tile]]
    (let [{:keys [character-id dialogue-id]} (get-in location [:placements tile])
          dialogue-options (->> dialogues
                                (u/filter-map #(= character-id (:character-id %)))
                                (u/map-values :synopsis))]
      {:location-display-name (:display-name location)
       :location-tile (global-point tile (:bounds location))
       :character-id character-id
       :character-display-name (get-in characters [character-id :display-name])
       :dialogue-id dialogue-id
       :dialogue-display-name (get-in dialogues [dialogue-id :synopsis])
       :dialogue-options dialogue-options})))

(reg-sub
  ::location-inspector
  :<- [:db-locations]
  :<- [:db-characters]
  (fn [[locations characters] [_ location-id]]
    (let [location (locations location-id)]
      {:display-name (:display-name location)
       :characters (->> location :placements
                        (map (fn [[_ {:keys [character-id]}]]
                               (let [character (get characters character-id)]
                                 {:character-id character-id
                                  :character-name (:display-name character)
                                  :character-color (:color character)})))
                        distinct
                        (sort-by :character-name))})))

(reg-sub
  ::world-inspector
  (fn [{:keys [locations characters dialogues]}]
    {:locations (count locations)
     :characters (count characters)
     :dialogues (count dialogues)}))

;; Views

(defn property [{title :title inline? :inline}]
  [:div.inspector__property {:class (when inline? "inspector__property_inline")}
   [:span.inspector__property__title title]
   (into [:div.inspector__property__payload]
         (r/children (r/current-component)))])

(defn placement-inspector [location-id tile]
  (let [{:keys [location-display-name
                location-tile
                character-id
                character-display-name
                dialogue-id
                dialogue-display-name
                dialogue-options]}
        (<sub [::placement-inspector location-id tile])]
    (letfn [(set-character [e]
              (>evt [:location-editor/set-placement-character
                     location-id tile (-> e e->val uuid)]))
            (set-dialogue [e]
              (>evt [:location-editor/set-placement-dialogue
                     location-id tile (uuid (e->val e))]))
            (unset-dialogue []
              (>evt [:location-editor/set-placement-dialogue
                     location-id tile nil]))]
      [:<>
       [:header
        [:span.title "Character"]
        [c/icon-button {:icon "times"
                        :title "Close Inspector"
                        :on-click #(>evt [:close-inspector])}]]
       [:div.inspector__content
        [property {:title "Placement"}
         (str
           location-display-name
           " [" (:x location-tile) "," (:y location-tile) "]")]
        [property {:title "Character"}
         [:a {:on-click #(>evt [:armchair.modals.character-form/open character-id])}
          character-display-name]]
        [property {:title "Dialogue"}
         (if (some? dialogue-id)
           [:div.insprop_dialogue
            [:a.insprop_dialogue__title
             {:on-click #(>navigate :dialogue-edit :id dialogue-id)}
             dialogue-display-name]
            [:span.insprop_dialogue__remove
              [c/icon-button {:icon "times"
                              :title "Remove Dialogue"
                              :on-click unset-dialogue}]]]
           [:<>
             [input/select {:value dialogue-id
                            :nil-value "No dialogue"
                            :options dialogue-options
                            :on-change set-dialogue}]
             [c/button {:title "Create a new dialogue"
                        :icon "plus"
                        :fill true
                        :on-click #(>evt [:armchair.modals.dialogue-creation/open character-id location-id tile])}]])]]
       [:div.inspector__actions
        [c/button {:title "Remove Character"
                   :type :danger
                   :fill true
                   :on-click #(>evt [:location-editor/remove-placement location-id tile])}]]])))

(defn trigger-inspector [location-id tile]
  (let [{:keys [source-display-name
                source-position-normalized
                target-id
                target-display-name
                target-position
                target-position-normalized]}
        (<sub [:location-editor/trigger-inspector location-id tile])]
    [:<>
     [:header
      [:span.title "Exit"]
      [c/icon-button {:icon "times"
                      :title "Close Inspector"
                      :on-click #(>evt [:close-inspector])}]]
     [:div.inspector__content
      [property {:title "From"}
       (str
         source-display-name
         " [" (:x source-position-normalized)
         "," (:y source-position-normalized) "]")]
      [property {:title "To"}
       (str
         target-display-name
         " [" (:x target-position-normalized)
         "," (:y target-position-normalized) "]")]
      [property {:title "Preview"}
       [:div {:style {:margin "5px auto 0"}}
        [location-preview target-id target-position]]]]
     [:div.inspector__actions
      [c/button {:title "Remove Exit"
                 :type :danger
                 :fill true
                 :on-click #(>evt [:location-editor/remove-trigger location-id tile])}]]]))


(defn location-inspector [location-id]
  (let [{:keys [display-name characters]} (<sub [::location-inspector location-id])]
    [:<>
     [:header
      [:span.title "Location"]
      [c/icon-button {:icon "times"
                      :title "Close Inspector"
                      :on-click #(>evt [:close-inspector])}]]
     [:div.inspector__content
      [property {:title "Name"}
       [input/text
        {:on-change #(>evt [:location-editor/update-name location-id (e->val %)])
         :value display-name}]]]
     [:div.inspector__actions
      [c/button {:title "Edit Tilemap"
                 :icon "map"
                 :fill true
                 :on-click #(>navigate :location-edit :id location-id)}]]
     [:div.inspector__content
      [property {:title "Characters"}
       (for [{:keys [character-id character-name]} characters]
         [:p {:key (str location-id ":" character-id)}
          character-name])]]
     [:div.inspector__actions
      [c/button {:title "Delete Location"
                 :type :danger
                 :fill true
                 :on-click #(>evt [:delete-location location-id])}]]]))

(defn tile-inspector [location-id tile]
  (case (<sub [:location-editor/tile-type location-id tile])
    :placement [placement-inspector location-id tile]
    :trigger [trigger-inspector location-id tile]
    nil))


(defn tilemap-inspector [location-id]
  (let [{:keys [active-layer active-sprite active-tool active-walk-state visible-layers]} (<sub [:location-editor/ui])
        {:keys [width height]} (<sub [:location-editor/dimensions location-id])]
    [:div.inspector__content
     [property {:title (str "Size: " width " x " height)}
      [:div {:class "insprop_resize-container"}
       [:div {:class "insprop_resize-container__reference"}
        [:div {:class "resizer resizer_horizontal resizer_top"}
         [:a {:on-click #(>evt [:location-editor/resize-larger location-id :up])} [c/icon "arrow-up" "extend"]]
         [:a {:on-click #(>evt [:location-editor/resize-smaller location-id :up])} [c/icon "arrow-down" "shrink"]]]
        [:div {:class "resizer resizer_horizontal resizer_bottom"}
         [:a {:on-click #(>evt [:location-editor/resize-smaller location-id :down])} [c/icon "arrow-up" "shrink"]]
         [:a {:on-click #(>evt [:location-editor/resize-larger location-id :down])} [c/icon "arrow-down" "extend"]]]
        [:div {:class "resizer resizer_vertical resizer_left"}
         [:a {:on-click #(>evt [:location-editor/resize-larger location-id :left])} [c/icon "arrow-left" "extend"]]
         [:a {:on-click #(>evt [:location-editor/resize-smaller location-id :left])} [c/icon "arrow-right" "shrink"]]]
        [:div {:class "resizer resizer_vertical resizer_right"}
         [:a {:on-click #(>evt [:location-editor/resize-smaller location-id :right])} [c/icon "arrow-left" "shrink"]]
         [:a {:on-click #(>evt [:location-editor/resize-larger location-id :right])} [c/icon "arrow-right" "extend"]]]]]]
     [property {:title "Layers"}
      [:ol.level-layers
       (for [[layer-id layer-name] config/location-editor-layers
             :let [visible? (contains? visible-layers layer-id)]]
         [:li {:key (str "layer" layer-id)
               :class ["level-layers__item"
                       (when (= active-layer layer-id) "level-layers__item_active")]}
          [:span.level-layers__item__name
           {:on-click #(>evt [:location-editor/set-active-layer layer-id])}
           layer-name]
          [:span
           {:class ["level-layers__item__visibility"
                    (str "level-layers__item__visibility_"
                         (if visible? "visible" "not-visible"))]
            :on-click #(>evt [:location-editor/toggle-layer-visibility layer-id])}
           (if visible?
             [c/icon "eye" "Hide layer"]
             [c/icon "eye-slash" "Show layer"])]])]]
     (case active-layer
       (:background1 :background2 :foreground1 :foreground2)
       [:<>
        [property {:title "Active Texture"}
         [:a {:class "insprop_active-sprite"
              :on-click (e-> #(>evt [:armchair.modals.sprite-selection/open active-sprite]))}
          [Sprite active-sprite]]]
        [property {:title "Tool"}
         [c/button {:icon "paint-brush"
                    :title "Paint"
                    :active (= :brush active-tool)
                    :on-click #(>evt [:location-editor/set-active-tool :brush])}]
         [c/button {:icon "eraser"
                    :title "Erase"
                    :active (= :eraser active-tool)
                    :on-click #(>evt [:location-editor/set-active-tool :eraser])}]]]
       :collision
       [property {:title "Collision State"}
        [:ul {:class "tile-grid"}
         (for [walk-state (list true false)]
           [:li {:key (str "walk-state-select:" walk-state)
                 :title (if walk-state "walkable" "not walkable")
                 :class ["tile-grid__item"
                         (when (= walk-state active-walk-state) "tile-grid__item_active")]
                 :style {:width (u/px config/tile-size)
                         :height (u/px config/tile-size)
                         :background-color "#fff"}}
            [:a {:on-click #(>evt [:location-editor/set-active-walk-state walk-state])
                 :style {:height (u/px config/tile-size)
                         :width (u/px config/tile-size)
                         :background-color (if walk-state
                                             "rgba(0, 255, 0, .4)"
                                             "rgba(255, 0, 0, .4)")}}]])]]
       nil)]))

(defn dialogue-inspector [dialogue-id]
  (let [{:keys [synopsis]} (<sub [:db/dialogue dialogue-id])]
    [:div.inspector__content
      [property {:title "Synopsis"} synopsis]]))

(defn world-inspector []
  (let [{:keys [locations characters dialogues]} (<sub [::world-inspector])]
    [:div.inspector__content
     [property {:title "Locations" :inline true} locations]
     [property {:title "Characters" :inline true} characters]
     [property {:title "Dialogues" :inline true} dialogues]]))

(defn inspector [page-name page-params]
  (let [[inspector-type & inspector-data] (<sub [:ui/inspector])
        page-inspector (case page-name
                         :locations {:title "World"
                                     :component [world-inspector]}
                         :location-edit {:title "Location Editor"
                                         :component [tilemap-inspector (uuid (:id page-params))]}
                         :dialogue-edit {:title "Dialogue Edit"
                                         :component [dialogue-inspector (uuid (:id page-params))]})]
    [:div#inspector
     [:header
      [:span.title (:title page-inspector)]]
     (case inspector-type
       :location [location-inspector (first inspector-data)]
       :tile [tile-inspector (first inspector-data) (second inspector-data)]
       (:component page-inspector))]))
