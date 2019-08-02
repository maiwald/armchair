(ns armchair.components
  (:require [clojure.string :refer [join]]
            [reagent.core :as r]
            [armchair.config :as config]
            [armchair.textures :refer [texture-path sprite-lookup]]
            [armchair.math :refer [abs clip point-delta translate-point]]
            [armchair.util :as u :refer [stop-e! >evt <sub e->left?]]))

;; Drag & Drop

(defn e->graph-cursor [e]
  (u/relative-cursor e (-> js/document
                           (.getElementsByClassName "graph")
                           (aget 0))))

(defn start-dragging-handler [ids]
  (fn [e]
    (when (e->left? e)
      (u/prevent-e! e)
      (>evt [:start-dragging ids (e->graph-cursor e)]))))

(defn connection [{:keys [kind start end]}]
  [:line {:class ["graph__connection"
                  (when (= kind :connector) "graph__connection_is-connector")]
          :x1 (:x start)
          :y1 (:y start)
          :x2 (:x end)
          :y2 (:y end)}])

(defn curved-connection [{:keys [kind start end]}]
  (let [[dx dy] (point-delta start end)
        m (translate-point start (/ dx 2) (/ dy 2))
        ctrl-x (if (pos? dx)
                 (+ (:x start)
                    (max (/ (abs dx) 6) 30))
                 (max (- (+ (:x start) (abs dx))
                         (/ (abs dx) 6))
                      (+ (:x start) 30)))
        ctrl-y (+ (:y start) (/ (- (:y m) (:y start)) 4))]
    [:path {:class ["graph__connection"
                    (when (= kind :connector) "graph__connection_is-connector")]
            :d (join " " (concat ["M" (:x start) (:y start)]
                                 (if (< 20 (abs dy))
                                   ["Q" ctrl-x ctrl-y (:x m) (:y m) "T" (:x end) (:y end)]
                                   ["L" (:x end) (:y end)])))}]))

(defn drag-item [item-id component]
  (let [position (<sub [:ui/position item-id])
        dragging? (<sub [:dragging-item? item-id])]
    [:div {:on-mouse-down stop-e!
           :class ["graph__item"
                   (when dragging? "graph__item_is-dragging")]
           :style {:left (:x position) :top (:y position)}}
     [component item-id]]))

(defn drag-canvas [{:keys [kind nodes]}]
  (let [connecting? (some? (<sub [:connector]))
        dragging? (<sub [:dragging?])
        mouse-down (start-dragging-handler (-> nodes vals flatten set))
        mouse-move (when (or dragging? connecting?)
                     #(>evt [:move-cursor (e->graph-cursor %)]))
        mouse-up (cond
                   connecting? #(>evt [:abort-connecting])
                   dragging? #(>evt [:end-dragging]))]
    [:div {:class (cond-> ["graph"]
                    dragging? (conj "graph_is-dragging")
                    connecting? (conj "graph_is-connecting"))
           :on-mouse-down mouse-down
           :on-mouse-move mouse-move
           :on-mouse-up mouse-up}
     (into [:div] (r/children (r/current-component)))
     (for [[item-component ids] nodes
           id ids]
       ^{:key (str kind id)} [drag-item id item-component])]))

;; Icon

(defn icon [glyph title options]
  (let [[title options] (if (map? title)
                          [nil title]
                          [title options])]
    [:i {:class [(str "fas fa-" glyph)
                 (when (:fixed? options) "fa-fw")]
         :title title}]))

;; Tag

(defn tag [{glyph :icon :keys [title on-click on-remove]}]
  [:div.tag
   (when glyph [icon glyph])
   (if on-click
     [:a {:class "tag__description"
          :on-click on-click}
      title]
     [:span {:class "tag__description"}
      title])
   (when on-remove
     [:a {:class "tag__remove"
          :on-click on-remove}
      [icon "times-circle" "Delete state"]])])

;; Dropdown

(defn dropdown [title items]
  (let [^boolean open (r/atom false)
        toggle-fn (fn []
                    (swap! open not)
                    nil)
        item-click-fn (fn [handler]
                        (fn []
                          (reset! open false)
                          (handler)
                          nil))]
    (fn [title items]
      [:div.dropdown
       [:div.dropdown__button
        [:div.dropdown__button__title title]
        [:div.dropdown__button__toggle
         [:a {:on-click toggle-fn} [icon "caret-down"]]]]
       (when @open
         [:ul.dropdown__menu
          (for [{:keys [title on-click]} items]
            [:li {:key (str "breadcrump" title)
                  :on-click (item-click-fn on-click)}
             title])])])))

;; Graph Node

(defn graph-node [{id :item-id}]
  (let [dragging? (<sub [:dragging-item? id])
        start-dragging (start-dragging-handler #{id})
        stop-dragging (when dragging?
                        (fn [e]
                          (u/prevent-e! e)
                          (>evt [:end-dragging])))]
    (fn [{:keys [title color actions on-connect-end]}]
      [:div {:class "graph-node"
             :on-mouse-up (when (some? (<sub [:connector])) on-connect-end)
             :style {:border-color color
                     :width (u/px config/line-width)}}
       [:div {:class "graph-node__header"
              :on-mouse-down start-dragging
              :on-mouse-up stop-dragging}
        [:p {:class "graph-node__header__title"} title]
        [:ul {:class "graph-node__header__actions actions"
              :on-mouse-down stop-e!}
         (for [[action-icon action-title action-handler :as action] actions
               :when (some? action)]
           [:li {:key (str id "-" title "-" action-title)
                 :class "action"
                 :on-click action-handler}
            [icon action-icon action-title]])]]
       (into [:div {:class "graph-node__content"}]
             (r/children (r/current-component)))])))

(defn action-wrapper [{:keys [actions]}]
  [:div.action-wrapper
   (into [:div.action-wrapper__content]
         (r/children (r/current-component)))
   (into [:div.action-wrapper__actions.actions_vertical]
         actions)])

;; Button

(defn button [{glyph :icon btn-type :type
               :keys [title on-click danger fill]}]
  [:button {:class ["button"
                    (when fill "button_fill")
                    (when (= btn-type :danger) "button_danger")]
            :on-click on-click
            :type "button"}
   (when (some? glyph) [:div {:class "button__icon"} [icon glyph title]])
   (when (some? title) [:div {:class "button__title"} title])])

;; Sprite Texture

(defn sprite-texture [texture title]
  (if-let [[file [x-offset y-offset]] (get sprite-lookup texture)]
    [:div.sprite-texture
     {:title title
      :style {:width (u/px config/tile-size)
              :height (u/px config/tile-size)
              :background-image (str "url(" (texture-path file) ")")
              :background-position (str (u/px (- x-offset))
                                        " "
                                        (u/px (- y-offset)))}}]
    [:img {:src (texture-path :missing_texture)
           :width (u/px config/tile-size)
           :height (u/px config/tile-size)}]))

;; Tabs

(defn tabs [{:keys [items active on-change]}]
  [:ul {:class "tabs"}
   (for [[id title] items]
     [:li {:key (str (hash items) id)
           :class ["tabs__item"
                   (when (= id active) "is-active")]
           :on-click #(on-change id)}
      title])])

;; Popover

(defn popover-trigger [{content :popover}]
  (into [:div {:style {:width "100%"
                       :height "100%"}
               :on-mouse-up u/stop-e!
               :on-click #(>evt [:open-popover (.-currentTarget %) content])}]
        (r/children (r/current-component))))

(defn popover-positioned []
  (let [position (r/atom [-9999 -9999])
        get-position (fn [this]
                       (let [offset 8
                             rect (u/get-rect (r/dom-node this))
                             ref-rect (u/get-rect (:reference (r/props this)))]
                         [(clip
                            (- (.-innerWidth js/window) (:w rect))
                            (+ (- (:x ref-rect) (/ (:w rect) 2))
                               (/ (:w ref-rect) 2)))
                          (clip
                            (- (.-innerHeight js/window) (:h rect))
                            (- (:y ref-rect) (:h rect) offset))]))]
    (r/create-class
      {:display-name "popover-positioned"
       :component-did-mount
       (fn [this]
         (reset! position (get-position this)))

       :component-did-update
       (fn [this]
         (let [new-position (get-position this)]
           (if (not= @position new-position)
             (reset! position new-position))))

       :reagent-render
       (fn [{:keys [content reference]}]
         [:div {:on-mouse-up u/stop-e!
                :class "popover"
                :style {:left (u/px (first @position))
                        :top (u/px (second @position))}}
          content])})))

(defn popover []
  (if-let [data (<sub [:popover])]
    [popover-positioned data]))
