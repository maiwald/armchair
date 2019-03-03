(ns armchair.dialogue-editor.views
  (:require [reagent.core :as r]
            [armchair.components
             :as c
             :refer [icon drag-canvas connection e->graph-cursor]]
            [armchair.routes :refer [>navigate]]
            [armchair.config :as config]
            [armchair.slds :as slds]
            [armchair.util :as u :refer [<sub >evt stop-e! e-> e->val e->left?]]))

(defn connector [{:keys [connected? connector disconnector]}]
  (if connected?
    [:div {:class "action"
           :on-mouse-down stop-e!
           :on-click disconnector}
     [icon "unlink" "Disconnect"]]
    [:div {:class "action action_connect"
           :on-mouse-down (e-> #(when (e->left? %) (connector %)))}
     [icon "link" "Connect"]]))

(defn npc-line-component [line-id]
  (let [{:keys [state
                initial-line?
                connected?
                text
                character-name
                character-color]} (<sub [:dialogue-editor/npc-line line-id])]
    [c/graph-node {:title character-name
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
     [c/connectable {:connector
                     [connector {:connected? connected?
                                 :connector #(>evt [:start-connecting-lines line-id (e->graph-cursor %)])
                                 :disconnector #(>evt [:dialogue-editor/disconnect-line line-id])}]}
      [:div {:class "line__scroll-wrapper"
             :style {:height (u/px config/line-height)}}
       [:p.line__text text]]]]))

(defn player-line-option-component [line-id index option]
  (let [{:keys [text connected?]} option]
    [:li
     [c/connectable {:connector
                     [connector {:connected? connected?
                                 :connector #(>evt [:start-connecting-lines line-id (e->graph-cursor %) index])
                                 :disconnector #(>evt [:dialogue-editor/disconnect-option line-id index])}]}
      [:div {:class "line__scroll-wrapper"
             :style {:height (u/px config/line-height)}}
       [:ul.line__conditions
        [:li
         [:span.line__conditions__switch-name "foo"]
         " " [:span.line__conditions__switch-condition "is"]
         " " [:span.line__conditions__switch-value "bar"]]
        [:li
         [:span.line__conditions__switch-name "some other thing"]
         " " [:span.line__conditions__switch-condition "is not"]
         " " [:span.line__conditions__switch-value "some other value"]]]
       [:p.line__text text]]]]))

(defn player-line-component [line-id]
  (letfn [(action-delete [e]
            (when (js/confirm "Are your sure you want to delete this line?")
              (>evt [:delete-line line-id])))
          (action-edit [e]
            (>evt [:open-player-line-modal line-id]))]
    (fn [line-id]
      (let [options (<sub [:dialogue-editor/player-line-options line-id])]
        [c/graph-node {:title "Player"
                       :on-connect-end #(>evt [:end-connecting-lines line-id])
                       :actions [["trash" "Delete" action-delete]
                                 ["edit" "Edit" action-edit]]}
         [:ul {:class "line__options"}
          (map-indexed (fn [index option]
                         ^{:key (str "line-option" line-id ":" index)}
                         [player-line-option-component line-id index option])
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
  (let [{:keys [trigger-ids connected?]} (<sub [:dialogue-editor/trigger-node id])]
    [c/graph-node {:title "Triggers"
                   :on-connect-end #(>evt [:end-connecting-lines id])
                   :actions [["trash"
                              "Delete"
                              #(when (js/confirm "Are your sure you want to delete this trigger node")
                                 (>evt [:dialogue-editor/delete-trigger-node id]))]]}
     [c/connectable {:connector
                     [connector {:connected? connected?
                                 :connector #(>evt [:start-connecting-lines id (e->graph-cursor %)])
                                 :disconnector #(>evt [:dialogue-editor/disconnect-line id])}]}
      [:ul.line__triggers
       (for [trigger-id trigger-ids]
         ^{:key (str "trigger" trigger-id)}
         [trigger-component id trigger-id])]
      [:div.line__footer
       [:a
        {:on-click #(>evt [:modal/open-trigger-creation id])
         :on-mouse-down stop-e!}
        [icon "plus"] " Add Trigger"]]]]))

(defn line-connection [start end]
  (let [start-pos (<sub [:ui/position start])
        end-pos (<sub [:ui/position end])]
    [connection {:start (u/translate-point start-pos [(- config/line-width 15)
                                                      (+ 33 (/ config/line-height 2))])
                 :end (u/translate-point end-pos [15 (+ 33 (/ config/line-height 2))])}]))

(defn option-connection [start index end]
  (let [start-pos (<sub [:ui/position start])
        end-pos (<sub [:ui/position end])]
    [connection {:start (u/translate-point start-pos [(- config/line-width 15)
                                                      (+ 33
                                                         (/ config/line-height 2)
                                                         (* index config/line-height))])
                 :end (u/translate-point end-pos [15 (+ 33 (/ config/line-height 2))])}]))

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
