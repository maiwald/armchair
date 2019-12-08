(ns armchair.location-editor.views
  (:require [reagent.core :as r]
            [armchair.slds :as slds]
            [armchair.input :as input]
            [armchair.components :as c]
            [armchair.config :as config]
            [armchair.routes :refer [>navigate]]
            [armchair.math :refer [Point
                                   Rect
                                   translate-point
                                   relative-point
                                   global-point
                                   rect->point-seq
                                   rect-contains?]]
            [armchair.util :as u :refer [px <sub >evt e-> e->val]]
            [armchair.modals.dialogue-creation :as dialogue-creation]
            [armchair.textures :refer [texture-path background-textures]]))

(defn dnd-texture [texture]
  [:div.dnd-texture
   [:img {:src (texture-path texture)
          :style {:height (str config/tile-size "px")
                  :width (str config/tile-size "px")
                  :max-width (str config/tile-size "px")
                  :max-height (str config/tile-size "px")}}]])

(defn set-dnd-texture! [e]
  (let [offset (/ config/tile-size 2)
        image (.querySelector (.-currentTarget e) ".dnd-texture img")]
    (.setDragImage (.-dataTransfer e) image offset offset)))

(defn get-tile [e]
  (->> (.-currentTarget e)
       (u/relative-cursor e)
       u/coord->tile))

(defn tile-paint-canvas [{:keys [on-paint dimension]}]
  (let [painted-tiles (r/atom nil)
        current-tile (r/atom nil)]
    (letfn [(set-current-tile [e] (reset! current-tile (get-tile e)))
            (clear-current-tile [] (reset! current-tile nil))
            (start-painting [] (reset! painted-tiles #{}))
            (stop-painting [] (reset! painted-tiles nil))
            (make-paint [paint-fn]
              (fn [e]
                (let [tile (translate-point (get-tile e) (:x dimension) (:y dimension))]
                  (when (and (some? @painted-tiles)
                             (not (contains? @painted-tiles tile)))
                    (swap! painted-tiles conj tile)
                    (paint-fn tile)))))]
      (fn [{:keys [on-paint]}]
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
             (let [{:keys [x y]} (u/tile->coord tile)]
               [:div {:class "interactor interactor_paint"
                      :style {:height (px config/tile-size)
                              :width (px config/tile-size)
                              :top (px y)
                              :left (px x)}}]))])))))

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
  (let [display-name (<sub [:location-editor/display-name location-id])]
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

(defn sidebar-collision [location-id]
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
          :on-drag-start (fn [e]
                           (set-dnd-texture! e)
                           (.setData (.-dataTransfer e) "text/plain" ":player")
                           (>evt [:location-editor/start-entity-drag [:player]]))}
     [dnd-texture :player]
     [:span {:class "tile-list__item__image"
             :style {:width (str config/tile-size "px")
                     :height (str config/tile-size "px")}}
      [c/sprite-texture :hare_down_idle1 "Player"]]
     [:span {:class "tile-list__item__label"} "Place Player"]]]])

(defn sidebar-triggers []
  [sidebar-widget
   [:ul {:class "tile-list"}
    [:li {:class "tile-list__item"
          :draggable true
          :on-drag-start (fn [e]
                           (set-dnd-texture! e)
                           (.setData (.-dataTransfer e) "text/plain" ":exit")
                           (>evt [:location-editor/start-entity-drag [:connection-trigger]]))}
     [dnd-texture :exit]
     [:span {:class "tile-list__item__image"
             :style {:width (str config/tile-size "px")
                     :height (str config/tile-size "px")}}
      [:img {:src (texture-path :exit)
             :title "Exit"}]]
     [:span {:class "tile-list__item__label"} "Place new Exit"]]]])


(defn sidebar-npcs [location-id]
  (let [available-characters (<sub [:location-editor/available-characters])]
    [sidebar-widget {:title "Available Characters"}
     [:ul {:class "tile-list"}
      (for [{:keys [character-id display-name texture]} available-characters]
        [:li {:key (str "character-select" display-name)
              :class "tile-list__item"
              :draggable true
              :on-drag-start (fn [e]
                               (set-dnd-texture! e)
                               (.setData (.-dataTransfer e) "text/plain" display-name)
                               (>evt [:location-editor/start-entity-drag [:character character-id]]))}
         [dnd-texture texture]
         [:span {:class "tile-list__item__image"
                 :style {:width (str config/tile-size "px")
                         :height (str config/tile-size "px")}}
          [c/sprite-texture texture display-name]]
         [:span {:class "tile-list__item__label"} display-name]])]
     (if (empty? available-characters) "There are no characters.")
     [c/button {:title "Create Character"
                :icon "plus"
                :fill true
                :on-click #(>evt [:open-character-modal])}]]))

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
                  [sidebar-npcs location-id]])])]))

(defn tile-style [{:keys [x y]}]
  {:width (str config/tile-size "px")
   :height (str config/tile-size "px")
   :top (* y config/tile-size)
   :left (* x config/tile-size)})

(defn do-all-tiles [rect layer-title f]
  [:<>
   (for [tile (rect->point-seq rect)
         :let [tile-data (f tile)]
         :when (some? tile-data)]
     [:div {:key (str "location-cell:" layer-title ":" (pr-str tile))
            :class "level__tile"
            :style (tile-style (global-point tile rect))}
      tile-data])])

(defn do-some-tiles [rect coll layer-title f]
  [:<>
   (for [[tile item] coll
         :when (rect-contains? rect tile)]
     [:div {:key (str "location-cell:" layer-title ":" (pr-str tile))
            :class "level__tile"
            :style (tile-style (global-point tile rect))}
      (f tile item)])])

(defn dropzone [{:keys [dimension highlight occupied on-drop]}]
  [do-all-tiles dimension "dropzone"
   (fn [tile]
     (let [occupied? (contains? occupied tile)]
       [:div {:class ["interactor"
                      (when (= tile highlight) "interactor_dropzone")
                      (when occupied? "interactor_disabled")]
              :on-drag-over u/prevent-e!
              :on-drag-enter (e-> #(if occupied?
                                     (>evt [:location-editor/unset-highlight])
                                     (>evt [:location-editor/set-highlight tile])))
              :on-drop (when-not occupied? (e-> #(on-drop tile)))}]))])

(defn texture-layer [location-id layer-id override-rect]
  (let [location (<sub [:location-editor/location location-id])
        rect (or override-rect (:dimension location))
        layer (get location layer-id)]
    [do-all-tiles rect (str "texture-layer:" layer-id)
     (fn [tile]
       (if-let [t (get layer tile)]
         [c/sprite-texture t]))]))

(defn player-tile [rect position]
  (when (rect-contains? rect position)
    [:div {:key (str "location-cell:player:" position)
           :class "level__tile"
           :style (tile-style (global-point position rect))}
     [c/sprite-texture :hare_down_idle1 "Player"]]))

(defn entity-layer [location-id override-rect]
  (let [{:keys [player-position
                characters
                dimension]} (<sub [:location-editor/entity-layer location-id])
        rect (or override-rect dimension)]
    [:<>
     [do-some-tiles rect characters "npc"
      (fn [tile {:keys [display-name texture]}]
        [c/sprite-texture texture display-name])]
     (when (some? player-position)
       [player-tile rect player-position])]))

(defn conntection-trigger-layer [location-id override-rect]
  (let [{:keys [dimension
                connection-triggers]} (<sub [:location-editor/connection-trigger-layer location-id])
        rect (or override-rect dimension)]
    [do-some-tiles rect connection-triggers "connection-trigger"
     (fn [tile {:keys [display-name]}]
       [:img {:src (texture-path :exit)
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
        dimension (Rect. (- (:x preview-tile) tiles-around)
                         (- (:y preview-tile) tiles-around)
                         (inc (* 2 tiles-around))
                         (inc (* 2 tiles-around)))]
    [:div {:class "level"
           :style {:width (u/px (* config/tile-size (:w dimension)))
                   :height (u/px (* config/tile-size (:h dimension)))}}
     [texture-layer location-id :background1 dimension]
     [texture-layer location-id :background2 dimension]
     [texture-layer location-id :foreground1 dimension]
     [texture-layer location-id :foreground2 dimension]
     [entity-layer location-id dimension]
     [conntection-trigger-layer location-id dimension]
     [:div {:key "location-cell:highlight"
            :class "level__tile level__tile_highlight"
            :style (tile-style (global-point preview-tile dimension))}]]))

(defn tile-select [{:keys [dimension on-select selected selectable?]}]
  (letfn [(on-click [e]
            (let [tile (relative-point (get-tile e) dimension)]
              (if (or (not (fn? selectable?))
                      (selectable? tile))
                (on-select tile))))]
    [:div {:style {:position "absolute"
                   :top 0
                   :left 0
                   :width (u/px (* config/tile-size (:w dimension)))
                   :height (u/px (* config/tile-size (:h dimension)))}}
     (when selected
       [:div {:key "location-cell:selected"
              :class "level__tile level__tile_highlight"
              :style (tile-style (global-point selected dimension))}])
     (when (fn? selectable?)
       [do-all-tiles dimension "disabled-selectors"
        (fn [tile]
          (if-not (selectable? tile)
            [:div {:class ["interactor" "interactor_disabled"]}]))])
     [:div {:class "level__layer"
            :on-click on-click}]]))

(defn position-select [location-id on-select selected]
  (let [{:keys [dimension]} (<sub [:location-editor/location location-id])
        occupied (<sub [:location-editor/physically-occupied-tiles location-id])]
    [:div {:style {:overflow "scroll"
                   :background-color "#000"
                   :max-width (u/px 600)
                   :max-height (u/px 400)}}
     [:div {:class "level"
            :style {:width (u/px (* config/tile-size (:w dimension)))
                    :height (u/px (* config/tile-size (:h dimension)))
                    :margin "auto"}}
      [texture-layer location-id :background1]
      [texture-layer location-id :background2]
      [texture-layer location-id :foreground1]
      [texture-layer location-id :foreground2]
      [entity-layer location-id]
      [conntection-trigger-layer location-id]
      [tile-select {:dimension dimension
                    :on-select on-select
                    :selected selected
                    :selectable? (fn [tile] (not (contains? occupied tile)))}]]]))

(defn character-popover [location-id tile]
  (letfn [(set-dialogue [e]
            (let [e-value (e->val e)
                  value (if (= e-value "nil") nil (uuid e-value))]
              (>evt [:location-editor/set-placement-dialogue
                     location-id tile value])))]
    (fn [location-id title]
      (let [{:keys [id
                    character-id
                    dialogue-id
                    display-name
                    dialogue-synopsis
                    dialogue-options]}
            (<sub [:location-editor/character-popover location-id tile])]
        [:div {:class "level-popover"}
         [:header display-name]
         [:ul.level-popover__quick-links
          [:li [:a {:on-click #(do (>evt [:close-popover])
                                   (>evt [:open-character-modal id]))}
                [c/icon "user"] "Edit Character"]]
          (if (some? dialogue-id)
            [:li [:a {:on-click #(do (>evt [:close-popover])
                                     (>navigate :dialogue-edit :id dialogue-id))}
                  [c/icon "comments"] "Edit Dialogue"]])]
         [:div.level-popover__reference
          [:span.level-popover__reference__title "Dialogue"]
          [:div.level-popover__reference__payload
           [input/select {:value dialogue-id
                          :nil-value "No Dialogue"
                          :options dialogue-options
                          :on-change set-dialogue}]
           [:a.new-dialogue {:on-click #(do (>evt [:close-popover])
                                            (>evt [::dialogue-creation/open character-id location-id tile]))}
            "Create new Dialogue"]]]
         [c/button {:title "Remove Character"
                    :type :danger
                    :fill true
                    :on-click #(do (>evt [:close-popover])
                                   (>evt [:location-editor/remove-character location-id tile]))}]]))))

(defn trigger-popover [location-id tile]
  (let [{:keys [id display-name position]
         {normalized-x :x normalized-y :y} :position-normalized}
        (<sub [:location-editor/trigger-popover location-id tile])]
    [:div {:class "level-popover"}
     [:header "Trigger"]
     [:div.level-popover__reference
      [:span.level-popover__reference__title "Exit to"]
      [:span.level-popover__reference__payload
       [:a {:on-click #(do (>evt [:close-popover])
                           (>navigate :location-edit :id id))}
        (str display-name " [" normalized-x " " normalized-y "]")]
       [location-preview id position]]]
     [c/button {:title "Remove Exit"
                :type :danger
                :fill true
                :on-click #(do (>evt [:close-popover])
                               (>evt [:location-editor/remove-trigger location-id tile]))}]]))

(defn edit-entity-layer [location-id]
  (let [{:keys [player-position
                characters
                dimension]} (<sub [:location-editor/entity-layer location-id])]
    [:<>
     (when player-position
       [do-some-tiles dimension {player-position :player} "player-select"
        (fn [_ _]
          [:div {:class "interactor interactor_draggable"
                 :title "Player"
                 :draggable true
                 :on-drag-start (fn [e]
                                  (set-dnd-texture! e)
                                  (.setData (.-dataTransfer e) "text/plain" ":player")
                                  (>evt [:location-editor/start-entity-drag [:player]]))}
           [dnd-texture :hare_down_idle1]])])

     [do-some-tiles dimension characters "character-select"
      (fn [tile {:keys [texture display-name]}]
        [:div {:class "interactor interactor_draggable"
               :title display-name
               :draggable true
               :on-drag-start (fn [e]
                                (set-dnd-texture! e)
                                (.setData (.-dataTransfer e) "text/plain" display-name)
                                (>evt [:location-editor/start-entity-drag [:placement tile]]))}
         [c/popover-trigger {:popover [character-popover location-id tile]}]
         [dnd-texture texture]])]]))

(defn edit-trigger-layer [location-id]
  (let [{:keys [dimension
                connection-triggers]} (<sub [:location-editor/connection-trigger-layer location-id])]
    [do-some-tiles dimension connection-triggers "connection-select"
     (fn [tile display-name]
       [:div {:class "interactor interactor_draggable"
              :title (str "to " display-name)
              :draggable true
              :on-drag-start (fn [e]
                               (set-dnd-texture! e)
                               (.setData (.-dataTransfer e) "text/plain" display-name)
                               (>evt [:location-editor/start-entity-drag [:connection-trigger tile]]))}
        [c/popover-trigger {:popover [trigger-popover location-id tile]}]
        [dnd-texture :exit]])]))

(defn canvas [location-id]
  (let [{:keys [dimension blocked]
         :as location} (<sub [:location-editor/location location-id])
        {:keys [active-pane
                active-layer
                visible-layers
                highlight]} (<sub [:location-editor/ui])
        dropzone-fn (if-let [[dnd-type dnd-payload] (<sub [:location-editor/dnd-payload])]
                      (case dnd-type
                        :character          #(>evt [:location-editor/place-character location-id dnd-payload %])
                        :player             #(>evt [:location-editor/move-player location-id %])
                        :placement          #(>evt [:location-editor/move-placement location-id dnd-payload %])
                        :connection-trigger #(>evt [:location-editor/move-trigger location-id dnd-payload %])))]
    [:div {:class "level-wrap"}
     [:div {:class "level"
            :style {:width (u/px (* config/tile-size (:w dimension)))
                    :height (u/px (* config/tile-size (:h dimension)))}}

      (for [[layer-id] (reverse config/location-editor-layers)
            :when (contains? visible-layers layer-id)]
        (case layer-id
          :entities
          ^{:key "layer::entities"}
          [entity-layer location-id]

          :collision
          ^{:key "layer::collision"}
          [collision-layer dimension blocked]

          :triggers
          ^{:key "layer::triggers"}
          [conntection-trigger-layer location-id]

          (:background1 :background2 :foreground1 :foreground2)
          ^{:key (str "layer:" layer-id)}
          [texture-layer location-id layer-id]))

      (when (= :level active-pane)
        (case active-layer
          (:background1 :background2 :foreground1 :foreground2)
          [tile-paint-canvas
           {:dimension dimension
            :on-paint #(>evt [:location-editor/paint location-id active-layer %])}]

          :collision
          [tile-paint-canvas
           {:dimension dimension
            :on-paint #(>evt [:location-editor/set-walkable location-id %])}]

          :entities
          [edit-entity-layer location-id]

          :triggers
          [edit-trigger-layer location-id]))

      (when (fn? dropzone-fn)
        [dropzone {:dimension dimension
                   :highlight highlight
                   :occupied (<sub [:location-editor/occupied-tiles location-id])
                   :on-drop dropzone-fn}])]]))

(defn location-editor [location-id]
  (if (<sub [:location-editor/location-exists? location-id])
    [:div {:class "location-editor"
           :on-drag-end #(>evt [:location-editor/stop-entity-drag])}
     [:div {:class "location-editor__sidebar"}
      [sidebar location-id]]
     [:div {:class "location-editor__canvas"}
      [canvas location-id]]]
    "Location not found."))
