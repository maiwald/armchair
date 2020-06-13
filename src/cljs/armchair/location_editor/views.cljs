(ns armchair.location-editor.views
  (:require [reagent.core :as r]
            [armchair.slds :as slds]
            [armchair.input :as input]
            [armchair.components :as c]
            [armchair.components.tile-map :refer [tile-dropzone]]
            [armchair.config :as config]
            [armchair.math :refer [Point
                                   Rect
                                   relative-point
                                   global-point
                                   rect->point-seq
                                   rect-contains?]]
            [armchair.util :as u :refer [px <sub >evt e-> e->val]]
            [armchair.textures :refer [image-path]]))

(defn tile-paint-canvas []
  (let [painted-tiles (r/atom nil)
        current-tile (r/atom nil)]
    (letfn [(set-current-tile [e] (reset! current-tile (u/e->tile e)))
            (clear-current-tile [] (reset! current-tile nil))
            (start-painting [] (reset! painted-tiles #{}))
            (stop-painting [] (reset! painted-tiles nil))
            (make-paint [paint-fn]
              (fn [e]
                (let [tile (u/e->tile e)]
                  (when (and (some? @painted-tiles)
                             (not (contains? @painted-tiles tile)))
                    (swap! painted-tiles conj tile)
                    (paint-fn tile)))))]
      (fn [{:keys [on-paint texture]}]
        (let [paint (make-paint on-paint)]
          [:div {:class "level__layer"
                 :on-mouse-enter set-current-tile
                 :on-mouse-leave clear-current-tile
                 :on-mouse-down (fn [e]
                                  (when (u/e->left? e)
                                    (start-painting)
                                    (paint e)))
                 :on-mouse-move (fn [e]
                                  (set-current-tile e)
                                  (paint e))
                 :on-mouse-up stop-painting}
           (if-let [tile @current-tile]
             (if (some? texture)
               [:div {:style (merge {:position "absolute"
                                     :opacity ".8"}
                                    (u/tile-style tile))}
                [c/sprite-texture texture]]
               [:div {:class "interactor interactor_paint"
                      :style (u/tile-style tile)}]))])))))

(defn sidebar-widget [{title :title}]
  (into [:div {:class "location-editor__sidebar-widget"}
         [:div {:class "location-editor__sidebar-widget__title"} title]]
        (r/children (r/current-component))))

(defn sidebar-layers []
  (let [{:keys [active-layer visible-layers]} (<sub [:location-editor/ui])]
    [sidebar-widget {:title "Layers"}
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
            [c/icon "eye-slash" "Show layer"])]])]]))

(defn sidebar-tool []
  (let [{:keys [active-tool]} (<sub [:location-editor/ui])]
    [sidebar-widget {:title "Tool"}
     [slds/radio-button-group
      {:options [[:brush [c/icon "paint-brush" "Paint"]]
                 [:eraser [c/icon "eraser" "Erase"]]]
       :on-change #(>evt [:location-editor/set-active-tool %])
       :active active-tool}]]))

(defn sidebar-info [location-id]
  (let [display-name (:display-name (<sub [:location-editor/location-inspector location-id]))]
    [sidebar-widget {:title "Location Name"}
     [input/text
      {:on-change #(>evt [:location-editor/update-name location-id (e->val %)])
       :value display-name}]]))

(defn sidebar-resize [location-id]
  (let [{:keys [width height]} (<sub [:location-editor/dimensions location-id])]
    [sidebar-widget {:title (str "Size: " width " x " height)}
     [:div {:class "resize-container"}
      [:div {:class "resize-container__reference"}
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
        [:a {:on-click #(>evt [:location-editor/resize-larger location-id :right])} [c/icon "arrow-right" "extend"]]]]]]))

(defn sidebar-texture-select []
  (let [{:keys [active-texture]} (<sub [:location-editor/ui])]
    [sidebar-widget {:title "Active Texture"}
     [:a {:class "active-texture"
          :on-click (e-> #(>evt [:armchair.modals.texture-selection/open active-texture]))}
      [c/sprite-texture active-texture]]]))

(defn sidebar-collision []
  (let [{:keys [active-walk-state]} (<sub [:location-editor/ui])]
    [sidebar-widget {:title "Collision State"}
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
              :style {:height (px config/tile-size)
                      :width (px config/tile-size)
                      :background-color (if walk-state
                                          "rgba(0, 255, 0, .4)"
                                          "rgba(255, 0, 0, .4)")}}]])]]))

(defn sidebar-player []
  [sidebar-widget
   [:ul {:class "tile-list"}
    [:li {:class "tile-list__item"
          :draggable true
          :on-drag-end #(>evt [:stop-entity-drag])
          :on-drag-start (fn [e]
                           (.setData (.-dataTransfer e) "text/plain" ":player")
                           (>evt [:start-entity-drag [:player]]))}
     [:span {:class "tile-list__item__image"
             :style {:width (str config/tile-size "px")
                     :height (str config/tile-size "px")}}
      [c/sprite-texture ["hare.png" (Point. 6 0)] "Player"]]
     [:span {:class "tile-list__item__label"} "Place Player"]]]])

(defn sidebar-triggers []
  [sidebar-widget
   [:ul {:class "tile-list"}
    [:li {:class "tile-list__item"
          :draggable true
          :on-drag-end #(>evt [:stop-entity-drag])
          :on-drag-start (fn [e]
                           (.setData (.-dataTransfer e) "text/plain" ":exit")
                           (>evt [:start-entity-drag [:connection-trigger]]))}
     [:span {:class "tile-list__item__image"
             :style {:width (str config/tile-size "px")
                     :height (str config/tile-size "px")}}
      [:img {:src (image-path "exit.png")
             :title "Exit"}]]
     [:span {:class "tile-list__item__label"} "Place new Exit"]]]])


(defn sidebar-npcs []
  (let [available-characters (<sub [:location-editor/available-characters])]
    [sidebar-widget {:title "Available Characters"}
     [:ul {:class "tile-list"}
      (for [{:keys [character-id display-name texture]} available-characters]
        [:li {:key (str "character-select" display-name)
              :class "tile-list__item"
              :draggable true
              :on-drag-end #(>evt [:stop-entity-drag])
              :on-drag-start (fn [e]
                               (.setData (.-dataTransfer e) "text/plain" display-name)
                               (>evt [:start-entity-drag [:character character-id]]))}
         [:span {:class "tile-list__item__image"
                 :style {:width (str config/tile-size "px")
                         :height (str config/tile-size "px")}}
          [c/sprite-texture texture display-name]]
         [:span {:class "tile-list__item__label"} display-name]])]
     (when (empty? available-characters) "There are no characters.")
     [c/button {:title "Create Character"
                :icon "plus"
                :fill true
                :on-click #(>evt [:armchair.modals.character-form/open])}]]))

(defn sidebar [location-id]
  (let [{:keys [active-pane active-layer]} (<sub [:location-editor/ui])]
    [:<>
     [c/tabs {:items [[:info "Info"]
                      [:level "Level"]]
              :active active-pane
              :on-change #(>evt [:location-editor/set-active-pane %])}]
     (case active-pane
       :info [:<>
              [sidebar-info location-id]
              [sidebar-resize location-id]]
       :level [:<>
               [sidebar-layers]
               (case active-layer
                 (:background1 :background2 :foreground1 :foreground2)
                 [:<>
                  [sidebar-texture-select]
                  [sidebar-tool]]

                 :collision
                 [sidebar-collision]

                 :triggers
                 [sidebar-triggers]

                 :entities
                 [:<>
                  [sidebar-player]
                  [sidebar-npcs]])])]))

(defn do-all-tiles [rect layer-title f]
  [:<>
   (for [tile (rect->point-seq rect)
         :let [tile-data (f tile)]
         :when (some? tile-data)]
     [:div {:key (str "location-cell:" layer-title ":" (pr-str tile))
            :class "level__tile"
            :style (u/tile-style (global-point tile rect))}
      tile-data])])

(defn do-some-tiles [rect coll layer-title f]
  [:<>
   (for [[tile item] coll
         :when (rect-contains? rect tile)]
     [:div {:key (str "location-cell:" layer-title ":" (pr-str tile))
            :class "level__tile"
            :style (u/tile-style (global-point tile rect))}
      (f tile item)])])

(defn texture-layer [{:keys [location-id layer-id override-rect]}]
  (let [location (<sub [:location-editor/location location-id])
        rect (or override-rect (:bounds location))
        layer (get location layer-id)]
    [do-all-tiles rect (str "texture-layer:" layer-id)
     (fn [tile]
       (if-let [t (get layer tile)]
         [c/sprite-texture t]))]))

(defn player-tile [rect position]
  (when (rect-contains? rect position)
    [:div {:key (str "location-cell:player:" position)
           :class "level__tile"
           :style (u/tile-style (global-point position rect))}
     [c/sprite-texture ["hare.png" (Point. 6 0)] "Player"]]))

(defn entity-layer [location-id override-rect]
  (let [{:keys [player-position
                characters
                bounds]} (<sub [:location-editor/entity-layer location-id])
        rect (or override-rect bounds)]
    [:<>
     [do-some-tiles rect characters "npc"
      (fn [_tile {:keys [display-name texture]}]
        [c/sprite-texture texture display-name])]
     (when (some? player-position)
       [player-tile rect player-position])]))

(defn conntection-trigger-layer [location-id override-rect]
  (let [{:keys [bounds
                connection-triggers]} (<sub [:location-editor/connection-trigger-layer location-id])
        rect (or override-rect bounds)]
    [do-some-tiles rect connection-triggers "connection-trigger"
     (fn [_tile {:keys [display-name]}]
       [:img {:src (image-path "exit.png")
              :title (str "to " display-name)}])]))

(defn collision-layer [rect blocked]
  [do-all-tiles rect "collision"
   (fn [tile]
     [:div {:class ["interactor"
                    (if (contains? blocked tile)
                      "interactor_not-walkable"
                      "interactor_walkable")]}])])

(defn location-preview [location-id preview-tile]
  (let [tiles-around 3
        bounds (Rect. (- (:x preview-tile) tiles-around)
                      (- (:y preview-tile) tiles-around)
                      (inc (* 2 tiles-around))
                      (inc (* 2 tiles-around)))]
    [:div {:class "level"
           :style {:width (u/px (* config/tile-size (:w bounds)))
                   :height (u/px (* config/tile-size (:h bounds)))}}
     [texture-layer {:location-id location-id
                     :layer-id :background1
                     :override-rect bounds}]
     [texture-layer {:location-id location-id
                     :layer-id :background2
                     :override-rect bounds}]
     [texture-layer {:location-id location-id
                     :layer-id :foreground1
                     :override-rect bounds}]
     [texture-layer {:location-id location-id
                     :layer-id :foreground2
                     :override-rect bounds}]
     [entity-layer location-id bounds]
     [conntection-trigger-layer location-id bounds]
     [:div {:key "location-cell:highlight"
            :class "level__tile level__tile_highlight"
            :style (u/tile-style (global-point preview-tile bounds))}]]))

(defn tile-select [{:keys [bounds on-select selected selectable?]}]
  (letfn [(on-click [e]
            (let [tile (relative-point (u/e->tile e) bounds)]
              (when (or (not (fn? selectable?))
                        (selectable? tile))
                (on-select tile))))]
    [:div {:style {:position "absolute"
                   :top 0
                   :left 0
                   :width (u/px (* config/tile-size (:w bounds)))
                   :height (u/px (* config/tile-size (:h bounds)))}}
     (when selected
       [:div {:key "location-cell:selected"
              :class "level__tile level__tile_highlight"
              :style (u/tile-style (global-point selected bounds))}])
     (when (fn? selectable?)
       [do-all-tiles bounds "disabled-selectors"
        (fn [tile]
          (if-not (selectable? tile)
            [:div {:class ["interactor" "interactor_disabled"]}]))])
     [:div {:class "level__layer"
            :on-click on-click}]]))

(defn position-select [location-id on-select selected]
  (let [{:keys [bounds]} (<sub [:location-editor/location location-id])
        occupied (<sub [:location-editor/physically-occupied-tiles location-id])]
    [:div {:style {:overflow "scroll"
                   :background-color "#000"
                   :max-width (u/px 600)
                   :max-height (u/px 400)}}
     [:div {:class "level"
            :style {:width (u/px (* config/tile-size (:w bounds)))
                    :height (u/px (* config/tile-size (:h bounds)))
                    :margin "auto"}}
      [texture-layer {:location-id location-id
                      :layer-id :background1}]
      [texture-layer {:location-id location-id
                      :layer-id :background2}]
      [texture-layer {:location-id location-id
                      :layer-id :foreground1}]
      [texture-layer {:location-id location-id
                      :layer-id :foreground2}]
      [entity-layer location-id]
      [conntection-trigger-layer location-id]
      [tile-select {:bounds bounds
                    :on-select on-select
                    :selected selected
                    :selectable? (fn [tile] (not (contains? occupied tile)))}]]]))

(defn property [{title :title}]
  [:div.inspector__property
   [:span.inspector__property__title title]
   (into [:div.inspector__property__payload]
         (r/children (r/current-component)))])

(defn placement-inspector [location-id tile]
  (let [{:keys [location-display-name
                location-tile
                character-id
                character-display-name
                dialogue-id
                dialogue-options]}
        (<sub [:location-editor/placement-inspector location-id tile])]
    (letfn [(set-character [e]
              (>evt [:location-editor/set-placement-character
                     location-id tile (-> e e->val uuid)]))
            (set-dialogue [e]
              (>evt [:location-editor/set-placement-dialogue
                     location-id tile (uuid (e->val e))]))
            (unset-dialogue []
              (>evt [:location-editor/set-placement-dialogue
                     location-id tile nil]))]
      [:div#inspector
       [:header
        [:span.title "Character"]
        [:a.close-button {:on-click #(>evt [:close-inspector])}
         [c/icon "times"]]]
       [:div.inspector__content
        [property {:title "Placement"}
         (str
           location-display-name
           " [" (:x location-tile) "," (:y location-tile) "]")]
        [property {:title "Character"}
         [:a {:on-click #(>evt [:armchair.modals.character-form/open character-id])}
          character-display-name]]
        [property {:title "Dialogue"}
         [input/select {:value dialogue-id
                        :nil-value "No dialogue"
                        :options dialogue-options
                        :on-change set-dialogue}]
         [c/button {:title "Create a new dialogue"
                    :icon "plus"
                    :fill true
                    :on-click #(>evt [:armchair.modals.dialogue-creation/open character-id location-id tile])}]]]
       [:div.inspector__actions
        [c/button {:title "Clear tile"
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
    [:div#inspector
     [:header
      [:span.title "Exit"]
      [:a.close-button {:on-click #(>evt [:close-inspector])}
       [c/icon "times"]]]
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

(defn tile-inspector [location-id tile]
  (case (<sub [:location-editor/tile-type location-id tile])
    :placement [placement-inspector location-id tile]
    :trigger [trigger-inspector location-id tile]
    nil))

(defn edit-entity-layer [location-id]
  (let [{:keys [player-position
                characters
                bounds]} (<sub [:location-editor/entity-layer location-id])]
    [:<>
     (when player-position
       [do-some-tiles bounds {player-position :player} "player-select"
        (fn [_ _]
          [:div {:class "interactor interactor_draggable"
                 :title "Player"
                 :draggable true
                 :on-drag-start (fn [e]
                                  (.setData (.-dataTransfer e) "text/plain" ":player")
                                  (>evt [:start-entity-drag [:player]]))
                 :on-drag-end #(>evt [:stop-entity-drag])}])])

     [do-some-tiles bounds characters "character-select"
      (fn [tile {:keys [display-name inspecting?]}]
        [:div {:class ["interactor" "interactor_draggable" (when inspecting? "interactor_focus")]
               :title display-name
               :draggable true
               :on-click #(>evt [:inspect :tile location-id tile])
               :on-drag-start (fn [e]
                                (.setData (.-dataTransfer e) "text/plain" display-name)
                                (>evt [:start-entity-drag [:placement tile]]))
               :on-drag-end #(>evt [:stop-entity-drag])}])]]))

(defn edit-trigger-layer [location-id]
  (let [{:keys [bounds
                connection-triggers]} (<sub [:location-editor/connection-trigger-layer location-id])]
    [do-some-tiles bounds connection-triggers "connection-select"
     (fn [tile {:keys [display-name inspecting?]}]
       [:div {:class ["interactor" "interactor_draggable" (when inspecting? "interactor_focus")]
              :title (str "to " display-name)
              :draggable true
              :on-click #(>evt [:inspect :tile location-id tile])
              :on-drag-start (fn [e]
                               (.setData (.-dataTransfer e) "text/plain" display-name)
                               (>evt [:start-entity-drag [:connection-trigger tile]]))
              :on-drag-end #(>evt [:stop-entity-drag])}])]))

(defn canvas [location-id]
  (let [{:keys [bounds blocked]} (<sub [:location-editor/location location-id])
        {:keys [active-pane
                active-layer
                visible-layers
                active-tool
                active-texture]} (<sub [:location-editor/ui])
        dropzone-fn (if-let [[dnd-type dnd-payload] (<sub [:ui/dnd])]
                      (case dnd-type
                        :character          #(>evt [:location-editor/place-character location-id dnd-payload (relative-point % bounds)])
                        :player             #(>evt [:location-editor/move-player location-id (relative-point % bounds)])
                        :placement          #(>evt [:location-editor/move-placement location-id dnd-payload (relative-point % bounds)])
                        :connection-trigger #(>evt [:location-editor/move-trigger location-id dnd-payload (relative-point % bounds)])))]
    [:div {:class "level-wrap"}
     [:div {:class "level"
            :style {:width (u/px (* config/tile-size (:w bounds)))
                    :height (u/px (* config/tile-size (:h bounds)))}}

      (for [[layer-id] (reverse config/location-editor-layers)
            :when (contains? visible-layers layer-id)]
        (case layer-id
          :entities
          ^{:key "layer::entities"}
          [entity-layer location-id]

          :collision
          ^{:key "layer::collision"}
          [collision-layer bounds blocked]

          :triggers
          ^{:key "layer::triggers"}
          [conntection-trigger-layer location-id]

          (:background1 :background2 :foreground1 :foreground2)
          ^{:key (str "layer:" layer-id)}
          [texture-layer {:location-id location-id
                          :layer-id layer-id}]))

      (case active-pane
        :level
        (case active-layer
          (:background1 :background2 :foreground1 :foreground2)
          [tile-paint-canvas
           {:texture (when (not= active-tool :eraser) active-texture)
            :on-paint #(>evt [:location-editor/paint location-id active-layer
                              (relative-point % bounds)])}]

          :collision
          [tile-paint-canvas
           {:on-paint #(>evt [:location-editor/set-walkable location-id
                              (relative-point % bounds)])}]

          :entities
          [edit-entity-layer location-id]

          :triggers
          [edit-trigger-layer location-id])

        [:<>
         [edit-entity-layer location-id]
         [edit-trigger-layer location-id]])

      (when (fn? dropzone-fn)
        [tile-dropzone {:occupied (<sub [:location/occupied-tiles location-id])
                        :on-drop dropzone-fn}])]]))

(defn location-editor [location-id]
  (if (<sub [:location-editor/location-exists? location-id])
    [:div {:class "location-editor"}
     [:div {:class "location-editor__sidebar"}
      [sidebar location-id]]
     [:div {:class "location-editor__canvas"}
      [canvas location-id]]]
    "Location not found."))
