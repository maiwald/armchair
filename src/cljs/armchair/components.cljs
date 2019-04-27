(ns armchair.components
  (:require [clojure.string :refer [join]]
            [reagent.core :as r]
            [armchair.config :as config]
            [armchair.textures :refer [texture-path sprite-lookup]]
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

(defn connection [{kind :kind
                   [x1 y1] :start
                   [x2 y2] :end}]
  [:line {:class ["graph__connection"
                  (when (= kind :connector) "graph__connection_is-connector")]
          :x1 x1
          :y1 y1
          :x2 x2
          :y2 y2}])

(defn curved-connection [{kind :kind
                          [x1 y1 :as start] :start
                          [x2 y2 :as end] :end}]
  (let [[dx dy] (u/point-delta start end)
        [mx my] (u/translate-point start [(/ dx 2) (/ dy 2)])
        [ctrl-x ctrl-y] [(if (pos? dx)
                           (+ x1 (max (/ (u/abs dx) 6)
                                      30))
                           (max (- (+ x1 (u/abs dx)) (/ (u/abs dx) 6))
                                (+ x1 30)))
                         (+ y1 (/ (- my y1) 4))]]
    [:path {:class ["graph__connection"
                    (when (= kind :connector) "graph__connection_is-connector")]
            :d (join " " (concat ["M" x1 y1]
                                 (if (< 20 (u/abs dy))
                                   ["Q" ctrl-x ctrl-y mx my "T" x2 y2]
                                   ["L" x2 y2])))}]))

(defn drag-item [item-id component]
  (let [[left top] (<sub [:ui/position item-id])
        dragging? (<sub [:dragging-item? item-id])]
    [:div {:on-mouse-down stop-e!
           :class ["graph__item"
                   (when dragging? "graph__item_is-dragging")]
           :style {:left left :top top}}
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

(defn icon [glyph title]
  [:i {:class (str "fas fa-" glyph)
       :title title}])

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

(defn button [{glyph :icon :keys [title on-click]}]
  [:button {:class "button"
            :on-click on-click}
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

