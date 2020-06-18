(ns armchair.modals.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

(reg-sub
  :dialogue/modal-line
  :<- [:db-lines]
  :<- [:db-dialogues]
  (fn [[lines dialogues] [_ line-id]]
    (if-let [{id :entity/id :keys [dialogue-id] :as line} (get lines line-id)]
      (-> line
          (assoc :initial-line? (= id (get-in dialogues [dialogue-id :initial-line-id])))
          (assoc :option-count (count (:options line)))))))

