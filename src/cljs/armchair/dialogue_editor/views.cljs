(ns armchair.dialogue-editor.views
  (:require [armchair.components
             :as c
             :refer [icon drag-canvas connection e->graph-cursor]]
            [armchair.routes :refer [>navigate]]
            [armchair.config :as config]
            [armchair.slds :as slds]
            [armchair.util :as u :refer [<sub >evt stop-e! e-> e->val e->left?]]))

(defn npc-line-component [line-id]
  (let [{:keys [state
                initial-line?
                connected?
                text
                character-name
                character-color]} (<sub [:dialogue-editor/npc-line line-id])]
    [:div.line
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
      [:div {:class "line__text"
             :style {:height (u/px config/line-height)}}
       [:p text]
       (if connected?
         [:div {:class "action"
                :on-mouse-down stop-e!
                :on-click #(>evt [:dialogue-editor/disconnect-line line-id])}
          [icon "unlink" "Connect"]]
         [:div {:class "action action_connect"
                :on-mouse-down (e-> #(when (e->left? %)
                                       (>evt [:start-connecting-lines line-id (e->graph-cursor %)])))}
          [icon "link" "Connect"]])]]]))

(defn player-line-option-component [line-id index option]
  (let [{:keys [text connected?]} option]
    [:li {:class "line__text"
          :style {:height (u/px config/line-height)}}
     [:p text]
     (if connected?
       [:div {:class "action"
              :on-mouse-down stop-e!
              :on-click #(>evt [:dialogue-editor/disconnect-option line-id index])}
        [icon "unlink" "Connect"]]
       [:div {:class "action action_connect"
              :on-mouse-down (e-> #(when (e->left? %)
                                     (>evt [:start-connecting-lines line-id (e->graph-cursor %) index])))}
        [icon "link" "Connect"]])]))

(defn player-line-component [line-id]
  (let [options (<sub [:dialogue-editor/player-line-options line-id])]
    [:div.line
     [c/graph-node {:title "Player"
                    :on-connect-end #(>evt [:end-connecting-lines line-id])
                    :actions [["trash" "Delete" #(when (js/confirm "Are your sure you want to delete this line?")
                                                   (>evt [:delete-line line-id]))]
                              ["edit" "Edit" #(>evt [:open-player-line-modal line-id])]]}
      [:ul {:class "line__options"}
       (map-indexed (fn [index option]
                      ^{:key (str "line-option" line-id ":" index)}
                      [player-line-option-component line-id index option])
                    options)]]]))

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
      [:span.triggers__switch-name switch-name]
      [:span.triggers__switch-value switch-value]]
     [:a {:on-mouse-down stop-e!
          :on-click #(>evt [:dialogue-editor/delete-trigger trigger-node-id trigger-id])}
      [icon "times-circle"]]]))

(defn trigger-node-component [id]
  (let [{:keys [trigger-ids connected?]} (<sub [:dialogue-editor/trigger-node id])]
    [:div.line
     [c/graph-node {:title "Triggers"
                    :on-connect-end #(>evt [:end-connecting-lines id])
                    :actions [["trash"
                               "Delete"
                               #(when (js/confirm "Are your sure you want to delete this trigger node")
                                  (>evt [:dialogue-editor/delete-trigger-node id]))]]}
      [:div.line__text
       [:ul.triggers
        (for [trigger-id trigger-ids]
          ^{:key (str "trigger" trigger-id)}
          [trigger-component id trigger-id])]
       (if connected?
         [:div {:class "action"
                :on-mouse-down stop-e!
                :on-click #(>evt [:dialogue-editor/disconnect-line id])}
          [icon "unlink" "Connect"]]
         [:div {:class "action action_connect"
                :on-mouse-down (e-> #(when (e->left? %)
                                       (>evt [:start-connecting-lines id (e->graph-cursor %)])))}
          [icon "link" "Connect"]])]
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
