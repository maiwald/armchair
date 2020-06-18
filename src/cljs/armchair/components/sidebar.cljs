(ns armchair.components.sidebar
  (:require [reagent.core :as r]
            [armchair.components :as c]))

(defn sidebar [{active :active-panel}]
  (let [active-panel (r/atom active)]
    (fn [{:keys [panels]}]
      [:aside.sidebar
       [:ul.panel-selectors
        (doall (for [[panel-key {:keys [icon label bottom?]}] panels
                     :let [active? (= panel-key @active-panel)]]
                [:li.panel-selector {:key (str "panel-selector-" panel-key)
                                     :class [(when active? "is-active")
                                             (when bottom? "is-bottom")]
                                     :on-click (fn []
                                                 (reset! active-panel
                                                         (if active? nil panel-key)))}
                 [c/icon icon label {:fixed? true}]]))]
       (when (contains? panels @active-panel)
         [:div.panel (get-in panels [@active-panel :component])])])))

