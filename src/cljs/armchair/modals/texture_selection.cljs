(ns armchair.modals.texture-selection
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [armchair.textures :refer [image-path image-size sprite-sheets sprite-sheet-size tile-sprite-sheets]]
            [armchair.components :as c]
            [armchair.input :as input]
            [armchair.util :as u :refer [<sub >evt e->val]]
            [armchair.events :refer [reg-event-data reg-event-meta]]
            [armchair.modals.events :refer [assert-no-open-modal
                                            build-modal-assertion]]
            [armchair.components.tile-map :refer [sprite-select]]
            [armchair.location-editor.views :refer [tile-select-old]]))

;; Events

(def assert-texture-selection-modal
  (build-modal-assertion :texture-selection))

(reg-event-meta
  ::open
  (fn [db]
    (assert-no-open-modal db)
    (assoc-in db [:modal :texture-selection]
              (zipmap (list :file-name :tile)
                      (get-in db [:location-editor :active-texture])))))


(reg-event-meta
  ::update-file
  (fn [db [_ file-name]]
    (assert-texture-selection-modal db)
    (assoc-in db [:modal :texture-selection]
              {:file-name file-name})))

(reg-event-meta
  ::update-tile
  (fn [db [_ tile]]
    (assert-texture-selection-modal db)
    (assoc-in db [:modal :texture-selection :tile] tile)))

(reg-event-data
  ::save
  (fn [db]
    (assert-texture-selection-modal db)
    (let [{:keys [file-name tile]} (get-in db [:modal :texture-selection])]
      (cond-> db
        (and (some? file-name) (some? tile))
        (->
          (dissoc :modal)
          (update :location-editor merge {:active-texture [file-name tile]
                                          :active-tool :brush}))))))

;; Subscriptions

(reg-sub
  ::modal-data
  :<- [:modal]
  (fn [modal] (:texture-selection modal)))

;; Views

(def texture-options
  (->> tile-sprite-sheets
       (map :file-name)
       (sort)
       (map #(vector % %))))

(defn modal []
  (letfn [(close-modal [] (>evt [:close-modal]))
          (update-file [e] (>evt [::update-file (e->val e)]))
          (update-tile [tile] (>evt [::update-tile tile]))
          (save [] (>evt [::save]))]
    (fn []
      (let [{:keys [file-name tile]} (<sub [::modal-data])
            {:keys [gutter offset tile-size]} (sprite-sheets file-name)
            image-width (:w (sprite-sheet-size file-name))
            image-height (:h (sprite-sheet-size file-name))]
        [c/modal {:title "Select Texture"
                  :close-handler close-modal
                  :confirm-handler save}
         [input/select {:label "File"
                        :on-change update-file
                        :options texture-options
                        :value file-name}]
         [input/label "Texture"]
         [:div {:style {:overflow "scroll"
                        :background-color "#000"
                        :max-height (u/px 400)}}
          [:div.level {:style {:width image-width
                               :height image-height}}
           [:img {:src (image-path file-name)
                  :width image-width
                  :height image-height
                  :style {:max-width "none"
                          :display "block"
                          :image-rendering "pixelated"}}]
           [sprite-select {:on-select update-tile
                           :selected tile
                           :tile-size tile-size
                           :gutter gutter
                           :offset offset}]]]]))))
