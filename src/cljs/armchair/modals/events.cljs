(ns armchair.modals.events
  (:require [armchair.events :refer [reg-event-data reg-event-meta]]))

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
  (fn [db]
    (dissoc db :modal)))

(reg-event-meta
  :open-npc-line-modal
  (fn [db [_ payload]]
    (assert-no-open-modal db)
    (assoc-in db [:modal :npc-line-id] payload)))
