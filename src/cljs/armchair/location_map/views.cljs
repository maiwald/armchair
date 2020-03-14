(ns armchair.location-map.views
  (:require [reagent.core :as r]
            [armchair.components :as c :refer [connection]]
            [armchair.input :as input]
            [armchair.math :as m]
            [armchair.util :as u :refer [stop-e! <sub >evt e->val e->left?]]
            [armchair.config :as config]
            [armchair.routes :refer [>navigate]]
            [goog.functions :refer [debounce]]))

(defn e->point [e]
  (m/Point. (.-clientX e) (.-clientY e)))

(defn location-inspector [location-id]
  (let [{:keys [display-name characters]} (<sub [:location-editor/location-inspector location-id])]
    [:div#inspector
     [:header
      [:span.title "Location"]
      [:a.close-button {:on-click #(>evt [:close-inspector])}
       [c/icon "times"]]]
     [:div.inspector__content
      [:div.inspector__property.inspector__property_inline
       [:span.inspector__property__title "Name"]
       [:div.inspector__property__payload
        [input/text
         {:on-change #(>evt [:location-editor/update-name location-id (e->val %)])
          :value display-name}]]]]
     [:div.inspector__actions
      [c/button {:title "Edit Objects"
                 :icon "map-marker-alt"
                 :fill true
                 :on-click #(>navigate :location-edit :id location-id)}]
      [c/button {:title "Edit Tilemap"
                 :icon "map"
                 :fill true
                 :on-click #(>navigate :location-edit :id location-id)}]]
     [:div.inspector__content
      [:div.inspector__property
       [:span.inspector__property__title "Characters"]
       [:div.inspector__property__payload
        (for [{:keys [character-id character-name]} characters]
          [:p {:key (str location-id ":" character-id)}
           character-name])]]]
     [:div.inspector__actions
      [c/button {:title "Delete Location"
                 :type :danger
                 :fill true
                 :on-click #(>evt [:delete-location location-id])}]]]))

(def map-scale 0.5)

(defn scroll-container []
  ; these don't need to be r/atoms because we dont need reactivity here
  (let [scroll-elem (atom nil)
        prev-cursor (atom nil)]
    (r/create-class
      {:display-name "scroll-container"
       :component-did-mount
       (fn [this]
         (if-let [{:keys [x y]} (:scroll-offset (r/props this))]
           (.scrollTo @scroll-elem x y)))
       :reagent-render
       (fn [{:keys [on-scroll dimensions]}]
         [:div
          {:ref #(reset! scroll-elem %)
           :style {:width "100%"
                   :height "100%"
                   :overflow "auto"}
           :on-scroll on-scroll
           :on-mouse-down (fn [e] (reset! prev-cursor (e->point e)))
           :on-mouse-move (fn [e]
                            (when (some? @prev-cursor)
                              (let [cursor (e->point e)
                                    [dx dy] (m/point-delta cursor @prev-cursor)]
                                (.scrollBy @scroll-elem dx dy)
                                (reset! prev-cursor cursor))))
           :on-mouse-up (fn [e] (reset! prev-cursor nil))}
          (into [:div
                 {:style {:min-width "100%"
                          :min-height "100%"
                          :width (u/px (:w dimensions))
                          :height (u/px (:h dimensions))}}]
                (r/children (r/current-component)))])})))

(defn drag-container []
  (let [dragging? (<sub [:dragging?])]
    (into [:div
           {:class ["graph" (if dragging? "graph_is-dragging")]
            :on-mouse-move (if dragging? #(>evt [:move-cursor (e->point %)]))}]
          (r/children (r/current-component)))))

(defn location [location-id ->position]
  (let [{:keys [dimension
                display-name
                preview-image-src
                inspecting?]} (<sub [:location-map/location location-id])
        scale (* config/tile-size map-scale)
        dragging? (<sub [:dragging-item? location-id])
        position (->position (<sub [:ui/position location-id]))
        start-dragging (fn [e]
                         (when (e->left? e)
                           (u/prevent-e! e)
                           (>evt [:start-dragging #{location-id} (e->point e)])))
        stop-dragging (when dragging?
                        (fn [e]
                          (u/prevent-e! e)
                          (>evt [:end-dragging])))]
    [:div {:class ["location"
                   "graph__item"
                   (when inspecting? "location_is-inspecting")
                   (when dragging? "graph__item_is-dragging")]
           :on-mouse-down u/stop-e!
           :style {:left (:x position) :top (:y position)}}
     [:div {:class "graph-node"}
      [:header {:class "graph-node__header"
                :on-mouse-down start-dragging
                :on-mouse-up stop-dragging}
       [:p {:class "graph-node__header__title"}
        [:a {:on-click #(>navigate :location-edit :id location-id)
             :on-mouse-down u/stop-e!}
         display-name]]]
      [:div {:class "graph-node__content"}
       (if (some? preview-image-src)
         [:img {:src preview-image-src
                :on-click #(>evt [:inspect :location location-id])
                :style {:width (u/px (* scale (:w dimension)))
                        :height (u/px (* scale (:h dimension)))}}])]]]))

(defn location-connection [dimensions start end]
  (let [start-pos (m/global-point (<sub [:ui/position start]) dimensions)
        end-pos (m/global-point (<sub [:ui/position end]) dimensions)]
    [connection {:start (m/translate-point start-pos (/ config/line-width 2) 15)
                 :end (m/translate-point end-pos (/ config/line-width 2) 15)}]))

(defn location-map []
  (let [{:keys [dimensions
                scroll-offset
                location-ids
                connections]} (<sub [:location-map map-scale])
        update-offset (debounce #(>evt [:location-map/update-offset %]) 200)
        on-scroll (fn [e]
                    (let [target (.-currentTarget e)
                          offset (m/Point.
                                   (.-scrollLeft target)
                                   (.-scrollTop target))]
                      (update-offset offset)))]
    [scroll-container {:dimensions dimensions
                       :scroll-offset scroll-offset
                       :on-scroll on-scroll}
     [drag-container
      (for [id location-ids]
        ^{:key (str "location:" id)}
        [location id (fn [position] (m/global-point position dimensions))])
      [:svg {:class "graph__connection-container" :version "1.1"
             :baseProfile "full"
             :xmlns "http://www.w3.org/2000/svg"}
       (for [[start end] connections]
         ^{:key (str "location-connection" start "->" end)}
         [location-connection dimensions start end])]]]))
