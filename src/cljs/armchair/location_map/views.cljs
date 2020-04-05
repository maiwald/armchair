(ns armchair.location-map.views
  (:require [reagent.core :as r]
            [armchair.components :as c]
            [armchair.input :as input]
            [armchair.math :as m]
            [armchair.util :as u :refer [<sub >evt e->val e->left?]]
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

(defn scroll-container []
  ; these don't need to be r/atoms because we dont need reactivity here
  (let [scroll-elem (atom nil)
        prev-cursor (atom nil)]
    (r/create-class
      {:display-name "scroll-container"
       :component-did-mount
       (fn [this]
         (if-let [{:keys [x y]} (:scroll-offset (r/props this))]
           (.scrollTo @scroll-elem x y)
           (.scrollTo @scroll-elem
                      (/ (- (.-scrollWidth @scroll-elem) (.-clientWidth @scroll-elem)) 2)
                      (/ (- (.-scrollHeight @scroll-elem) (.-clientHeight @scroll-elem)) 2))))
       :reagent-render
       (fn [{:keys [on-scroll width height]}]
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
           :on-mouse-up (fn [] (reset! prev-cursor nil))}
          (into [:div
                 {:style {:min-width "100%"
                          :min-height "100%"
                          :width (u/px width)
                          :height (u/px height)}}]
                (r/children (r/current-component)))])})))

(defn drag-container []
  (let [dragging? (<sub [:dragging?])]
    (into [:div
           {:class ["drag-container" (when dragging? "drag-container_is-dragging")]
            :on-mouse-move (when dragging? #(>evt [:move-cursor (e->point %)]))}]
          (r/children (r/current-component)))))

(defn location [location-id]
  (let [{:keys [display-name
                preview-image-background-src
                preview-image-foreground-src
                preview-image-w
                preview-image-h
                zoom-scale
                inspecting?]} (<sub [:location-map/location location-id])
        dragging? (<sub [:dragging-item? location-id])
        position (<sub [:location-map/location-position location-id])
        start-dragging (fn [e]
                         (when (e->left? e)
                           (u/prevent-e! e)
                           (>evt [:start-dragging #{location-id} (e->point e) zoom-scale])))
        stop-dragging (fn [e]
                        (when dragging?
                          (u/prevent-e! e)
                          (>evt [:end-dragging])))
        inspect-location #(>evt [:inspect :location location-id])]
    [:div {:class ["location"
                   (when inspecting? "location_is-inspecting")
                   (when dragging? "location_is-dragging")]
           :on-mouse-down u/stop-e!
           :style {:left (:x position) :top (:y position)}}
     [:header {:class "location__header"
               :on-mouse-down (fn [e]
                                (inspect-location)
                                (start-dragging e))
               :on-mouse-up stop-dragging}
      [:p {:class "location__header__title"}
       display-name]]
     (when (and (some? preview-image-background-src)
                (some? preview-image-foreground-src))
       [:div {:class "location__preview"
              :style {:width (u/px preview-image-w)
                      :height (u/px preview-image-h)}}
        [:img {:src preview-image-background-src
               :style {:width (u/px preview-image-w)
                       :height (u/px preview-image-h)}}]
        [:img {:src preview-image-foreground-src
               :style {:width (u/px preview-image-w)
                       :height (u/px preview-image-h)}}]])]))

(defn location-connection [[start-location start-offset]
                           [end-location end-offset]]
  (let [start-pos (<sub [:location-map/location-position start-location])
        end-pos (<sub [:location-map/location-position end-location])]
    [:line {:class ["location-connection"]
            :x1 (+ (:x start-pos) (:x start-offset))
            :y1 (+ (:y start-pos) (:y start-offset))
            :x2 (+ (:x end-pos) (:x end-offset))
            :y2 (+ (:y end-pos) (:y end-offset))}]))

(defn connections []
  (let [cs (<sub [:location-map/connections])]
    [:svg {:class "location-connections" :version "1.1"
           :baseProfile "full"
           :xmlns "http://www.w3.org/2000/svg"}
     (for [[start end] cs]
       ^{:key (str "location-connection" start "->" end)}
       [location-connection start end])]))

(defn location-map []
  (let [update-offset (debounce #(>evt [:location-map/update-offset %]) 200)
        on-scroll (fn [e]
                    (let [target (.-currentTarget e)
                          offset (m/Point.
                                   (.-scrollLeft target)
                                   (.-scrollTop target))]
                      (update-offset offset)))]
    (fn []
      (let [{:keys [bounds scroll-offset location-ids]} (<sub [:location-map])]
        [scroll-container {:width (:w bounds)
                           :height (:h bounds)
                           :scroll-offset scroll-offset
                           :on-scroll on-scroll}
         [drag-container
          (for [id location-ids]
            ^{:key (str "location:" id)}
            [location id])
          [connections]]]))))
