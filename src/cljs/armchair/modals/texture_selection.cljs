(ns armchair.modals.texture-selection
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [armchair.config :as config]
            [armchair.textures :refer [image-path tile-sprite-sheets]]
            [armchair.slds :as slds]
            [armchair.input :as input]
            [armchair.util :as u :refer [<sub >evt e->val]]
            [armchair.math :as m]
            [armchair.events :refer [reg-event-data reg-event-meta]]
            [armchair.modals.events :refer [assert-no-open-modal
                                            build-modal-assertion]]
            [armchair.location-editor.views :refer [tile-select]]))

;; Events

(def assert-texture-selection-modal
  (build-modal-assertion :texture-selection))

(reg-event-meta
  ::open
  (fn [db]
    (assert-no-open-modal db)
    (assoc-in db [:modal :texture-selection]
              (zipmap (list :file :tile)
                      (get-in db [:location-editor :active-texture])))))


(reg-event-meta
  ::update-file
  (fn [db [_ file]]
    (assert-texture-selection-modal db)
    (assoc-in db [:modal :texture-selection]
              {:file file})))

(reg-event-meta
  ::update-tile
  (fn [db [_ tile]]
    (assert-texture-selection-modal db)
    (assoc-in db [:modal :texture-selection :tile] tile)))

(reg-event-data
  ::save
  (fn [db]
    (assert-texture-selection-modal db)
    (let [{:keys [file tile]} (get-in db [:modal :texture-selection])]
      (cond-> db
        (and (some? file) (some? tile))
        (->
          (dissoc :modal)
          (update :location-editor merge {:active-texture [file tile]
                                          :active-tool :brush}))))))

;; Subscriptions

(reg-sub
  ::modal-data
  :<- [:modal]
  (fn [modal] (:texture-selection modal)))

;; Views

(defn modal []
  (letfn [(close-modal [] (>evt [:close-modal]))
          (update-file [e] (>evt [::update-file (e->val e)]))
          (update-tile [tile] (>evt [::update-tile tile]))
          (save [] (>evt [::save]))]
    (fn []
      (let [{:keys [file tile]} (<sub [::modal-data])
            image-size (get tile-sprite-sheets file)]
        [slds/modal {:title "Select Texture"
                     :close-handler close-modal
                     :confirm-handler save}
         [input/select {:label "File"
                        :on-change update-file
                        :options (->> (keys tile-sprite-sheets)
                                      (map #(vector % %)))
                        :value file}]
         [input/label "Texture"]
         [:div {:style {:overflow "scroll"
                        :background-color "#000"
                        :max-width (u/px 600)
                        :max-height (u/px 400)}}
          [:div.level
           [:img {:src (image-path file)
                  :style {:max-width "none"
                          :display "block"}}]
           [:div.level-layer
            [tile-select {:bounds (m/rect-scale image-size (/ 1 config/tile-size))
                          :on-select update-tile
                          :selected tile}]]]]]))))
