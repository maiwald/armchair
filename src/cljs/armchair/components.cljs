(ns armchair.components
  (:require [reagent.core :as r]
            [armchair.util :as u :refer [>evt <sub e-> e->left?]]))

;; Drag & Drop

(defn e->graph-cursor [e]
  (u/relative-cursor e (-> js/document
                           (.getElementsByClassName "graph")
                           (aget 0))))

(defn start-dragging-handler [ids]
  (e-> (fn [e]
         (when (e->left? e)
           (>evt [:start-dragging ids (e->graph-cursor e)])))))

(defn connection [{kind :kind [x1 y1] :start [x2 y2] :end}]
  [:line {:class ["graph__connection"
                  (when (= kind :connector) "graph__connection_is-connector")]
          :x1 x1
          :y1 y1
          :x2 x2
          :y2 y2}])

(defn drag-item [item-id component]
  (let [position (<sub [:ui/position item-id])
        dragging? (<sub [:dragging-item? item-id])
        start-dragging (start-dragging-handler #{item-id})
        stop-dragging (when dragging? (e-> #(>evt [:end-dragging])))]
    [:div {:class ["graph__item"
                   (when dragging? "graph__item_is-dragging")]
           :on-mouse-down start-dragging
           :on-mouse-up stop-dragging
           :style {:left (first position)
                   :top (second position)}}
     [component item-id]]))

(defn drag-canvas [{:keys [kind nodes]} & connection-children]
  (let [connecting? (some? (<sub [:connector]))
        dragging? (<sub [:dragging?])
        mouse-down (start-dragging-handler (-> nodes vals flatten set))
        mouse-move (e-> #(when (or dragging? connecting?)
                           (>evt [:move-cursor (e->graph-cursor %)])))
        mouse-up (cond
                   connecting? (e-> #(>evt [:abort-connecting]))
                   dragging? (e-> #(>evt [:end-dragging])))]
    [:div {:class (cond-> ["graph"]
                    dragging? (conj "graph_is-dragging")
                    connecting? (conj "graph_is-connecting"))
           :on-mouse-down mouse-down
           :on-mouse-move mouse-move
           :on-mouse-up mouse-up}
     (into [:div] connection-children)
     (for [[item-component ids] nodes
           id ids]
       ^{:key (str kind id)} [drag-item id item-component])]))

;; Icon

(defn icon [glyph title]
  [:i {:class (str "fas fa-" glyph)
       :title title}])

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
