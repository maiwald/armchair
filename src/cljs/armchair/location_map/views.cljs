(ns armchair.location-map.views
  (:require [reagent.core :as r]
            [armchair.components :as c]
            [armchair.components.tile-map :refer [tile-select tile-dropzone]]
            [armchair.input :as input]
            [armchair.math :as m]
            [armchair.util :as u :refer [<sub >evt e->val e->left?]]
            [armchair.routes :refer [>navigate]]
            [goog.functions :refer [debounce]]))

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

(defn scroll-center-to-point [elem {:keys [x y]}]
  (let [max-x (- (.-scrollWidth elem) (.-clientWidth elem))
        max-y (- (.-scrollHeight elem) (.-clientHeight elem))]
    (.scrollTo elem
               (m/clamp 0 max-x (- x (/ (.-clientWidth elem) 2)))
               (m/clamp 0 max-y (- y (/ (.-clientHeight elem) 2))))))

(defn scroll-container []
  ; these don't need to be r/atoms because we dont need reactivity here
  (let [scroll-elem (atom nil)
        prev-cursor (atom nil)
        end-scrolling (fn [] (reset! prev-cursor nil))]
    (r/create-class
      {:display-name "scroll-container"
       :component-did-mount
       (fn [this]
         (scroll-center-to-point
           @scroll-elem
           (or (:scroll-center (r/props this))
               (m/Point. (/ (.-scrollWidth @scroll-elem) 2)
                         (/ (.-scrollHeight @scroll-elem) 2)))))
       :component-did-update
       (fn [this [_ {old-zoom-scale :zoom-scale}]]
         (let [{new-zoom-scale :zoom-scale
                center :scroll-center} (r/props this)]
           (when (not= old-zoom-scale new-zoom-scale)
             (scroll-center-to-point @scroll-elem center))))
       :reagent-render
       (fn [{:keys [on-scroll width height]}]
         [:div
          {:ref #(reset! scroll-elem %)
           :class "scroll-container"
           :on-scroll on-scroll
           :on-mouse-down (fn [e] (reset! prev-cursor (u/e->point e)))
           :on-mouse-move (fn [e]
                            (when (some? @prev-cursor)
                              (let [cursor (u/e->point e)
                                    [dx dy] (m/point-delta cursor @prev-cursor)]
                                (.scrollBy @scroll-elem dx dy)
                                (reset! prev-cursor cursor))))
           :on-mouse-leave end-scrolling
           :on-mouse-up end-scrolling}
          (into [:div
                 {:class "scroll-content"
                  :style {:width (u/px width)
                          :height (u/px height)}}]
                (r/children (r/current-component)))])})))

(defn drag-container []
  (let [dragging? (<sub [:dragging?])]
    (into [:div
           {:class ["drag-container" (when dragging? "drag-container_is-dragging")]
            :on-mouse-move (when dragging? #(>evt [:move-cursor (u/e->point %)]))}]
          (r/children (r/current-component)))))

(defn location [location-id]
  (let [{:keys [display-name
                characters
                preview-image-background-src
                preview-image-foreground-src
                preview-image-w
                preview-image-h
                zoom-scale
                bounds
                inspecting?]} (<sub [:location-map/location location-id])
        dragging? (<sub [:dragging-item? location-id])
        position (<sub [:location-map/location-position location-id])
        start-dragging (fn [e]
                         (when (e->left? e)
                           (u/prevent-e! e)
                           (>evt [:start-dragging #{location-id} (u/e->point e) zoom-scale])))
        stop-dragging (fn [e]
                        (when dragging?
                          (u/prevent-e! e)
                          (>evt [:end-dragging])))
        inspect-location #(>evt [:inspect :location location-id])]
    [:div {:class ["location"
                   (when inspecting? "location_is-inspecting")
                   (when dragging? "location_is-dragging")]
           :on-mouse-down u/stop-e!
           :style {:left (u/px (:x position))
                   :top (u/px (:y position))}}
     [:header {:class "location__header"
               :on-mouse-down (fn [e]
                                (inspect-location)
                                (start-dragging e))
               :on-mouse-up stop-dragging}
      [:p {:class "location__header__title"}
       display-name]]
     (if (and (some? preview-image-background-src)
              (some? preview-image-foreground-src))
       [:div {:class "location__tilemap"
              :style {:width (u/px preview-image-w)
                      :height (u/px preview-image-h)}}
        [:img {:src preview-image-background-src
               :style {:width (u/px preview-image-w)
                       :height (u/px preview-image-h)}}]
        (when (seq characters)
          [:div
           (for [[tile {:keys [texture display-name inspecting?]}] characters]
             [:div {:key (str "location-character:" location-id ",tile:" (pr-str tile))
                    :class ["location__tilemap__character" (when inspecting? "location__tilemap__character_is-inspecting")]
                    :style (u/tile-style tile zoom-scale)}
              [c/sprite-texture texture display-name zoom-scale]])])
        [:img {:src preview-image-foreground-src
               :style {:width (u/px preview-image-w)
                       :height (u/px preview-image-h)}}]
        (if-let [[dnd-type dnd-payload] (<sub [:ui/dnd])]
          (case dnd-type
            :character
            [tile-dropzone {:zoom-scale zoom-scale
                            :occupied (<sub [:location/occupied-tiles location-id])
                            :on-drop #(>evt [:location-editor/place-character location-id dnd-payload (m/relative-point % bounds)])}]
            nil)
          [tile-select {:zoom-scale zoom-scale
                        :on-select #(>evt [:inspect :tile location-id (m/relative-point % bounds)])}])]
       [:div {:class "location__loading"
              :style {:width (u/px preview-image-w)
                      :height (u/px preview-image-h)}}
        [c/spinner]])]))

(defn location-connection [[start-location start-tile]
                           [end-location end-tile]]
  (let [tile-size (<sub [:location-map/tile-size])
        {start-tile :tile start-tile-center :tile-center} (<sub [:location-map/connection-position start-location start-tile])
        {end-tile :tile end-tile-center :tile-center} (<sub [:location-map/connection-position end-location end-tile])]
    [:<>
     [:rect {:stroke "red"
             :stroke-width 1
             :fill "none"
             :x (:x start-tile)
             :y (:y start-tile)
             :height tile-size :width tile-size}]
     [:rect {:stroke "red"
             :stroke-width 1
             :fill "none"
             :x (:x end-tile)
             :y (:y end-tile)
             :height tile-size :width tile-size}]
     [:line {:class ["location-connection"]
             :x1 (:x start-tile-center)
             :y1 (:y start-tile-center)
             :x2 (:x end-tile-center)
             :y2 (:y end-tile-center)}]]))

(defn connections []
  (let [cs (<sub [:location-map/connections])]
    [:svg {:class "location-connections" :version "1.1"
           :baseProfile "full"
           :xmlns "http://www.w3.org/2000/svg"}
     (for [[start end] cs]
       ^{:key (str "location-connection" start "->" end)}
       [location-connection start end])]))


(defn e->scroll-center [e]
  (let [target (.-currentTarget e)]
    (m/Point. (+ (.-scrollLeft target) (/ (.-clientWidth target) 2))
              (+ (.-scrollTop target) (/ (.-clientHeight target) 2)))))

(defn location-map []
  (let [update-scroll-center (debounce #(>evt [:location-map/update-scroll-center %]) 200)
        on-scroll (comp update-scroll-center e->scroll-center)]
    (fn []
      (let [{:keys [bounds scroll-center location-ids zoom-scale]} (<sub [:location-map])]
        [scroll-container {:width (:w bounds)
                           :height (:h bounds)
                           :scroll-center scroll-center
                           :zoom-scale zoom-scale
                           :on-scroll on-scroll}
         [drag-container
          (for [id location-ids]
            ^{:key (str "location:" id)}
            [location id])
          [connections]]]))))
