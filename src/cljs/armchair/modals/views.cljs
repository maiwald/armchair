(ns armchair.modals.views
  (:require [armchair.slds :as slds]
            [armchair.input :as input]
            [armchair.components :as c :refer [icon]]
            [armchair.config :as config]
            [armchair.util :as u :refer [<sub >evt stop-e! e->val]]
            [armchair.textures :refer [character-textures]]
            [armchair.modals.trigger-creation]
            [armchair.modals.connection-trigger-creation]
            [armchair.modals.switch-form]
            [armchair.modals.unlock-conditions-form]))

(defn dialogue-creation-modal [{:keys [character-id synopsis]}]
  [slds/modal {:title "Create Dialogue"
               :confirm-handler #(>evt [:create-dialogue])
               :close-handler #(>evt [:close-modal])}
   [:<>
    [input/select {:label "Character *"
                   :on-change #(>evt [:dialogue-creation-update :character-id (uuid (e->val %))])
                   :options (<sub [:dialogue-creation/character-options])
                   :value character-id}]
    [input/textarea {:label "Synopsis *"
                     :on-change #(>evt [:dialogue-creation-update :synopsis (e->val %)])
                     :value synopsis}]]])

(defn dialogue-state-modal [{:keys [line-id description]}]
  [slds/modal {:title "Dialogue State"
               :confirm-handler #(>evt [:create-dialogue-state])
               :close-handler #(>evt [:close-modal])}
   [input/text {:label "State description"
                :on-change #(>evt [:dialogue-state-update (e->val %)])
                :value description}]])

(defn npc-line-form-modal [line-id]
  (let [line (<sub [:dialogue/modal-line line-id])]
    [slds/modal {:title "NPC Line"
                 :close-handler #(>evt [:close-modal])}
     [:<>
      [input/select {:label "Character"
                     :disabled (:initial-line? line)
                     :on-change #(>evt [:update-line line-id :character-id (uuid (e->val %))])
                     :options (<sub [:character-options])
                     :value (:character-id line)}]
      [input/textarea {:label "Text"
                       :on-change #(>evt [:update-line line-id :text (e->val %)])
                       :value (:text line)}]]]))

(defn character-form-modal [{:keys [display-name color texture]}]
  (let [update-handler (fn [field] #(>evt [:character-form/update field (e->val %)]))]
    [slds/modal {:title "Character"
                 :close-handler #(>evt [:close-modal])
                 :confirm-handler #(>evt [:character-form/save])}
     [input/text {:label "Name"
                  :on-change (update-handler :display-name)
                  :value display-name}]
     [input/text {:label "Color"
                  :on-change (update-handler :color)
                  :value color}]
     [:div {:class "color-picker"}
      (for [c config/color-grid]
        [:div {:key (str "color-picker-color:" c)
               :class ["color-picker__color"
                       (when (= c color) "color-picker__color_selected")]
               :on-click #(>evt [:character-form/update :color c])
               :style {:background-color c}}])]
     [input/select {:label "Avatar"
                    :options (mapv #(vector % %) character-textures)
                    :value texture
                    :on-change #(>evt [:character-form/update :texture (keyword (e->val %))])}]
     [c/sprite-texture texture]]))

(defn location-creation-modal [display-name]
  (let [update-name #(>evt [:update-location-creation-name (e->val %)])]
    [slds/modal {:title "Create Location"
                 :close-handler #(>evt [:close-modal])
                 :confirm-handler #(>evt [:create-location])}
     [input/text {:label "Name"
                  :on-change update-name
                  :value display-name}]]))


(defn modal []
  (if-let [modal (<sub [:modal])]
    (condp #(contains? %2 %1) modal
      :dialogue-creation           [dialogue-creation-modal (:dialogue-creation modal)]
      :dialogue-state              [dialogue-state-modal (:dialogue-state modal)]
      :npc-line-id                 [npc-line-form-modal (:npc-line-id modal)]
      :character-form              [character-form-modal (:character-form modal)]
      :location-creation           [location-creation-modal (:location-creation modal)]
      :trigger-creation            [armchair.modals.trigger-creation/modal (:trigger-creation modal)]
      :switch-form                 [armchair.modals.switch-form/modal]
      :unlock-conditions-form      [armchair.modals.unlock-conditions-form/modal]
      :connection-trigger-creation [armchair.modals.connection-trigger-creation/modal (:connection-trigger-creation modal)])))
