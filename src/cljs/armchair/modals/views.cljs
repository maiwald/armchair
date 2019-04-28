(ns armchair.modals.views
  (:require [armchair.slds :as slds]
            [armchair.input :as input]
            [armchair.components :as c :refer [icon]]
            [armchair.config :as config]
            [armchair.util :as u :refer [<sub >evt stop-e! e->val]]
            [armchair.textures :refer [character-textures]]))

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

(defn trigger-creation-modal [{:keys [switch-kind switch-id switch-value]}]
  (let [{:keys [kind-options
                switch-options
                value-options]} (case switch-kind
                                  :dialogue-state
                                  (<sub [:trigger-creation/dialogue-state-options])
                                  :switch
                                  (<sub [:trigger-creation/switch-options]))]
    [slds/modal {:title "Add Trigger"
                 :close-handler #(>evt [:close-modal])
                 :confirm-handler #(>evt [:modal/save-trigger])}
     [slds/radio-button-group {:options [[:dialogue-state "Dialogue State"]
                                         [:switch "Switch"]]
                               :active switch-kind
                               :on-change #(>evt [:modal/update-trigger-kind %])}]
     [input/select {:label (case switch-kind
                             :dialogue-state "Dialogue"
                             :switch "Switch")
                    :options switch-options
                    :value switch-id
                    :disabled (nil? switch-kind)
                    :on-change #(>evt [:modal/update-trigger-switch-id (uuid (e->val %))])}]
     [input/select {:label "Value"
                    :options value-options
                    :value switch-value
                    :disabled (nil? switch-id)
                    :on-change #(>evt [:modal/update-trigger-value (uuid (e->val %))])}]]))

(defn switch-form-modal []
  (let [{:keys [display-name values]} (<sub [:modal/switch-form])]
    [slds/modal {:title "Switch"
                 :close-handler #(>evt [:close-modal])
                 :confirm-handler #(>evt [:modal/save-switch])}
     [input/text {:label "Name"
                  :on-change #(>evt [:modal/update-switch-name (e->val %)])
                  :value display-name}]
     (for [[index value-name] values]
       ^{:key (str "switch-value" index)}
       [:div
        [input/text {:label "Value"
                     :on-change #(>evt [:modal/update-switch-value index (e->val %)])
                     :value value-name}]
        [:a {:on-mouse-down stop-e!
             :on-click #(>evt [:modal/remove-switch-value index])}
         [icon "times-circle"]]])
     [c/button {:title "Add Option"
                :icon "plus"
                :on-click #(>evt [:modal/add-switch-value])}]]))

(defn conditions-form-term [index {:keys [switch-id switch-options
                                          operator operator-options
                                          switch-value-id switch-value-options]}]
  (letfn [(update-switch [e]
            (>evt [:modal/update-condition-term-switch index (uuid (e->val e))]))
          (update-operator [e]
            (>evt [:modal/update-condition-term-operator index (keyword (e->val e))]))
          (update-value [e]
            (>evt [:modal/update-condition-term-value index (uuid (e->val e))]))]
    [:li {:class "condition-select"}
     [:div {:class "condition-select__switch"}
      [input/select {:on-change update-switch
                     :options switch-options
                     :value switch-id}]]
     [:div {:class "condition-select__operator"}
      [input/select {:on-change update-operator
                     :options operator-options
                     :value operator}]]
     [:div {:class "condition-select__value"}
      [input/select {:on-change update-value
                     :disabled (empty? switch-value-options)
                     :options switch-value-options
                     :value switch-value-id}]]
     [:div {:class "condition-select__delete"}
      [:a {:on-click #(>evt [:modal/remove-condition-term index])}
       [icon "trash" "Delete condition"]]]]))

(defn conditions-form-modal []
  (letfn [(close-modal [e] (>evt [:close-modal]))
          (save-conditions [e] (>evt [:modal/save-condition]))
          (add-condition [e] (>evt [:modal/add-condition-term]))
          (update-conjunction [e]
            (>evt [:modal/update-condition-conjunction (keyword (e->val e))]))]
    (fn []
      (let [{:keys [terms conjunction]} (<sub [:modal/conditions-form])]
        [slds/modal {:title "Unlock Conditions"
                     :close-handler close-modal
                     :confirm-handler save-conditions}
         (when (< 1 (count terms))
           [input/select {:on-change update-conjunction
                               :options (u/map-values :display-name config/condition-conjunctions)
                               :value conjunction}])
         [:ul
          (for [[index term] terms]
            ^{:key (str "condition-select" index)}
            [conditions-form-term index term])]
         [c/button {:title "Add Condition"
                    :icon "plus"
                    :on-click add-condition}]]))))

(defn modal []
  (if-let [modal (<sub [:modal])]
    (condp #(contains? %2 %1) modal
      :dialogue-creation [dialogue-creation-modal (:dialogue-creation modal)]
      :dialogue-state    [dialogue-state-modal (:dialogue-state modal)]
      :npc-line-id       [npc-line-form-modal (:npc-line-id modal)]
      :character-form    [character-form-modal (:character-form modal)]
      :location-creation [location-creation-modal (:location-creation modal)]
      :trigger-creation  [trigger-creation-modal (:trigger-creation modal)]
      :switch-form       [switch-form-modal]
      :conditions-form   [conditions-form-modal])))
