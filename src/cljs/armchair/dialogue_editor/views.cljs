(ns armchair.dialogue-editor.views
  (:require [reagent.core :as r]
            [armchair.components
             :as c
             :refer [icon curved-connection connection]]
            [armchair.config :as config]
            [armchair.input :as input]
            [armchair.math :as m :refer [translate-point]]
            [armchair.modals.unlock-conditions-form :as unlock-conditions-form]
            [armchair.modals.switch-form :as switch-form]
            [armchair.modals.trigger-creation :as trigger-creation]
            [armchair.util :as u :refer [<sub >evt stop-e! prevent-e! e->val e->left?]]))

(def option-position-lookup (r/atom {}))

(defn e->graph-cursor [e]
  (u/relative-cursor e (js/document.getElementById "graph-canvas")))


(defn inline-textarea [{:keys [label text on-change]}]
  (let [text-state (r/atom text)
        handle-text-change #(reset! text-state (e->val %))
        handle-text-blur on-change]
    (r/create-class
      {:display-name "inline-textarea"
       :component-did-update (fn [this old-args]
                               (let [old-text (:text (second old-args))
                                     new-text (:text (second (r/argv this)))]
                                 (when (not= old-text new-text)
                                   (reset! text-state new-text))))
       :reagent-render
       (fn []
         [input/textarea {:label label
                          :options {:on-mouse-down stop-e!
                                    :on-blur #(handle-text-blur @text-state)}
                          :on-change handle-text-change
                          :value @text-state}])})))

(defn get-option-start-position [elem]
  (let [graph (aget (js/document.getElementsByClassName "scroll-content") 0)]
    (when (and (some? elem) (some? graph))
      (let [rect (u/get-rect elem)
            graph-rect (u/get-rect graph)]
        (translate-point (m/rect-point rect)
                         (- (:w rect) (:x graph-rect))
                         (- (:y graph-rect)))))))

(defn connector [{:keys [connected? on-connect on-disconnect]}]
  (if connected?
    [:div {:class "action"
           :on-mouse-down stop-e!
           :on-click on-disconnect}
     [icon "times-circle" "Disconnect"]]
    [:div {:class "action action_connect"
           :on-mouse-down (fn [e]
                            (when (e->left? e)
                              (prevent-e! e)
                              (on-connect e)))}
     [icon "circle-notch" "Connect"]]))

(defn drag-item [item-id bounds component]
  (let [position (m/global-point (<sub [:ui/position item-id]) bounds)
        dragging? (<sub [:dragging-item? item-id])]
    [:div {:on-mouse-down stop-e!
           :class ["graph__item"
                   (when dragging? "graph__item_is-dragging")]
           :style {:left (:x position) :top (:y position)}}
     [component item-id]]))

(defn drag-canvas []
  (let [connecting? (some? (<sub [:ui/connector]))
        dragging? (<sub [:dragging?])]
    (into [:div {:id "graph-canvas"
                 :class ["graph"
                         (when dragging? "graph_is-dragging")
                         (when connecting? "graph_is-connecting")]
                 :on-mouse-move (fn [e]
                                  (when (or dragging? connecting?)
                                    (>evt [:move-cursor (e->graph-cursor e)])))
                 :on-mouse-up (fn []
                                (cond
                                  connecting? (>evt [:abort-connecting])
                                  dragging? (>evt [:end-dragging])))}]
          (r/children (r/current-component)))))

(defn graph-node [{id :item-id
                   :keys [title color width actions on-connect-end]
                   :or {width (u/px config/line-width)}}]
  (let [dragging? (<sub [:dragging-item? id])
        connecting? (some? (<sub [:ui/connector]))
        start-dragging (fn [e]
                         (when (e->left? e)
                           (u/stop-e! e)
                           (>evt [:start-dragging #{id} (e->graph-cursor e)])))
        stop-dragging (when dragging?
                        (fn [e]
                          (u/stop-e! e)
                          (>evt [:end-dragging])))]
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
           (r/children (r/current-component)))]))

(defn npc-line-component [line-id]
  (let [{:keys [connected?
                text
                character-name
                character-color]} (<sub [:dialogue-editor/npc-line line-id])]
    [graph-node {:title character-name
                 :item-id line-id
                 :color character-color
                 :on-connect-end #(>evt [:dialogue-editor/end-connecting-lines line-id])
                 :actions [["trash" "Delete" #(>evt [:dialogue-editor/delete-node line-id])]
                           ["edit" "Edit" #(>evt [:open-npc-line-modal line-id])]]}
     [:div {:class "line__content-wrapper"}
      [:div.graph-node__connector
       [connector {:connected? connected?
                   :on-connect #(>evt [:dialogue-editor/start-connecting-lines line-id (e->graph-cursor %)])
                   :on-disconnect #(>evt [:dialogue-editor/disconnect-line line-id])}]]
      [:div.line__text
       [inline-textarea {:text text
                         :on-change #(>evt [:update-line line-id :text %])}]]]]))

(defn action-wrapper [{:keys [actions]}]
  [:div.action-wrapper
   (into [:div.action-wrapper__content]
         (r/children (r/current-component)))
   (into [:div.action-wrapper__actions.actions_vertical]
         actions)])

(defn player-line-option-component [line-id index _option _total-count]
  (let [handle-text-change #(>evt [:dialogue-editor/update-option line-id index %])
        edit-condition #(>evt [::unlock-conditions-form/open line-id index])
        move-up #(>evt [:dialogue-editor/move-option line-id index :up])
        move-down #(>evt [:dialogue-editor/move-option line-id index :down])
        delete #(>evt [:dialogue-editor/delete-option line-id index])]
    (fn [line-id
         index
         {:keys [text connected? conditions condition-conjunction]}
         total-count]
      [:li
       [action-wrapper {:actions
                        [[:div.action {:on-click edit-condition}
                          [icon "unlock" "Edit Unlock Conditions"]]
                         [:div.action {:on-click delete}
                          [icon "trash" "Delete"]]
                         (when-not (= 0 index)
                           [:div.action {:on-click move-up}
                            [icon "arrow-up" "Move up"]])
                         (when-not (= (dec total-count) index)
                           [:div.action {:on-click move-down}
                            [icon "arrow-down" "Move down"]])]}
        [:div {:class "line__content-wrapper"
               :ref #(swap! option-position-lookup assoc [line-id index] %)}
         [:div.graph-node__connector
          [connector {:connected? connected?
                      :on-connect #(>evt [:dialogue-editor/start-connecting-lines line-id (e->graph-cursor %) index])
                      :on-disconnect #(>evt [:dialogue-editor/disconnect-option line-id index])}]]
         (when (seq conditions)
           [:div.line__condition
            [icon "unlock" "Unlock Conditions"]
            (when (< 1 (count conditions))
              [:div.line__condition__conjunction condition-conjunction])
            [:ul
             (for [{:keys [switch operator value]} conditions]
               [:li {:key (str line-id index switch operator value)}
                [:span.line__condition__switch switch]
                " " [:span.line__condition__operator operator]
                " " [:span.line__condition__value value]])]])
         [:div.line__text
          [inline-textarea {:text text
                            :on-change handle-text-change}]]]]])))

(defn player-line-component [line-id]
  (letfn [(action-delete [] (>evt [:dialogue-editor/delete-node line-id]))
          (action-add-option [] (>evt [:dialogue-editor/add-option line-id]))]
    (fn [line-id]
      (let [options (<sub [:dialogue-editor/player-line-options line-id])]
        [graph-node {:title "Player"
                     :item-id line-id
                     :on-connect-end #(>evt [:dialogue-editor/end-connecting-lines line-id])
                     :actions [["trash" "Delete" action-delete]
                               ["plus" "Add Option" action-add-option]]}
         [:ul {:class "line__options"}
          (map-indexed (fn [index option]
                         ^{:key (str "line-option" line-id ":" index)}
                         [player-line-option-component line-id index option (count options)])
                       options)]]))))


(defn trigger-component [trigger-node-id trigger-id]
  (let [{:keys [switch-id
                switch-name
                switch-value]} (<sub [:dialogue-editor/trigger trigger-id])]
    [:li
     [:a {:on-mouse-down stop-e!
          :on-click #(>evt [::switch-form/open switch-id])}
      [:span.line__triggers__switch-name switch-name]
      [:span.line__triggers__switch-value switch-value]]
     [:a {:on-mouse-down stop-e!
          :on-click #(>evt [:dialogue-editor/delete-trigger trigger-node-id trigger-id])}
      [icon "times-circle"]]]))

(defn trigger-node-component [id]
  (letfn [(action-delete [] (>evt [:dialogue-editor/delete-node id]))
          (action-add-trigger [] (>evt [::trigger-creation/open id]))]
    (fn [id]
      (let [{:keys [trigger-ids connected?]} (<sub [:dialogue-editor/trigger-node id])]
        [graph-node {:title "Triggers"
                     :item-id id
                     :on-connect-end #(>evt [:dialogue-editor/end-connecting-lines id])
                     :actions [["trash" "Delete" action-delete]
                               ["plus" "Add Trigger" action-add-trigger]]}
         [:div {:class "line__content-wrapper"}
          [:div.graph-node__connector
           [connector {:connected? connected?
                       :on-connect #(>evt [:dialogue-editor/start-connecting-lines id (e->graph-cursor %)])
                       :on-disconnect #(>evt [:dialogue-editor/disconnect-line id])}]]
          [:ul.line__triggers
           (for [trigger-id trigger-ids]
             ^{:key (str "trigger" trigger-id)}
             [trigger-component id trigger-id])]]]))))

(defn initial-line-component [id]
  (let [{:keys [synopsis connected?]} (<sub [:dialogue-editor/initial-line id])]
    [graph-node {:title "Dialogue Start"
                 :item-id id
                 :on-connect-end #(>evt [:dialogue-editor/end-connecting-lines id])}
     [:div {:class "line__content-wrapper"}
      [:div.graph-node__connector
       [connector {:connected? connected?
                   :on-connect #(>evt [:dialogue-editor/start-connecting-initial-line id (e->graph-cursor %)])
                   :on-disconnect #(>evt [:dialogue-editor/disconnect-initial-line id])}]]
      [:div.line__text
       [inline-textarea {:label "Synopsis"
                         :text synopsis
                         :on-change #(>evt [:dialogue-editor/update-synopsis id %])}]]]]))

(defn case-node-component [id]
  (letfn [(action-delete [] (>evt [:dialogue-editor/delete-node id]))]
    (fn [id]
      (let [{:keys [switch-name clauses]} (<sub [:dialogue-editor/case-node id])]
        [graph-node {:title switch-name
                     :item-id id
                     :on-connect-end #(>evt [:dialogue-editor/end-connecting-lines id])
                     :actions [["trash" "Delete" action-delete]]}
         [:ul.line__case_clauses
          (for [{:keys [display-name connected? switch-value-id]} clauses]
            ^{:key (str "case" id "clause" display-name)}
            [:li
             [:div {:class "line__content-wrapper"}
              [:div.graph-node__connector
               [connector {:connected? connected?
                           :on-connect #(>evt [:dialogue-editor/start-connecting-lines id (e->graph-cursor %) switch-value-id])
                           :on-disconnect #(>evt [:dialogue-editor/disconnect-case-clause id switch-value-id])}]]
              [:div display-name]]])]]))))

(def top-offset 55)

(defn line-connection [bounds start end]
  (let [start-pos (m/global-point (<sub [:ui/position start]) bounds)
        end-pos (m/global-point (<sub [:ui/position end]) bounds)]
    [curved-connection
     {:start (translate-point start-pos (+ config/line-width 15) top-offset)
      :end (translate-point end-pos 0 top-offset)}]))

(defn case-connection [bounds start index end]
  (let [start-pos (m/global-point (<sub [:ui/position start]) bounds)
        end-pos (m/global-point (<sub [:ui/position end]) bounds)]
    [curved-connection
     {:start (translate-point start-pos (+ config/line-width 15) (+ (* index 34)
                                                                    top-offset))
      :end (translate-point end-pos 0 top-offset)}]))

(defn option-connection [bounds start index end]
  (let [_ (<sub [:ui/position start])
        end-pos (m/global-point (<sub [:ui/position end]) bounds)]
    (if-let [start-pos (get-option-start-position (get @option-position-lookup [start index]))]
      [curved-connection
       {:start (translate-point start-pos 35 20)
        :end (translate-point end-pos 0 top-offset)}])))

(defn dialogue-editor-header [dialogue-id]
  (let [{:keys [synopsis]} (<sub [:dialogue-editor/header dialogue-id])]
    [:header.page-header {:class "bg-sky-800 text-white"}
     [:h1 "Dialogue: " synopsis]
     [:ul.page-header__actions
      [:li
       [c/button {:title "New Character Line"
                  :icon "plus"
                  :on-click #(>evt [:create-npc-line dialogue-id])}]]
      [:li
       [c/button {:title "New Player Line"
                  :icon "plus"
                  :on-click #(>evt [:create-player-line dialogue-id])}]]
      [:li
       [c/button {:title "New Trigger Node"
                  :icon "plus"
                  :on-click #(>evt [:create-trigger-node dialogue-id])}]]
      [:li
       [c/button {:title "New Switch Node"
                  :icon "plus"
                  :on-click #(>evt [:armchair.modals.case-node-creation/open dialogue-id])}]]]]))

(defn canvas [dialogue-id]
  (if-let [{:keys [bounds
                   lines
                   initial-line-connection
                   line-connections
                   case-connections
                   option-connections]} (<sub [:dialogue-editor/dialogue dialogue-id])]
    [c/scroll-container {:width (:w bounds)
                         :height (:h bounds)}
     [drag-canvas
       (for [{:keys [kind id]} lines]
         ^{:key (str kind ":" id)}
         [drag-item id bounds
          (case kind
            :npc npc-line-component
            :player player-line-component
            :trigger trigger-node-component
            :initial initial-line-component
            :case case-node-component)])
       [drag-item dialogue-id bounds initial-line-component]
       [:svg {:class "graph__connection-container" :version "1.1"
              :baseProfile "full"
              :xmlns "http://www.w3.org/2000/svg"}
        (when-let [{:keys [start end]} (<sub [:ui/connector])]
          [connection {:start start
                       :end end
                       :kind :connector}])
        (when-let [[start end] initial-line-connection]
          [line-connection bounds start end])
        (for [[start end] line-connections]
          ^{:key (str "line-connection:" start "->" end)}
          [line-connection bounds start end])
        (for [[start index end] case-connections]
          ^{:key (str "case-connection:" start ":" index "->" end)}
          [case-connection bounds start index end])
        (for [[start index end] option-connections]
          ^{:key (str "response-connection:" start ":" index "->" end)}
          [option-connection bounds start index end])]]]
    [:span "Dialogue not found."]))

(defn dialogue-editor [dialogue-id]
  [:<>
   [dialogue-editor-header dialogue-id]
   [canvas dialogue-id]])
