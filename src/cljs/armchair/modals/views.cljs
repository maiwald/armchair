(ns armchair.modals.views
  (:require [armchair.components :as c]
            [armchair.input :as input]
            [armchair.util :as u :refer [<sub >evt e->val]]
            [armchair.modals.character-form]
            [armchair.modals.dialogue-creation]
            [armchair.modals.trigger-creation]
            [armchair.modals.case-node-creation]
            [armchair.modals.location-creation]
            [armchair.modals.connection-trigger-creation]
            [armchair.modals.switch-form]
            [armchair.modals.unlock-conditions-form]
            [armchair.modals.sprite-selection]))

(defn npc-line-form-modal [line-id]
  (let [line (<sub [:dialogue/modal-line line-id])]
    [c/modal {:title "Character Line"
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


(defn modal []
  (when-let [modal (<sub [:modal])]
    (condp #(contains? %2 %1) modal
      :dialogue-creation           [armchair.modals.dialogue-creation/modal]
      :npc-line-id                 [npc-line-form-modal (:npc-line-id modal)]
      :character-form              [armchair.modals.character-form/modal (:character-form modal)]
      :location-creation           [armchair.modals.location-creation/modal (:location-creation modal)]
      :trigger-creation            [armchair.modals.trigger-creation/modal (:trigger-creation modal)]
      :case-node-creation          [armchair.modals.case-node-creation/modal (:case-node-creation modal)]
      :switch-form                 [armchair.modals.switch-form/modal]
      :unlock-conditions-form      [armchair.modals.unlock-conditions-form/modal]
      :connection-trigger-creation [armchair.modals.connection-trigger-creation/modal (:connection-trigger-creation modal)]
      :sprite-selection            [armchair.modals.sprite-selection/modal])))
