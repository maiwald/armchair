(ns armchair.dialogue-editor.views
  (:require [reagent.core :as r]
            [armchair.components
             :as c
             :refer [icon drag-canvas connection e->graph-cursor]]
            [armchair.routes :refer [>navigate]]
            [armchair.config :as config]
            [armchair.slds :as slds]
            [armchair.util :as u :refer [<sub >evt stop-e! e-> e->val e->left?]]))

(def option-position-lookup (r/atom {}))

(defn inline-textarea [{:keys [text on-change]}]
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
         [:textarea {:on-mouse-down stop-e!
                     :on-change handle-text-change
                     :on-blur #(handle-text-blur @text-state)
                     :value @text-state}])})))

(defn get-rect [elem]
  (let [graph (-> js/document
                  (.getElementsByClassName "graph")
                  (aget 0))]
    (if (and (some? elem)
             (some? graph))
      (let [rect (.getBoundingClientRect elem)
            top-offset (.-top (.getBoundingClientRect graph))]
        {:top (- (.-top rect) top-offset)
         :left (.-left rect)
         :bottom (- (.-bottom rect) top-offset)
         :right (.-right rect)
         :width (.-width rect)
         :height (.-height rect)})
      {:top 0 :left 0 :botton 0 :right 0 :width 0 :height 0})))

(defn connector [{:keys [connected? connector disconnector]}]
  (if connected?
    [:div {:class "action"
           :on-mouse-down stop-e!
           :on-click disconnector}
     [icon "times-circle" "Disconnect"]]
    [:div {:class "action action_connect"
           :on-mouse-down (e-> #(when (e->left? %)
                                  (.preventDefault %)
                                  (connector %)))}
     [icon "circle-notch" "Connect"]]))

(defn npc-line-component [line-id]
  (let [{:keys [state
                initial-line?
                connected?
                text
                character-name
                character-color]} (<sub [:dialogue-editor/npc-line line-id])]
    [c/graph-node {:title character-name
                   :item-id line-id
                   :color character-color
                   :on-connect-end #(>evt [:end-connecting-lines line-id])
                   :actions [(when-not (or initial-line? (some? state))
                               ["tag"
                                "Create named state"
                                #(>evt [:open-dialogue-state-modal line-id])])
                             (when-not initial-line?
                               ["trash"
                                "Delete"
                                #(when (js/confirm "Are your sure you want to delete this line?")
                                   (>evt [:delete-line line-id]))])
                             ["edit" "Edit" #(>evt [:open-npc-line-modal line-id])]]}
     (cond
       initial-line? [:div {:class "line__state"}
                      [c/tag {:icon "tag"
                              :title "Initial Line"}]]
       (some? state) [:div {:class "line__state"}
                      [c/tag {:title state
                              :icon "tag"
                              :on-click #(>evt [:open-dialogue-state-modal line-id])
                              :on-remove #(>evt [:dialogue-editor/delete-dialogue-state line-id])}]])
     [:div {:class "line__content-wrapper"}
      [:div.graph-node__connector
       [connector {:connected? connected?
                   :connector #(>evt [:start-connecting-lines line-id (e->graph-cursor %)])
                   :disconnector #(>evt [:dialogue-editor/disconnect-line line-id])}]]
      [:p.line__text
       [inline-textarea {:text text
                         :on-change #(>evt [:update-line line-id :text %])}]]]]))

(defn player-line-option-component [line-id index option total-count]
  (let [handle-text-change #(>evt [:dialogue-editor/update-option line-id index %])
        move-up #(>evt [:dialogue-editor/move-option line-id index :up])
        move-down #(>evt [:dialogue-editor/move-option line-id index :down])
        delete #(when (js/confirm "Do you really want to delete this option?")
                  (>evt [:dialogue-editor/delete-option line-id index]))]
    (fn [line-id index {:keys [text connected?]}]
      [:li
       [c/action-wrapper {:actions
                          [[:div.action {:on-click #(js/alert "edit me")}
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
                      :connector #(>evt [:start-connecting-lines line-id (e->graph-cursor %) index])
                      :disconnector #(>evt [:dialogue-editor/disconnect-option line-id index])}]]
         ; [:div.line__conditions
         ;  [icon "unlock" "Unlock Conditions"]
         ;  [:ul
         ;   [:li
         ;    [:span.line__conditions__switch-name "foo"]
         ;    " " [:span.line__conditions__switch-condition "is"]
         ;    " " [:span.line__conditions__switch-value "bar"]]
         ;   [:li
         ;    [:span.line__conditions__switch-name "some other thing"]
         ;    " " [:span.line__conditions__switch-condition "is not"]
         ;    " " [:span.line__conditions__switch-value "some other value"]]]]
         [:p.line__text
          [inline-textarea {:text text
                            :on-change handle-text-change}]]]]])))

(defn player-line-component [line-id]
  (letfn [(action-delete [e]
            (when (js/confirm "Are your sure you want to delete this line?")
              (>evt [:delete-line line-id])))
          (action-add-option [e]
            (>evt [:dialogue-editor/add-option line-id]))]
    (fn [line-id]
      (let [options (<sub [:dialogue-editor/player-line-options line-id])]
        [c/graph-node {:title "Player"
                       :item-id line-id
                       :on-connect-end #(>evt [:end-connecting-lines line-id])
                       :actions [["trash" "Delete" action-delete]
                                 ["plus" "Add Option" action-add-option]]}
         [:ul {:class "line__options"}
          (map-indexed (fn [index option]
                         ^{:key (str "line-option" line-id ":" index)}
                         [player-line-option-component line-id index option (count options)])
                       options)]]))))


(defn trigger-component [trigger-node-id trigger-id]
  (let [{:keys [switch-kind
                switch-id
                switch-name
                switch-value]} (<sub [:dialogue-editor/trigger trigger-id])]
    [:li
     [:a {:on-mouse-down stop-e!
          :on-click (if (= switch-kind :dialogue-state)
                      #(>navigate :dialogue-edit :id switch-id)
                      #(>evt [:modal/open-switch-modal switch-id]))}
      [:span.line__triggers__switch-name switch-name]
      [:span.line__triggers__switch-value switch-value]]
     [:a {:on-mouse-down stop-e!
          :on-click #(>evt [:dialogue-editor/delete-trigger trigger-node-id trigger-id])}
      [icon "times-circle"]]]))

(defn trigger-node-component [id]
  (letfn [(action-delete [e]
            (when (js/confirm "Are your sure you want to delete this trigger node")
              (>evt [:dialogue-editor/delete-trigger-node id])))
          (action-add-trigger [e]
            (>evt [:modal/open-trigger-creation id]))]
    (fn [id]
      (let [{:keys [trigger-ids connected?]} (<sub [:dialogue-editor/trigger-node id])]
        [c/graph-node {:title "Triggers"
                       :item-id id
                       :on-connect-end #(>evt [:end-connecting-lines id])
                       :actions [["trash" "Delete" action-delete]
                                 ["plus" "Add Trigger" action-add-trigger]]}
         [:div {:class "line__content-wrapper"}
          [:div.graph-node__connector
           [connector {:connected? connected?
                       :connector #(>evt [:start-connecting-lines id (e->graph-cursor %)])
                       :disconnector #(>evt [:dialogue-editor/disconnect-line id])}]]
          [:ul.line__triggers
           (for [trigger-id trigger-ids]
             ^{:key (str "trigger" trigger-id)}
             [trigger-component id trigger-id])]]]))))

(def top-offset 55)
(def left-offset 20)

(defn line-connection [start end]
  (let [[start-x start-y] (<sub [:ui/position start])
        [end-x end-y] (<sub [:ui/position end])]
    [connection {:start (u/translate-point [start-x start-y]
                                           [(+ config/line-width 15) top-offset])

                 :end (u/translate-point [end-x end-y]
                                         [left-offset top-offset])}]))

(defn option-connection [start index end]
  (let [_ (<sub [:ui/position start])
        [end-x end-y] (<sub [:ui/position end])
        {start-right :right
         start-top :top
         start-height :height} (get-rect (get @option-position-lookup [start index]))]
    [connection {:start (u/translate-point [start-right start-top]
                                           [35 20])
                 :end (u/translate-point [end-x end-y]
                                         [left-offset top-offset])}]))

(defn dialogue-editor [dialogue-id]
  (if-let [{:keys [npc-line-ids
                   player-line-ids
                   trigger-node-ids
                   line-connections
                   option-connections]} (<sub [:dialogue-editor/dialogue dialogue-id])]
    [:div {:class "content-wrapper"}
     [:div {:class "new-item-button"}
      [slds/add-button "New Player Line" #(>evt [:create-player-line dialogue-id])]
      [slds/add-button "New NPC Line" #(>evt [:create-npc-line dialogue-id])]
      [slds/add-button "New Trigger Node" #(>evt [:create-trigger-node dialogue-id])]]
     [drag-canvas {:kind "line"
                   :nodes {npc-line-component npc-line-ids
                           player-line-component player-line-ids
                           trigger-node-component trigger-node-ids}}
      [:svg {:class "graph__connection-container" :version "1.1"
             :baseProfile "full"
             :xmlns "http://www.w3.org/2000/svg"}
       (when-let [connector (<sub [:connector])]
         [connection connector])
       (for [[start end] line-connections]
         ^{:key (str "line-connection:" start "->" end)}
         [line-connection start end])
       (for [[start index end] option-connections]
         ^{:key (str "response-connection:" start ":" index "->" end)}
         [option-connection start index end])]]]
    [:span "Dialogue not found."]))
