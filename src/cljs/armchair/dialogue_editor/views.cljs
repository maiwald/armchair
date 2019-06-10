(ns armchair.dialogue-editor.views
  (:require [reagent.core :as r]
            [armchair.components
             :as c
             :refer [icon drag-canvas curved-connection connection e->graph-cursor]]
            [armchair.routes :refer [>navigate]]
            [armchair.config :as config]
            [armchair.slds :as slds]
            [armchair.util :as u :refer [<sub >evt stop-e! prevent-e! e->val e->left?]]))

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

(defn get-option-start-position [elem]
  (let [graph (aget (js/document.getElementsByClassName "graph") 0)]
    (when (and (some? elem) (some? graph))
      (let [{:keys [right top]} (u/get-rect elem)
            top-offset (:top (u/get-rect graph))]
        [right (- top top-offset)]))))

(defn connector [{:keys [connected? connector disconnector]}]
  (if connected?
    [:div {:class "action"
           :on-mouse-down stop-e!
           :on-click disconnector}
     [icon "times-circle" "Disconnect"]]
    [:div {:class "action action_connect"
           :on-mouse-down (fn [e]
                            (when (e->left? e)
                              (prevent-e! e)
                              (connector e)))}
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
        edit-condition #(>evt [:modal/open-condition-modal line-id index])
        move-up #(>evt [:dialogue-editor/move-option line-id index :up])
        move-down #(>evt [:dialogue-editor/move-option line-id index :down])
        delete #(when (js/confirm "Do you really want to delete this option?")
                  (>evt [:dialogue-editor/delete-option line-id index]))]
    (fn [line-id index {:keys [text connected? conditions condition-conjunction]}]
      [:li
       [c/action-wrapper {:actions
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
                      :connector #(>evt [:start-connecting-lines line-id (e->graph-cursor %) index])
                      :disconnector #(>evt [:dialogue-editor/disconnect-option line-id index])}]]
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
                      #(>evt [:armchair.modals.switch-form/open switch-id]))}
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

(defn line-connection [start end]
  (let [[start-x start-y] (<sub [:ui/position start])
        [end-x end-y] (<sub [:ui/position end])]
    [curved-connection
     {:start (u/translate-point [start-x start-y] [(+ config/line-width 15) top-offset])
      :end (u/translate-point [end-x end-y] [0 top-offset])}]))

(defn option-connection [start index end]
  (let [_ (<sub [:ui/position start])
        [end-x end-y] (<sub [:ui/position end])]
    (when-let [start (get-option-start-position (get @option-position-lookup [start index]))]
      [curved-connection
       {:start (u/translate-point start [35 20])
        :end (u/translate-point [end-x end-y] [0 top-offset])}])))

(defn dialogue-editor [dialogue-id]
  (if-let [{:keys [npc-line-ids
                   player-line-ids
                   trigger-node-ids
                   line-connections
                   option-connections]} (<sub [:dialogue-editor/dialogue dialogue-id])]
    [:div {:class "content-wrapper"}
     [:div {:class "new-item-button"}
      [c/button {:title "New Player Line"
                 :icon "plus"
                 :on-click #(>evt [:create-player-line dialogue-id])}]
      [c/button {:title "New NPC Line"
                 :icon "plus"
                 :on-click #(>evt [:create-npc-line dialogue-id])}]
      [c/button {:title "New Trigger Node"
                 :icon "plus"
                 :on-click #(>evt [:create-trigger-node dialogue-id])}]]
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
