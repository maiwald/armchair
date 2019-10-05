(ns armchair.modals.events
  (:require [clojure.spec.alpha :as s]
            [armchair.config :as config]
            [armchair.events :refer [reg-event-data reg-event-meta]]
            [armchair.math :refer [Rect]]
            [armchair.util :as u]))

(defn assert-no-open-modal [db]
  (assert (not (contains? db :modal))
          "Attempting to open a modal while modal is open!"))

(defn build-modal-assertion [modal-key]
  (fn [db event-name]
    (assert (contains? (:modal db) modal-key)
            (str "Modal " modal-key " not present."
                 (when event-name " Cannot execute " event-name "!")))))

;; specific Modals

(reg-event-meta
  :close-modal
  (fn [db [_ modal-fn | args]]
    (dissoc db :modal)))

(reg-event-meta
  :open-character-modal
  (fn [db [_ id]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :character-form]
              (if-let [{:keys [display-name color texture]} (get-in db [:characters id])]
                {:id id
                 :display-name display-name
                 :texture texture
                 :color color}
                {:display-name ""
                 :color (rand-nth config/color-grid)}))))

(defn assert-character-modal [db]
  (assert (contains? (:modal db) :character-form)
          "No character creation initiated. Cannot set value!"))

(reg-event-data
  :character-form/update
  (fn [db [_ field value]]
    (assert-character-modal db)
    (assoc-in db [:modal :character-form field] value)))

(reg-event-data
  :character-form/save
  (fn [db]
    (assert-character-modal db)
    (let [{:keys [id display-name texture color]} (get-in db [:modal :character-form])
          id (or id (random-uuid))
          character {:entity/id id
                     :entity/type :character
                     :display-name display-name
                     :texture texture
                     :color color}]
      (cond-> db
        (and (s/valid? :character/character character)
             (some? texture))
        (-> (assoc-in [:characters id] character)
            (dissoc :modal))))))

(reg-event-meta
  :open-location-creation
  (fn [db]
    (assert-no-open-modal db)
    (assoc-in db [:modal :location-creation] "")))

(defn assert-location-creation-modal [db]
  (assert (contains? (:modal db) :location-creation)
          "No Location creation modal present. Cannot create!"))

(reg-event-meta
  :update-location-creation-name
  (fn [db [_ display-name]]
    (assert-location-creation-modal db)
    (assoc-in db [:modal :location-creation] display-name)))

(reg-event-data
  :create-location
  (fn [db]
    (assert-location-creation-modal db)
    (let [id (random-uuid)
          display-name (get-in db [:modal :location-creation])]
      (-> db
          (dissoc :modal)
          (assoc-in [:ui/positions id] config/default-ui-position)
          (assoc-in [:locations id] {:entity/id id
                                     :entity/type :location
                                     :dimension (Rect. 0 0 3 3)
                                     :background1 {}
                                     :background2 {}
                                     :foreground1 {}
                                     :foreground2 {}
                                     :blocked #{}
                                     :connection-triggers {}
                                     :display-name display-name})))))

(reg-event-meta
  :open-npc-line-modal
  (fn [db [_ payload]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :npc-line-id] payload)))

;; Dialogue state modal

(reg-event-meta
  :open-dialogue-state-modal
  (fn [db [_ line-id]]
    (assert-no-open-modal db)
    (let [dialogue-id (get-in db [:lines line-id :dialogue-id])
          description (get-in db [:dialogues dialogue-id :states line-id])]
      (assoc-in db [:modal :dialogue-state] {:line-id line-id
                                             :description description}))))

(defn assert-dialogue-state-modal [db]
  (assert (contains? (:modal db) :dialogue-state)
          "No dialogue state modal open. Cannot set value!"))

(reg-event-meta
  :dialogue-state-update
  (fn [db [_ description]]
    (assert-dialogue-state-modal db)
    (assoc-in db [:modal :dialogue-state :description] description)))

(reg-event-data
  :create-dialogue-state
  (fn [db]
    (assert-dialogue-state-modal db)
    (let [{:keys [line-id description]} (get-in db [:modal :dialogue-state])
          dialogue-id (get-in db [:lines line-id :dialogue-id])]
      (cond-> (dissoc db :modal)
        (not-empty description)
        (update-in [:dialogues dialogue-id :states] assoc line-id description)))))
