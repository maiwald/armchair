(ns armchair.modals.location-creation
  (:require [re-frame.core :as re-frame :refer [dispatch]]
            [armchair.config :as config]
            [armchair.slds :as slds]
            [armchair.input :as input]
            [armchair.util :as u :refer [>evt e->val]]
            [armchair.math :refer [Rect]]
            [armchair.events :refer [reg-event-data reg-event-meta]]
            [armchair.modals.events :refer [assert-no-open-modal
                                            build-modal-assertion]]))

;; Events

(def assert-location-creation-modal
  (build-modal-assertion :location-creation))

(reg-event-data
  ::create-location
  (fn [db]
    (assert-location-creation-modal db)
    (let [id (random-uuid)
          display-name (get-in db [:modal :location-creation])]
      (dispatch [:armchair.location-previews/generate-preview id])
      (-> db
          (dissoc :modal)
          (assoc :ui/inspector [:location {:location-id id}])
          (assoc-in [:ui/positions id] config/default-ui-position)
          (assoc-in [:locations id] {:entity/id id
                                     :entity/type :location
                                     :bounds (Rect. 0 0 3 3)
                                     :background1 {}
                                     :background2 {}
                                     :foreground1 {}
                                     :foreground2 {}
                                     :blocked #{}
                                     :connection-triggers {}
                                     :placements {}
                                     :display-name display-name})))))

(reg-event-meta
  ::open
  (fn [db]
    (assert-no-open-modal db)
    (assoc-in db [:modal :location-creation] "")))

(reg-event-meta
  ::update-name
  (fn [db [_ display-name]]
    (assert-location-creation-modal db)
    (assoc-in db [:modal :location-creation] display-name)))

;; Views

(defn modal [display-name]
  (let [update-name #(>evt [::update-name (e->val %)])]
    [slds/modal {:title "Create Location"
                 :close-handler #(>evt [:close-modal])
                 :confirm-handler #(>evt [::create-location])}
     [input/text {:label "Name"
                  :on-change update-name
                  :value display-name}]]))
