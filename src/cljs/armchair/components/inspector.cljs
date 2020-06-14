(ns armchair.components.inspector
  (:require [reagent.core :as r]
            [armchair.util :as u :refer [<sub >evt e->val]]
            [armchair.components :as c]
            [armchair.routes :refer [>navigate]]
            [armchair.input :as input]
            [armchair.location-editor.views :refer [location-preview]]))

(defn property [{title :title inline? :inline}]
  [:div.inspector__property {:class (when inline? "inspector__property_inline")}
   [:span.inspector__property__title title]
   (into [:div.inspector__property__payload]
         (r/children (r/current-component)))])

(defn placement-inspector [location-id tile]
  (let [{:keys [location-display-name
                location-tile
                character-id
                character-display-name
                dialogue-id
                dialogue-options]}
        (<sub [:location-editor/placement-inspector location-id tile])]
    (letfn [(set-character [e]
              (>evt [:location-editor/set-placement-character
                     location-id tile (-> e e->val uuid)]))
            (set-dialogue [e]
              (>evt [:location-editor/set-placement-dialogue
                     location-id tile (uuid (e->val e))]))
            (unset-dialogue []
              (>evt [:location-editor/set-placement-dialogue
                     location-id tile nil]))]
      [:div#inspector
       [:header
        [:span.title "Character"]
        [:a.close-button {:on-click #(>evt [:close-inspector])}
         [c/icon "times"]]]
       [:div.inspector__content
        [property {:title "Placement"}
         (str
           location-display-name
           " [" (:x location-tile) "," (:y location-tile) "]")]
        [property {:title "Character"}
         [:a {:on-click #(>evt [:armchair.modals.character-form/open character-id])}
          character-display-name]]
        [property {:title "Dialogue"}
         [input/select {:value dialogue-id
                        :nil-value "No dialogue"
                        :options dialogue-options
                        :on-change set-dialogue}]
         [c/button {:title "Create a new dialogue"
                    :icon "plus"
                    :fill true
                    :on-click #(>evt [:armchair.modals.dialogue-creation/open character-id location-id tile])}]]]
       [:div.inspector__actions
        [c/button {:title "Clear tile"
                   :type :danger
                   :fill true
                   :on-click #(>evt [:location-editor/remove-placement location-id tile])}]]])))

(defn trigger-inspector [location-id tile]
  (let [{:keys [source-display-name
                source-position-normalized
                target-id
                target-display-name
                target-position
                target-position-normalized]}
        (<sub [:location-editor/trigger-inspector location-id tile])]
    [:div#inspector
     [:header
      [:span.title "Exit"]
      [:a.close-button {:on-click #(>evt [:close-inspector])}
       [c/icon "times"]]]
     [:div.inspector__content
      [property {:title "From"}
       (str
         source-display-name
         " [" (:x source-position-normalized)
         "," (:y source-position-normalized) "]")]
      [property {:title "To"}
       (str
         target-display-name
         " [" (:x target-position-normalized)
         "," (:y target-position-normalized) "]")]
      [property {:title "Preview"}
       [:div {:style {:margin "5px auto 0"}}
        [location-preview target-id target-position]]]]
     [:div.inspector__actions
      [c/button {:title "Remove Exit"
                 :type :danger
                 :fill true
                 :on-click #(>evt [:location-editor/remove-trigger location-id tile])}]]]))


(defn location-inspector [location-id]
  (let [{:keys [display-name characters]} (<sub [:location-editor/location-inspector location-id])]
    [:div#inspector
     [:header
      [:span.title "Location"]
      [:a.close-button {:on-click #(>evt [:close-inspector])}
       [c/icon "times"]]]
     [:div.inspector__content
      [property {:title "name" :inline true}
       [input/text
        {:on-change #(>evt [:location-editor/update-name location-id (e->val %)])
         :value display-name}]]]
     [:div.inspector__actions
      [c/button {:title "Edit Tilemap"
                 :icon "map"
                 :fill true
                 :on-click #(>navigate :location-edit :id location-id)}]]
     [:div.inspector__content
      [property {:title "Characters"}
       (for [{:keys [character-id character-name]} characters]
         [:p {:key (str location-id ":" character-id)}
          character-name])]]
     [:div.inspector__actions
      [c/button {:title "Delete Location"
                 :type :danger
                 :fill true
                 :on-click #(>evt [:delete-location location-id])}]]]))

(defn tile-inspector [location-id tile]
  (case (<sub [:location-editor/tile-type location-id tile])
    :placement [placement-inspector location-id tile]
    :trigger [trigger-inspector location-id tile]
    nil))

(defn inspector []
  (let [[inspector-type {:keys [location-id location-position]}] (<sub [:ui/inspector])]
    (case inspector-type
      :location [location-inspector location-id]
      :tile [tile-inspector location-id location-position])))
