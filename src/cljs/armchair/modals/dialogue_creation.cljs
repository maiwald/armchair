(ns armchair.modals.dialogue-creation
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [clojure.string :refer [blank?]]
            [armchair.config :as config]
            [armchair.slds :as slds]
            [armchair.input :as input]
            [armchair.modals.events :refer [assert-no-open-modal
                                            build-modal-assertion]]
            [armchair.math :as math]
            [armchair.util :as u :refer [<sub >evt e->val]]
            [armchair.events :refer [reg-event-data reg-event-meta]]))

;; Events

(def assert-dialogue-creation-modal
  (build-modal-assertion :dialogue-creation))

(reg-event-meta
  ::open
  (fn [db _]
    (assert-no-open-modal db)
    (assoc-in db [:modal :dialogue-creation]
              {:character-id nil
               :synopsis nil})))

(reg-event-meta
  ::update
  (fn [db [_ field value]]
    (assert-dialogue-creation-modal db)
    (assoc-in db [:modal :dialogue-creation field] value)))

(reg-event-data
  ::save
  (fn [db]
    (assert-dialogue-creation-modal db)
    (let [dialogue-id (random-uuid)
          line-id (random-uuid)
          modal-data (get-in db [:modal :dialogue-creation])]
      (if (or (blank? (:character-id modal-data))
              (blank? (:synopsis modal-data)))
        db
        (-> db
            (assoc-in [:dialogues dialogue-id] (merge modal-data
                                                      {:entity/id dialogue-id
                                                       :entity/type :dialogue
                                                       :initial-line-id line-id}))
            (update :ui/positions assoc
                    dialogue-id config/default-ui-position
                    line-id (math/translate-point config/default-ui-position
                                                  (+ config/line-width 60) 0))
            (assoc-in [:lines line-id] {:entity/id line-id
                                        :entity/type :line
                                        :kind :npc
                                        :character-id (:character-id modal-data)
                                        :dialogue-id dialogue-id
                                        :text ""})
            (dissoc :modal))))))

;; Views

(defn modal [{:keys [character-id synopsis]}]
  [slds/modal {:title "Create Dialogue"
               :confirm-handler #(>evt [::save])
               :close-handler #(>evt [:close-modal])}
   [:<>
    [input/select {:label "Character *"
                   :on-change #(>evt [::update :character-id (uuid (e->val %))])
                   :options (<sub [:character-options])
                   :value character-id}]
    [input/textarea {:label "Synopsis *"
                     :on-change #(>evt [::update :synopsis (e->val %)])
                     :value synopsis}]]])
