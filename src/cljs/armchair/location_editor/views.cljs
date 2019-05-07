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
            (paint [e]
              (let [tile (u/translate-point (get-tile e) delta)]
                (when (and (some? @painted-tiles)
                           (not (contains? @painted-tiles tile)))
                  (swap! painted-tiles conj tile)
                  (on-paint tile))))]
      (fn []
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
                            :left (px x)}}]))]))))

(defn sidebar-widget [{title :title}]
  (into [:div {:class "location-editor__sidebar-widget"}
         [:div {:class "location-editor__sidebar-widget__title"} title]]
        (r/children (r/current-component))))

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
                      (when (= texture active-texture) "tile-grid__item_active")]}
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
              :style {:background-color "#fff"}}
         [:a {:on-click #(>evt [:location-editor/set-active-walk-state walk-state])
              :style {:height (px config/tile-size)
                      :width (px config/tile-size)
                      :background-color (if walk-state
                                          "rgba(0, 255, 0, .4)"
                                          "rgba(255, 0, 0, .4)")}}]])]]))

(defn sidebar-player []
  [sidebar-widget {:title "Player"}
   [:ul {:class "tile-list"}
    [:li {:class "tile-list__item"
          :draggable true
          :on-drag-start (fn [e]
                           (set-dnd-texture! e)
                           (.setData (.-dataTransfer e) "text/plain" ":player")
                           (>evt [:location-editor/start-entity-drag :player]))}
     [dnd-texture :player]
     [:span {:class "tile-list__item__image"
             :style {:width (str config/tile-size "px")
                     :height (str config/tile-size "px")}}
      [c/sprite-texture :human "Player"]]
     [:span {:class "tile-list__item__label"} "Player"]]]])

(defn sidebar-npcs [location-id]
  (let [available-npcs (<sub [:location-editor/available-npcs location-id])
        dnd-character-id (:character-id (<sub [:dnd-payload]))]
    [sidebar-widget {:title "Available Characters"}
     [:ul {:class "tile-list"}
      (for [[character-id {:keys [display-name texture]}] available-npcs]
        [:li {:key (str "character-select" display-name)
              :class "tile-list__item"
              :draggable true
              :on-drag-start (fn [e]
                               (set-dnd-texture! e)
                               (.setData (.-dataTransfer e) "text/plain" display-name)
                               (>evt [:location-editor/start-entity-drag {:character-id character-id}]))}
         [dnd-texture texture]
         [:span {:class "tile-list__item__image"
                 :style {:width (str config/tile-size "px")
                         :height (str config/tile-size "px")}}
          [c/sprite-texture texture display-name]]
         [:span {:class "tile-list__item__label"} display-name]])]
     (if (empty? available-npcs) "All Characters are placed in locations.")
     [c/button {:title "Create Character"
                :icon "plus"
                :on-click #(>evt [:open-character-modal])}]]))

(defn sidebar [location-id]
  (let [{:keys [tool]} (<sub [:location-editor/ui])]
    [:<>
     [c/tabs {:items [[:info "Info"]
                      [:background-painter "Background"]
                      [:collision "Collision"]
                      [:npcs-select "Entities"]]
              :active tool
              :on-change #(>evt [:location-editor/set-tool %])}]
     (case tool
       :info [:<>
              [sidebar-info location-id]
              [sidebar-resize location-id]]
       :background-painter [sidebar-paint location-id]
       :collision [sidebar-collision]
       :npcs-select [:<>
                     [sidebar-player]
                     [sidebar-npcs location-id]]
       nil)]))

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
     [:div {:key (str "location-cell:" layer-title ":" tile)
            :class "level__tile"
            :style (apply tile-style (u/rect->0 rect tile))}
      (f tile)])])

(defn do-some-tiles [rect coll layer-title f]
  [:<> (for [[tile item] coll
             :when (u/rect-contains? rect tile)]
         [:div {:key (str "location-cell:" layer-title ":" tile)
                :class "level__tile"
                :style (apply tile-style (u/rect->0 rect tile))}
          (f tile item)])])

(defn dropzone [{:keys [dimension highlight on-drop]}]
  [do-all-tiles dimension "dropzone"
   (fn [tile]
     [:div {:class ["interactor" (when (= tile highlight) "interactor_dropzone")]
            :on-drag-over u/prevent-e!
            :on-drag-enter (e-> #(>evt [:location-editor/set-highlight tile]))
            :on-drop (e-> #(on-drop tile))}])])

(defn background-tiles [rect background black?]
  [do-all-tiles rect "background"
   (fn [tile]
     (if black?
       (if-let [t (get background tile)] [c/sprite-texture t])
       [c/sprite-texture (get background tile)]))])

(defn player-layer [rect position]
  [do-some-tiles rect {position :player} "player"
   (fn []
     [c/sprite-texture :human "Player"])])

(defn npc-layer [rect npcs]
  [do-some-tiles rect npcs "npc"
   (fn [tile {:keys [display-name texture]}]
     [c/sprite-texture texture display-name])])

(defn conntection-trigger-layer [rect connection-triggers]
  [do-some-tiles rect connection-triggers "connection-trigger"
   (fn [tile {:keys [id display-name]}]
     [:img {:src (texture-path :exit)
            :title (str "to " display-name)}])])

(defn location-preview [location-id tile]
  (let [tiles-around 3
        dimension [(u/translate-point tile [(- tiles-around) (- tiles-around)])
                   (u/translate-point tile [tiles-around tiles-around])]
        {:keys [background connection-triggers]} (<sub [:location-editor/location location-id])
        npcs (<sub [:location-editor/npcs location-id])
        player-position (<sub [:location-editor/player-position location-id])]
    [:div {:class "level"
           :style {:width (u/px (* config/tile-size (inc (* tiles-around 2))))
                   :height (u/px (* config/tile-size (inc (* tiles-around 2))))}}
     [background-tiles dimension background true]
     (when player-position [player-layer dimension player-position])
     [npc-layer dimension npcs]
     [conntection-trigger-layer dimension connection-triggers]]))

(defn npc-popover [{:keys [id display-name dialogue-id dialogue-synopsis]}]
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
                             (>evt [:location-editor/remove-character id]))}]])

(defn trigger-popover [location-id tile {:keys [id display-name target target-normalized]}]
  [:div {:class "level-popover"}
   [:ul
    [:li.level-popover__reference
     [:span.level-popover__reference__title "Exit to"]
     [:span.level-popover__reference__payload
      [:a {:on-click #(do (>evt [:close-popover])
                          (>navigate :location-edit :id id))}
       display-name " " (str target-normalized)]
      [location-preview id target]]]]
   [c/button {:title "Remove Trigger"
              :type :danger
              :fill true
              :on-click #(do (>evt [:close-popover])
                             (>evt [:location-editor/remove-trigger location-id tile]))}]])

(defn canvas [location-id]
  (let [{:keys [dimension background walk-set connection-triggers]} (<sub [:location-editor/location location-id])
        npcs (<sub [:location-editor/npcs location-id])
        {:keys [tool highlight painting?]} (<sub [:location-editor/ui])
        player-position (<sub [:location-editor/player-position location-id])
        dnd-payload (<sub [:dnd-payload])
        dropzone-fn (cond
                      (= dnd-payload :player)
                      #(>evt [:location-editor/move-player location-id %])

                      (contains? dnd-payload :character-id)
                      #(>evt [:location-editor/move-character location-id (:character-id dnd-payload) %])

                      (contains? dnd-payload :connection-trigger)
                      #(>evt [:location-editor/move-trigger location-id (:connection-trigger dnd-payload) %]))]

    [:div {:class "level-wrap"}
     [:div {:class "level"
            :style {:width (u/px (* config/tile-size (u/rect-width dimension)))
                    :height (u/px (* config/tile-size (u/rect-height dimension)))}}
      [background-tiles dimension background]
      (when-not (contains? #{:background-painter :collision} tool)
        [:<>
         (when player-position [player-layer dimension player-position])
         [npc-layer dimension npcs]
         [conntection-trigger-layer dimension connection-triggers]])

      (case tool
        :background-painter
        [tile-paint-canvas
         {:dimension dimension
          :on-paint #(>evt [:location-editor/paint location-id %])}]

        :collision
        [:<>
         [do-all-tiles dimension "walkable-area"
          (fn [tile]
            [:div {:class ["interactor"
                           (if (contains? walk-set tile)
                             "interactor_walkable"
                             "interactor_not-walkable")]}])]
         [tile-paint-canvas
          {:dimension dimension
           :on-paint #(>evt [:location-editor/set-walkable location-id %])}]]

        :npcs-select
        [:<>
         [do-some-tiles dimension npcs "npc-select"
          (fn [_ {:keys [id display-name texture dialogue-id dialogue-synopsis] :as npc}]
            [:div {:class "interactor interactor_draggable"
                   :title display-name
                   :draggable true
                   :on-drag-start (fn [e]
                                    (set-dnd-texture! e)
                                    (.setData (.-dataTransfer e) "text/plain" display-name)
                                    (>evt [:location-editor/start-entity-drag {:character-id id}]))}
             [c/popover-trigger {:popover [npc-popover npc]}]
             [dnd-texture texture]])]

         [do-some-tiles dimension connection-triggers "connection-select"
          (fn [tile {:keys [id display-name target target-normalized] :as trigger}]
            [:div {:class "interactor interactor_draggable"
                   :title (str "to " display-name)
                   :draggable true
                   :on-drag-start (fn [e]
                                    (set-dnd-texture! e)
                                    (.setData (.-dataTransfer e) "text/plain" display-name)
                                    (>evt [:location-editor/start-entity-drag {:connection-trigger tile}]))}
             [c/popover-trigger {:popover [trigger-popover location-id tile trigger]}]
             [dnd-texture :exit]])]

         (when (fn? dropzone-fn)
           [dropzone {:dimension dimension
                      :highlight highlight
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
