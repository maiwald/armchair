(ns armchair.dialogue-editor.views
  (:require [armchair.components :refer [icon drag-canvas connection e->graph-cursor]]
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
                character-color]} (<sub [:dialogue-editor/npc-line line-id])
        connecting? (some? (<sub [:connector]))]
    [:div {:class "line"
           :on-mouse-up (when connecting? #(>evt [:end-connecting-lines line-id]))
           :style {:border-color character-color
                   :width (str config/line-width "px")}}
     [:header {:class "line__header"}
      [:p {:class "name"} character-name]
      [:ul {:class "actions"
            :on-mouse-down stop-e!}
       (when-not (or initial-line? (some? state))
         [:li {:class "action"
               :on-click #(>evt [:open-dialogue-state-modal line-id])}
          [icon "tag" "Create named state"]])
       (when-not initial-line?
         [:li {:class "action"
               :on-click #(when (js/confirm "Are your sure you want to delete this line?")
                            (>evt [:delete-line line-id]))}
          [icon "trash" "Delete"]])
       [:li {:class "action"
             :on-click #(>evt [:open-npc-line-modal line-id])}
        [icon "edit" "Edit"]]]]
     (cond
       initial-line? [:div {:class "line__state"}
                      [icon "tag"]
                      [:p {:class "description"} "Initial Line"]]
       (some? state) [:div {:class "line__state"}
                      [icon "tag"]
                      [:a {:class "description"
                           :on-mouse-down stop-e!
                           :on-click #(>evt [:open-dialogue-state-modal line-id])}
                       state]
                      [:a {:on-mouse-down stop-e!
                           :on-click #(>evt [:dialogue-editor/delete-dialogue-state line-id])}
                       [icon "times-circle" "Delete state"]]])
     [:div {:class "line__text"
            :style {:height (str config/line-height "px")}}
      [:p text]
      (if connected?
        [:div {:class "action"
               :on-mouse-down stop-e!
               :on-click #(>evt [:dialogue-editor/disconnect-line line-id])}
         [icon "unlink" "Connect"]]
        [:div {:class "action action_connect"
               :on-mouse-down (e-> #(when (e->left? %)
                                      (>evt [:start-connecting-lines line-id (e->graph-cursor %)])))}
         [icon "link" "Connect"]])]]))

(defn player-line-option-component [line-id index option]
  (let [{:keys [text connected?]} option]
    [:li {:class "line__text"
          :style {:height (str config/line-height "px")}}
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
  (let [options (<sub [:dialogue-editor/player-line-options line-id])
        connecting? (some? (<sub [:connector]))]
    [:div {:class "line"
           :on-mouse-up (when connecting? #(>evt [:end-connecting-lines line-id]))
           :style {:width (str config/line-width "px")}}
     [:div {:class "line__header"}
      [:p {:class "name"} "Player"]
      [:ul {:class "actions"
            :on-mouse-down stop-e!}
       [:li {:class "action"
             :on-click #(when (js/confirm "Are your sure you want to delete this line?")
                          (>evt [:delete-line line-id]))}
        [icon "trash" "Delete"]]
       [:li {:class "action"
             :on-click #(>evt [:open-player-line-modal line-id])}
        [icon "edit" "Edit"]]]]
     [:ul {:class "line__options"}
      (map-indexed (fn [index option]
                     ^{:key (str "line-option" line-id ":" index)}
                     [player-line-option-component line-id index option])
                   options)]]))

(defn trigger-component [trigger-node-id trigger-id]
  (let [{:keys [switch-id switch-name switch-value]} (<sub [:dialogue-editor/trigger trigger-id])]
    [:li
     [:a {:on-mouse-down stop-e!
          :on-click #(>navigate :dialogue-edit :id switch-id)}
      [:span.triggers__switch-name switch-name]
      [:span.triggers__switch-value switch-value]]
     [:a {:on-mouse-down stop-e!
          :on-click #(>evt [:dialogue-editor/delete-trigger trigger-node-id trigger-id])}
      [icon "times-circle"]]]))

(defn trigger-node-component [id]
  (let [connecting? (some? (<sub [:connector]))
        {:keys [trigger-ids connected?]} (<sub [:dialogue-editor/trigger-node id])]
    [:div.line {:on-mouse-up (when connecting? #(>evt [:end-connecting-lines id]))
                :style {:width (str config/line-width "px")}}
     [:div.line__header
      [:p.name "Triggers"]
      [:ul.actions {:on-mouse-down stop-e!}
       [:li {:class "action"
             :on-click #(when (js/confirm "Are your sure you want to delete this trigger node")
                          (>evt [:dialogue-editor/delete-trigger-node id]))}
        [icon "trash" "Delete"]]]]
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
       [icon "plus"] " Add Trigger"]]]))

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
