(ns armchair.components.sidebar
  (:require [armchair.components :as c]))

(defn sidebar [{:keys [panels active-panel on-panel-change on-panel-close]}]
  [:aside.sidebar
   [:ul.panel-selectors
    (for [[panel-key {:keys [icon label]}] panels
          :let [active? (= panel-key active-panel)]]
      [:li.panel-selector {:key (str "sidebar-panel-selector-" panel-key)
                           :class [(when active? "is-active")]
                           :on-click (if (= active-panel panel-key)
                                       on-panel-close
                                       #(on-panel-change panel-key))}
       [c/icon icon label {:fixed? true}]])]
   (when (contains? panels active-panel)
     [:div.panel (get-in panels [active-panel :component])])])
