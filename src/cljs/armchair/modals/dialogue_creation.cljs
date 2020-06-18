(ns armchair.modals.dialogue-creation
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [clojure.string :refer [blank?]]
            [armchair.config :as config]
            [armchair.components :as c]
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
  (fn [db [_ character-id location-id location-position]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :dialogue-creation]
              (cond-> {:character-id character-id
                       :synopsis nil}
                (some? location-id)
                (assoc :location-id location-id
                       :location-position location-position)))))

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
          {:keys [character-id synopsis location-id location-position]} (get-in db [:modal :dialogue-creation])]
      (if (or (blank? character-id) (blank? synopsis))
        db
        (-> (if (some? location-id)
              (assoc-in db [:locations location-id
                            :placements location-position
                            :dialogue-id] dialogue-id)
              db)
            (assoc-in [:dialogues dialogue-id] {:entity/id dialogue-id
                                                :entity/type :dialogue
                                                :character-id character-id
                                                :synopsis synopsis
                                                :initial-line-id line-id})
            (update :ui/positions assoc
                    dialogue-id config/default-ui-position
                    line-id (math/translate-point config/default-ui-position
                                                  (+ config/line-width 60) 0))
            (assoc-in [:lines line-id] {:entity/id line-id
                                        :entity/type :line
                                        :kind :npc
                                        :character-id character-id
                                        :dialogue-id dialogue-id
                                        :text ""})
            (dissoc :modal))))))

;; Subscriptions

(reg-sub
  ::modal-data
  :<- [:modal]
  (fn [modal-data]
    (if-let [{:keys [character-id synopsis location-id]}
             (:dialogue-creation modal-data)]
      {:character-id character-id
       :synopsis synopsis
       :character-changeable? (or (nil? location-id)
                                  (nil? character-id))})))

;; Views

(defn modal []
  (let [{:keys [character-id synopsis character-changeable?]} (<sub [::modal-data])]
    [c/modal {:title "Create Dialogue"
              :confirm-handler #(>evt [::save])
              :close-handler #(>evt [:close-modal])}
     [:<>
      [input/select {:label "Character *"
                     :on-change #(>evt [::update :character-id (uuid (e->val %))])
                     :disabled (not character-changeable?)
                     :options (<sub [:character-options])
                     :value character-id}]
      [input/textarea {:label "Synopsis *"
                       :on-change #(>evt [::update :synopsis (e->val %)])
                       :value synopsis}]]]))
