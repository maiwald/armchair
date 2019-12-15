(ns armchair.modals.views
  (:require [armchair.slds :as slds]
            [armchair.input :as input]
            [armchair.util :as u :refer [<sub >evt stop-e! e->val]]
            [armchair.modals.character-form]
            [armchair.modals.dialogue-creation]
            [armchair.modals.trigger-creation]
            [armchair.modals.case-node-creation]
            [armchair.modals.connection-trigger-creation]
            [armchair.modals.switch-form]
            [armchair.modals.unlock-conditions-form]
            [armchair.modals.texture-selection]))

(defn dialogue-state-modal [{:keys [line-id description]}]
  [slds/modal {:title "Dialogue State"
               :confirm-handler #(>evt [:create-dialogue-state])
               :close-handler #(>evt [:close-modal])}
   [input/text {:label "State description"
                :on-change #(>evt [:dialogue-state-update (e->val %)])
                :value description}]])

(defn npc-line-form-modal [line-id]
  (let [line (<sub [:dialogue/modal-line line-id])]
    [slds/modal {:title "Character Line"
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
      :dialogue-creation           [armchair.modals.dialogue-creation/modal]
      :dialogue-state              [dialogue-state-modal (:dialogue-state modal)]
      :npc-line-id                 [npc-line-form-modal (:npc-line-id modal)]
      :character-form              [armchair.modals.character-form/modal (:character-form modal)]
      :location-creation           [location-creation-modal (:location-creation modal)]
      :trigger-creation            [armchair.modals.trigger-creation/modal (:trigger-creation modal)]
      :case-node-creation          [armchair.modals.case-node-creation/modal (:case-node-creation modal)]
      :switch-form                 [armchair.modals.switch-form/modal]
      :unlock-conditions-form      [armchair.modals.unlock-conditions-form/modal]
      :connection-trigger-creation [armchair.modals.connection-trigger-creation/modal (:connection-trigger-creation modal)]
      :texture-selection           [armchair.modals.texture-selection/modal])))
