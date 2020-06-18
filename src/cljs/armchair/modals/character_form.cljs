(ns armchair.modals.character-form
  (:require [clojure.spec.alpha :as s]
            [armchair.config :as config]
            [armchair.slds :as slds]
            [armchair.input :as input]
            [armchair.util :as u :refer [>evt e->val]]
            [armchair.math :as m]
            [armchair.events :refer [reg-event-data reg-event-meta]]
            [armchair.textures :refer [character-sprite-sheets]]
            [armchair.components :as c]
            [armchair.modals.events :refer [assert-no-open-modal build-modal-assertion]]))

;; Events

(def assert-character-modal
  (build-modal-assertion :character-form))

(reg-event-meta
  ::open
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

(reg-event-data
  ::update
  (fn [db [_ field value]]
    (assert-character-modal db)
    (assoc-in db [:modal :character-form field] value)))

(reg-event-data
  ::save
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

;; Views

(defn modal [{:keys [display-name color texture]}]
  (let [update-handler (fn [field] #(>evt [::update field (e->val %)]))]
    [slds/modal {:title "Character"
                 :close-handler #(>evt [:close-modal])
                 :confirm-handler #(>evt [::save])}
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
               :on-click #(>evt [::update :color c])
               :style {:background-color c}}])]
     [input/label "Avatar"]
     [:ul {:class "tile-grid"
           :style {:height "200px"}}
      (for [[file dimensions] character-sprite-sheets
            tile (-> dimensions
                     (m/rect-scale (/ 1 config/tile-size))
                     m/rect->point-seq)
            :let [new-texture [file tile]]]
        [:li {:key (str "avatar-select:" file ":" (pr-str tile))
              :class ["tile-grid__item"
                      (when (= new-texture texture) "tile-grid__item_active")]
              :style {:width (u/px config/tile-size)
                      :height (u/px config/tile-size)
                      :background-color "#fff"}}
         [:a {:on-click #(>evt [::update :texture new-texture])}
          [c/sprite-texture new-texture]]])]]))
