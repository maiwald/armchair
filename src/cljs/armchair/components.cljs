(ns armchair.components
  (:require [clojure.string :refer [join]]
            [reagent.core :as r]
            [armchair.config :as config]
            [armchair.textures :refer [image-path image-size]]
            [armchair.math :as m :refer [abs point-delta translate-point]]
            [armchair.util :as u]))

;; SVG Connections

(defn connection [{:keys [kind start end]}]
  [:line {:class ["graph__connection"
                  (when (= kind :connector) "graph__connection_is-connector")]
          :x1 (:x start)
          :y1 (:y start)
          :x2 (:x end)
          :y2 (:y end)}])

(defn curved-connection [{:keys [kind start end]}]
  (let [[dx dy] (point-delta start end)
        m (translate-point start (/ dx 2) (/ dy 2))
        ctrl-x (if (pos? dx)
                 (+ (:x start)
                    (max (/ (abs dx) 6) 30))
                 (max (- (+ (:x start) (abs dx))
                         (/ (abs dx) 6))
                      (+ (:x start) 30)))
        ctrl-y (+ (:y start) (/ (- (:y m) (:y start)) 4))]
    [:path {:class ["graph__connection"
                    (when (= kind :connector) "graph__connection_is-connector")]
            :d (join " " (concat ["M" (:x start) (:y start)]
                                 (if (< 20 (abs dy))
                                   ["Q" ctrl-x ctrl-y (:x m) (:y m) "T" (:x end) (:y end)]
                                   ["L" (:x end) (:y end)])))}]))


;; Icon

(defn icon [glyph title options]
  (let [[title options] (if (map? title)
                          [nil title]
                          [title options])]
    [:i {:class [(str "fas fa-" glyph)
                 (when (:fixed? options) "fa-fw")]
         :title title}]))

(defn spinner []
  [:i {:class ["fas fa-spinner fa-spin"]}])

;; Tag

(defn tag [{glyph :icon :keys [title on-click on-remove]}]
  [:div.tag
   (when glyph [icon glyph])
   (if on-click
     [:a {:class "tag__description"
          :on-click on-click}
      title]
     [:span {:class "tag__description"}
      title])
   (when on-remove
     [:a {:class "tag__remove"
          :on-click on-remove}
      [icon "times-circle" "Delete state"]])])

;; Button

(defn button [{glyph :icon
               btn-type :type
               :keys [title on-click fill]
               :or {fill false}}]
  [:button {:class ["button"
                    (when fill "button_fill")
                    (when (= btn-type :danger) "button_danger")]
            :on-click on-click
            :type "button"}
   (when (some? glyph) [:div {:class "button__icon"} [icon glyph title]])
   (when (some? title) [:div {:class "button__title"} title])])

(defn icon-button [{glyph :icon
                    :keys [title on-click]}]
  [:button {:class ["icon-button"]
            :on-click on-click
            :type "button"}
   [icon glyph title {:fixed? true}]])

;; Sprite Texture

(defn sprite-texture [[file {:keys [x y]}] title zoom-scale]
  (let [{:keys [w h]} (image-size file)
        zoom-scale (or zoom-scale 1)]
    [:div.sprite-texture
     {:title title
      :style {:width (u/px (* zoom-scale config/tile-size))
              :height (u/px (* zoom-scale config/tile-size))
              :background-image (str "url(" (image-path file) ")")
              :background-size (str (u/px (* w zoom-scale))
                                    " "
                                    (u/px (* h zoom-scale)))
              :background-position (str (u/px (- (* zoom-scale config/tile-size x)))
                                        " "
                                        (u/px (- (* zoom-scale config/tile-size y))))}}]))

;; Tabs

(defn tabs [{:keys [items active on-change]}]
  [:ul {:class "tabs"}
   (for [[id title] items]
     [:li {:key (str (hash items) id)
           :class ["tabs__item"
                   (when (= id active) "is-active")]
           :on-click #(on-change id)}
      title])])

;; Scroll Container

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

