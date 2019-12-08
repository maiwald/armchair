(ns armchair.modals.texture-selection
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [armchair.config :as config]
            [armchair.textures :refer [texture-path sprite-sheets sprite-lookup]]
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
  (fn [db [_ dialogue-id]]
    (assert-no-open-modal db)
    (let [[file coord] (sprite-lookup (get-in db [:location-editor :active-texture]))]
      (assoc-in db [:modal :texture-selection]
                {:file file
                 :tile (u/coord->tile coord)}))))


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
    (let [{:keys [file]
           {:keys [x y]} :tile} (get-in db [:modal :texture-selection])
          active-texture (get-in sprite-sheets [file :tiles [x y]])]
      (cond-> db
        (some? active-texture)
        (->
          (dissoc :modal)
          (assoc-in [:location-editor :active-texture] active-texture))))))

;; Subscriptions

(reg-sub
  ::modal-data
  :<- [:modal]
  (fn [modal] (:texture-selection modal)))

;; Views

(defn modal []
  (letfn [(close-modal [e] (>evt [:close-modal]))
          (update-file [e] (>evt [::update-file (keyword (e->val e))]))
          (update-tile [e] (>evt [::update-tile e]))
          (save [] (>evt [::save]))]
    (fn []
      (let [{:keys [file tile]} (<sub [::modal-data])
            image-size (get-in sprite-sheets [file :dimensions])]
        [slds/modal {:title "Select Texture"
                     :close-handler close-modal
                     :confirm-handler save}
         [input/select {:label "File"
                        :on-change update-file
                        :options (->> (keys sprite-sheets)
                                      (map #(vector % %)))
                        :value file}]
         [input/label "Texture"]
         [:div {:style {:overflow "scroll"
                        :background-color "#000"
                        :max-width (u/px 600)
                        :max-height (u/px 400)}}
          [:div.level
           [:img {:src (texture-path file)
                  :style {:max-width "none"
                          :display "block"}}]
           [:div.level-layer
            [tile-select {:dimension (m/rect-scale image-size (/ 1 config/tile-size))
                          :on-select update-tile
                          :selectable? (fn [{:keys [x y]}]
                                         (contains?
                                           (get-in sprite-sheets [file :tiles])
                                           [x y]))
                          :selected tile}]]]]]))))
