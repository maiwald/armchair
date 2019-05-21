(ns armchair.location-editor.views
  (:require [reagent.core :as r]
            [armchair.slds :as slds]
            [armchair.input :as input]
            [armchair.components :as c]
            [armchair.config :as config]
            [armchair.routes :refer [>navigate]]
            [armchair.util :as u :refer [px <sub >evt e-> e->val]]
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

(defn tile-paint-canvas [{:keys [on-paint]
                          [delta _] :dimension}]
  (let [painted-tiles (r/atom nil)
        current-tile (r/atom nil)]
    (letfn [(get-tile [e] (u/coord->tile (u/relative-cursor e (.-currentTarget e))))
            (set-current-tile [e] (reset! current-tile (get-tile e)))
            (clear-current-tile [] (reset! current-tile nil))
            (start-painting [] (reset! painted-tiles #{}))
            (stop-painting [] (reset! painted-tiles nil))
            (make-paint [paint-fn]
              (fn [e]
                (let [tile (u/translate-point (get-tile e) delta)]
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
           (when-let [tile @current-tile]
             (let [[x y] (u/tile->coord tile)]
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
      (for [[layer-id layer-name] config/location-editor-layers]
        [:li {:key (str "layer" layer-id)
              :class ["level-layers__item"
                      (when (= active-layer layer-id) "level-layers__item_active")]}
         [:span.level-layers__item__name
          {:on-click #(>evt [:location-editor/set-active-layer layer-id])}
          layer-name]
         [:span.level-layers__item__visibility
          {:on-click #(>evt [:location-editor/toggle-layer-visibility layer-id])}
          (if (contains? visible-layers layer-id)
            [c/icon "eye" "Hide layer"]
            [c/icon "eye-slash" "Show layer"])]])]]))

(defn sidebar-info [location-id]
  (let [display-name (<sub [:location-editor/display-name location-id])]
    [sidebar-widget {:title "Location Name"}
     [input/text
      {:on-change #(>evt [:location-editor/update-name location-id (e->val %)])
       :value display-name}]]))

(defn sidebar-resize [location-id]
  (let [{:keys [width height]} (<sub [:location-editor/dimensions location-id])]
    [sidebar-widget {:title (str "Size: " width "x" height)}
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

(defn sidebar-paint [location-id]
  (let [{:keys [active-texture]} (<sub [:location-editor/ui])]
    [sidebar-widget {:title "Background Textures"}
     [:ul {:class "tile-grid"}
      (for [texture background-textures]
        [:li {:key (str "texture-select:" texture)
              :title texture
              :class ["tile-grid__item"
                      (when (= texture active-texture) "tile-grid__item_active")]
              :style {:width (u/px config/tile-size)
                      :height (u/px config/tile-size)}}
         [:a {:on-click #(>evt [:location-editor/set-active-texture texture])}
          [c/sprite-texture texture]]])]]))

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

(defn sidebar-player-and-exit []
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
      [c/sprite-texture :human "Player"]]
     [:span {:class "tile-list__item__label"} "Place Player"]]
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
  (let [available-npcs (<sub [:location-editor/available-npcs location-id])]
    [sidebar-widget {:title "Available Characters"}
     [:ul {:class "tile-list"}
      (for [[character-id {:keys [display-name texture]}] available-npcs]
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
     (if (empty? available-npcs) "All Characters are placed in locations.")
     [c/button {:title "Create Character"
                :icon "plus"
                :fill true
                :on-click #(>evt [:open-character-modal])}]]))

(defn sidebar [location-id]
  (let [{:keys [active-pane active-layer]} (<sub [:location-editor/ui])]
    [:<>
     [c/tabs {:items [[:info "Info"]
                      [:paint "Level"]]
              :active active-pane
              :on-change #(>evt [:location-editor/set-active-pane %])}]
     (case active-pane
       :info [:<>
              [sidebar-info location-id]
              [sidebar-resize location-id]]
       :paint [:<>
               [sidebar-layers]
               (case active-layer
                 :background [sidebar-paint location-id]
                 :collision [sidebar-collision]
                 :entities [:<>
                            [sidebar-player-and-exit]
                            [sidebar-npcs location-id]])])]))

(defn tile-style [x y]
  {:width (str config/tile-size "px")
   :height (str config/tile-size "px")
   :top (* y config/tile-size)
   :left (* x config/tile-size)})

(defn do-all-tiles [[[x1 y1] [x2 y2] :as rect] layer-title f]
  [:<>
   (for [x (range x1 (inc x2))
         y (range y1 (inc y2))
         :let [tile [x y]]]
     (if-let [tile-data (f tile)]
       [:div {:key (str "location-cell:" layer-title ":" tile)
              :class "level__tile"
              :style (apply tile-style (u/rect->0 rect tile))}
        tile-data]))])

(defn do-some-tiles [rect coll layer-title f]
  [:<> (for [[tile item] coll
             :when (u/rect-contains? rect tile)]
         [:div {:key (str "location-cell:" layer-title ":" tile)
                :class "level__tile"
                :style (apply tile-style (u/rect->0 rect tile))}
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

(defn background-tiles [rect background]
  [do-all-tiles rect "background"
   (fn [tile]
     (when-let [t (get background tile)]
       [c/sprite-texture t]))])

(defn player-tile [rect position]
  (when (u/rect-contains? rect position)
    [:div {:key (str "location-cell:player:" position)
           :class "level__tile"
           :style (apply tile-style (u/rect->0 rect position))}
     [c/sprite-texture :human "Player"]]))

(defn npc-layer [rect npcs]
  [do-some-tiles rect npcs "npc"
   (fn [tile {:keys [display-name texture]}]
     [c/sprite-texture texture display-name])])

(defn conntection-trigger-layer [rect connection-triggers]
  [do-some-tiles rect connection-triggers "connection-trigger"
   (fn [tile {:keys [display-name]}]
     [:img {:src (texture-path :exit)
            :title (str "to " display-name)}])])

(defn collision-layer [rect blocked]
  [do-all-tiles rect "collision"
   (fn [tile]
     [:div {:class ["interactor"
                    (if (contains? blocked tile)
                      "interactor_not-walkable"
                      "interactor_walkable")]}])])

(defn location-preview [location-id preview-tile]
  (let [tiles-around 3
        dimension [(u/translate-point preview-tile [(- tiles-around) (- tiles-around)])
                   (u/translate-point preview-tile [tiles-around tiles-around])]
        {:keys [background
                connection-triggers
                npcs
                player-position]} (<sub [:location-editor/location location-id])]
    [:div {:class "level"
           :style {:width (u/px (* config/tile-size (inc (* tiles-around 2))))
                   :height (u/px (* config/tile-size (inc (* tiles-around 2))))}}
     [background-tiles dimension background]
     (when player-position [player-tile dimension player-position])
     [npc-layer dimension npcs]
     [conntection-trigger-layer dimension connection-triggers]
     [:div {:key "location-cell:highlight"
            :class "level__tile level__tile_highlight"
            :style (apply tile-style [tiles-around tiles-around])}]]))

(defn position-select [location-id on-select selected]
  (let [{:keys [dimension
                background
                connection-triggers
                player-position
                npcs]} (<sub [:location-editor/location location-id])
        occupied (<sub [:location-editor/physically-occupied-tiles location-id])]
    [:div {:style {:overflow "scroll"
                   :background-color "#000"
                   :width (u/px 600)
                   :height (u/px 400)
                   :max-width (u/px 600)
                   :max-height (u/px 400)}}
     [:div {:class "level"
            :style {:width (u/px (* config/tile-size (u/rect-width dimension)))
                    :height (u/px (* config/tile-size (u/rect-height dimension)))
                    :margin "auto"}}
      [background-tiles dimension background]
      (when player-position [player-tile dimension player-position])
      [npc-layer dimension npcs]
      [conntection-trigger-layer dimension connection-triggers]
      (when selected
        [:div {:key "location-cell:selected"
               :class "level__tile level__tile_highlight"
               :style (apply tile-style (u/rect->0 dimension selected))}])
      [do-all-tiles dimension "selectors"
       (fn [tile]
         (let [occupied? (contains? occupied tile)]
           [:div {:class ["interactor"
                          (when occupied? "interactor_disabled")]
                  :on-click (when-not occupied? #(on-select tile))}]))]]]))

(defn npc-popover [location-id tile]
  (let [{:keys [id display-name dialogue-id dialogue-synopsis]}
        (<sub [:location-editor/npc-popover location-id tile])]
    [:div {:class "level-popover"}
     [:header
      display-name " "
      [:a.edit {:on-click #(do (>evt [:close-popover])
                               (>evt [:open-character-modal id]))}
       [c/icon "edit" (str "Edit " display-name)]]]
     [:ul
      [:li.level-popover__reference
       [:span.level-popover__reference__title "Dialogue"]
       [:span.level-popover__reference__payload
        [:a {:on-click #(do (>evt [:close-popover])
                            (>navigate :dialogue-edit :id dialogue-id))}
         dialogue-synopsis]]]]
     [c/button {:title "Remove Character"
                :type :danger
                :fill true
                :on-click #(do (>evt [:close-popover])
                               (>evt [:location-editor/remove-character id]))}]]))

(defn trigger-popover [location-id tile]
  (let [{:keys [id display-name position position-normalized]}
        (<sub [:location-editor/trigger-popover location-id tile])]
    [:div {:class "level-popover"}
     [:ul
      [:li.level-popover__reference
       [:span.level-popover__reference__title "Exit to"]
       [:span.level-popover__reference__payload
        [:a {:on-click #(do (>evt [:close-popover])
                            (>navigate :location-edit :id id))}
         display-name " " (str position-normalized)]
        [location-preview id position]]]]
     [c/button {:title "Remove Exit"
                :type :danger
                :fill true
                :on-click #(do (>evt [:close-popover])
                               (>evt [:location-editor/remove-trigger location-id tile]))}]]))

(defn canvas [location-id]
  (let [{:keys [dimension
                background
                blocked
                npcs
                connection-triggers
                player-position]} (<sub [:location-editor/location location-id])
        {:keys [active-layer visible-layers highlight]} (<sub [:location-editor/ui])
        [dnd-type dnd-payload] (<sub [:dnd-payload])
        show-layer? (fn [layer] (or (= active-layer layer)
                                    (contains? visible-layers layer)))
        dropzone-fn (case dnd-type
                      :player             #(>evt [:location-editor/move-player location-id %])
                      :character          #(>evt [:location-editor/move-character location-id dnd-payload %])
                      :connection-trigger #(>evt [:location-editor/move-trigger location-id dnd-payload %])
                      nil)]

    [:div {:class "level-wrap"}
     [:div {:class "level"
            :style {:width (u/px (* config/tile-size (u/rect-width dimension)))
                    :height (u/px (* config/tile-size (u/rect-height dimension)))}}
      (when (show-layer? :background)
        [background-tiles dimension background])

      (when (show-layer? :collision)
        [collision-layer dimension blocked])

      (when (show-layer? :entities)
        [:<>
          (when player-position [player-tile dimension player-position])
          [npc-layer dimension npcs]
          [conntection-trigger-layer dimension connection-triggers]])

      (case active-layer
        :background
        [tile-paint-canvas
         {:dimension dimension
          :on-paint #(>evt [:location-editor/paint location-id %])}]

        :collision
        [tile-paint-canvas
         {:dimension dimension
          :on-paint #(>evt [:location-editor/set-walkable location-id %])}]

        :entities
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
               [dnd-texture :human]])])

         [do-some-tiles dimension npcs "npc-select"
          (fn [tile {:keys [id texture display-name]}]
            [:div {:class "interactor interactor_draggable"
                   :title display-name
                   :draggable true
                   :on-drag-start (fn [e]
                                    (set-dnd-texture! e)
                                    (.setData (.-dataTransfer e) "text/plain" display-name)
                                    (>evt [:location-editor/start-entity-drag [:character id]]))}
             [c/popover-trigger {:popover [npc-popover location-id tile]}]
             [dnd-texture texture]])]

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
             [dnd-texture :exit]])]

         (when (fn? dropzone-fn)
           [dropzone {:dimension dimension
                      :highlight highlight
                      :occupied (<sub [:location-editor/occupied-tiles location-id])
                      :on-drop dropzone-fn}])]

        nil)]]))

(defn location-editor [location-id]
  (if (<sub [:location-editor/location-exists? location-id])
    [:div {:class "location-editor"
           :on-drag-end #(>evt [:location-editor/stop-entity-drag])}
     [:div {:class "location-editor__sidebar"}
      [sidebar location-id]]
     [:div {:class "location-editor__canvas"}
      [canvas location-id]]]
    "Location not found."))
