(ns armchair.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [armchair.undo]
            [armchair.events]
            [armchair.subs]
            [armchair.dialogue-editor.events]
            [armchair.dialogue-editor.subs]
            [armchair.location-editor.events]
            [armchair.location-editor.subs]
            [armchair.game.subs]
            [armchair.util :refer [>evt]]
            [armchair.routes :as routes]
            [armchair.views :as views]
            [armchair.config :as config]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/root]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (when (empty? js/location.hash)
    (js/history.replaceState #js{} "" routes/root))
  (re-frame/dispatch-sync [:show-page (subs js/location.hash 1)])
  (dev-setup)
  (mount-root))

;; Undo/Redo key bindings
(set! (.-onkeypress js/window)
      (fn [e]
        (when (.-ctrlKey e)
          (condp = (.-code e)
            "KeyZ" (>evt [:undo])
            "KeyY" (>evt [:redo])))))

