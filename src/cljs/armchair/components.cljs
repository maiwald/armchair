(ns armchair.components
  (:require [clojure.string :refer [join]]
            [reagent.core :as r]
            [armchair.config :as config]
            [armchair.textures :refer [image-path image-size]]
            [armchair.math :as m :refer [abs point-delta translate-point]]
            [armchair.util :as u :refer [stop-e! >evt <sub e->left?]]))

;; Drag & Drop

(defn e->graph-cursor [e]
  (u/relative-cursor e (-> js/document
                           (.getElementsByClassName "graph")
                           (aget 0))))

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

(defn drag-item [item-id bounds component]
  (let [position (m/global-point (<sub [:ui/position item-id]) bounds)
        dragging? (<sub [:dragging-item? item-id])]
    [:div {:on-mouse-down stop-e!
           :class ["graph__item"
                   (when dragging? "graph__item_is-dragging")]
           :style {:left (:x position) :top (:y position)}}
     [component item-id]]))

(defn drag-canvas []
  (let [elem (atom nil)
        drag-offset (atom nil)]
    (r/create-class
      {:display-name "drag-canvas"
       :component-did-mount
       (fn []
         (reset! drag-offset nil))
       :reagent-render
       (fn [{:keys [kind nodes bounds]}]
         (let [connecting? (some? (<sub [:connector]))
               dragging? (<sub [:dragging?])]
           [:div {:ref #(reset! elem %)
                  :class (cond-> ["graph"]
                           dragging? (conj "graph_is-dragging")
                           connecting? (conj "graph_is-connecting"))
                  :on-mouse-down (fn [e]
                                   (reset! drag-offset (e->graph-cursor e)))
                  :on-mouse-move (fn [e]
                                   (when (or dragging? connecting?)
                                     (>evt [:move-cursor (e->graph-cursor e)]))
                                   (if-some [offset @drag-offset]
                                     (let [cursor (e->graph-cursor e)
                                           [dx dy] (m/point-delta cursor offset)]
                                       (.scrollBy @elem dx dy)
                                       (reset! drag-offset cursor))))
                  :on-mouse-up (fn []
                                 (cond
                                   connecting? (>evt [:abort-connecting])
                                   dragging? (>evt [:end-dragging]))
                                 (reset! drag-offset nil))}
            [:div.graph__scroll-content
             {:style {:width (u/px (:w bounds))
                      :height (u/px (:h bounds))}}
             (into [:<>] (r/children (r/current-component)))
             (for [[item-component ids] nodes
                   id ids]
               ^{:key (str kind ":" id)}
               [drag-item id bounds item-component])]]))})))

;; Icon

(defn icon [glyph title options]
  (let [[title options] (if (map? title)
                          [nil title]
                          [title options])]
    [:i {:class [(str "fas fa-" glyph)
                 (when (:fixed? options) "fa-fw")]
         :title title}]))

(defn spinner []
  [:i {:class ["fas fa-spinner fa-spin"]}])

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

;; Graph Node

(defn graph-node [{id :item-id}]
  (let [dragging? (<sub [:dragging-item? id])
        connecting? (some? (<sub [:connector]))
        start-dragging (fn [e]
                         (when (e->left? e)
                           (u/prevent-e! e)
                           (>evt [:start-dragging #{id} (e->graph-cursor e)])))
        stop-dragging (when dragging?
                        (fn [e]
                          (u/prevent-e! e)
                          (>evt [:end-dragging])))]
    (fn [{:keys [title color width actions on-connect-end]
          :or {color "gray" width (u/px config/line-width)}}]
      [:div {:class "graph-node"
             :on-mouse-up (when connecting? on-connect-end)
             :style {:border-color color
                     :width width}}
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
               :keys [title on-click fill border]
               :or {fill false border true}}]
  [:button {:class ["button"
                    (when fill "button_fill")
                    (when-not border "button_no-border")
                    (when (= btn-type :danger) "button_danger")]
            :on-click on-click
            :type "button"}
   (when (some? glyph) [:div {:class "button__icon"} [icon glyph title]])
   (when (some? title) [:div {:class "button__title"} title])])

;; Sprite Texture

(defn sprite-texture [[file {:keys [x y]}] title zoom-scale]
  (let [{:keys [w h]} (image-size file)
        zoom-scale (or zoom-scale 1)]
    [:div.sprite-texture
     {:title title
      :style {:width (u/px (* zoom-scale config/tile-size))
              :height (u/px (* zoom-scale config/tile-size))
              :background-image (str "url(" (image-path file) ")")
              :background-size (str (u/px (* w zoom-scale))
                                    " "
                                    (u/px (* h zoom-scale)))
              :background-position (str (u/px (- (* zoom-scale config/tile-size x)))
                                        " "
                                        (u/px (- (* zoom-scale config/tile-size y))))}}]))

;; Tabs

(defn tabs [{:keys [items active on-change]}]
  [:ul {:class "tabs"}
   (for [[id title] items]
     [:li {:key (str (hash items) id)
           :class ["tabs__item"
                   (when (= id active) "is-active")]
           :on-click #(on-change id)}
      title])])
