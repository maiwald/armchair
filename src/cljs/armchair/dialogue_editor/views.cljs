(ns armchair.dialogue-editor.views
  (:require [armchair.components :refer [icon drag-canvas connection e->graph-cursor]]
            [armchair.config :as config]
            [armchair.slds :as slds]
            [armchair.util :refer [<sub >evt stop-e! e-> e->val e->left? translate-point]]))

(defn npc-line-component [{:keys [id info-ids initial-line? text character-name character-color]}]
  (let [connecting? (some? (<sub [:connector]))]
    [:div {:class "line"
           :on-mouse-up (when connecting? #(>evt [:end-connecting-lines id]))
           :style {:border-color character-color
                   :width (str config/line-width "px")}}
     [:header {:class "line__header"}
      [:p {:class "name"} character-name]
      [:ul {:class "states"}
       (when-not (empty? info-ids)
         [:li {:class "state"} [icon "info-circle" "This line contains infos."]])]
      [:ul {:class "actions" :on-mouse-down stop-e!}
       (when-not initial-line?
         [:li {:class "action" :on-click #(when (js/confirm "Are your sure you want to delete this line?")
                                            (>evt [:delete-line id]))}
          [icon "trash" "Delete"]])
       [:li {:class "action" :on-click #(>evt [:open-npc-line-modal id])}
        [icon "edit" "Edit"]]]]
     (when initial-line?
       [:div {:class "line__state"}
        [icon "sign-out-alt"]
        [:p "Initial Line"]])
     [:div {:class "line__text"
            :style {:height (str config/line-height "px")}}
      [:p text]
      [:div {:class "action action_connect"
             :on-mouse-down (e-> #(when (e->left? %)
                                    (>evt [:start-connecting-lines id (e->graph-cursor %)])))}
       [icon "project-diagram" "Connect"]]]]))

(defn player-line-component [{:keys [id options]}]
  (let [connecting? (some? (<sub [:connector]))]
    [:div {:class "line"
           :on-mouse-up (when connecting? #(>evt [:end-connecting-lines id]))
           :style {:width (str config/line-width "px")}}
     [:div {:class "line__header"}
      [:p {:class "name"} "Player"]
      [:ul {:class "actions" :on-mouse-down stop-e!}
       [:li {:class "action" :on-click #(when (js/confirm "Are your sure you want to delete this line?")
                                          (>evt [:delete-line id]))}
        [icon "trash" "Delete"]]
       [:li {:class "action" :on-click #(>evt [:open-player-line-modal id])}
        [icon "edit" "Edit"]]]]
     [:ul {:class "line__options"}
      (map-indexed (fn [index option]
                     [:li {:key (str "line-option" id ":" index)
                           :class "line__text"
                           :style {:height (str config/line-height "px")}}
                      [:p
                       (when-not (empty? (:required-info-ids option))
                         [:span {:class "state"}
                          [icon "lock" "This option requires information."]])
                       (:text option)]
                      [:div {:class "action action_connect"
                             :on-mouse-down (e-> #(when (e->left? %)
                                                    (>evt [:start-connecting-lines id (e->graph-cursor %) index])))}
                       [icon "project-diagram" "Connect"]]])
                   options)]]))

(defn line-component [line-id]
  (let [line (<sub [:dialogue-editor/line line-id])]
    (case (:kind line)
      :npc [npc-line-component line]
      :player [player-line-component line])))

(defn npc-connection [start end]
  (let [start-pos (<sub [:ui/position start])
        end-pos (<sub [:ui/position end])]
    [connection {:start (translate-point start-pos [(- config/line-width 15)
                                                    (+ 33 (/ config/line-height 2))])
                 :end (translate-point end-pos [15 (+ 33 (/ config/line-height 2))])}]))

(defn player-connection [start index end]
  (let [start-pos (<sub [:ui/position start])
        end-pos (<sub [:ui/position end])]
    [connection {:start (translate-point start-pos [(- config/line-width 15)
                                                    (+ 33
                                                       (/ config/line-height 2)
                                                       (* index config/line-height))])
                 :end (translate-point end-pos [15 (+ 33 (/ config/line-height 2))])}]))

(defn dialogue-editor [dialogue-id]
  (if-let [{:keys [line-ids npc-connections player-connections]} (<sub [:dialogue-editor/dialogue dialogue-id])]
    [:div {:class "full-page"}
     [:div {:class "new-item-button"}
      [slds/add-button "New Player Line" #(>evt [:create-player-line dialogue-id])]
      [slds/add-button "New NPC Line" #(>evt [:create-npc-line dialogue-id])]]
     [drag-canvas {:kind "line"
                    :item-ids line-ids
                    :item-component line-component}
      [:svg {:class "graph__connection-container" :version "1.1"
             :baseProfile "full"
             :xmlns "http://www.w3.org/2000/svg"}
       (when-let [connector (<sub [:connector])]
         [connection connector])
       (for [[start end] npc-connections]
         ^{:key (str "line-connection:" start "->" end)}
         [npc-connection start end])
       (for [[start index end] player-connections]
         ^{:key (str "response-connection:" start ":" index "->" end)}
         [player-connection start index end])]]]
    [:span "No dialogue selected!"]))
