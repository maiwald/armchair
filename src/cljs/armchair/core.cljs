(ns armchair.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [armchair.undo]
            [armchair.events]
            [armchair.subs]
            [armchair.modals.events]
            [armchair.modals.subs]
            [armchair.dialogue-editor.events]
            [armchair.dialogue-editor.subs]
            [armchair.location-editor.events]
            [armchair.location-editor.subs]
            [armchair.game.subs]
            [armchair.util :refer [>evt <sub]]
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
  (re-frame/dispatch-sync [:load-storage-state])
  (when (empty? js/location.hash)
    (js/history.replaceState #js{} "" routes/root))
  (re-frame/dispatch-sync [:show-page (subs js/location.hash 1)])
  (dev-setup)
  (mount-root))

;; Undo/Redo key bindings

(set! (.-onkeyup js/window)
      (fn [e]
        (when (= "Escape" (.-code e))
          (>evt [:close-modal])
          (>evt [:cancel-dragging]))
        (when (.-ctrlKey e)
          (condp = (.-code e)
            "KeyZ" (>evt [:undo])
            "KeyY" (>evt [:redo])
            nil))))

;; Popover

(defn close-popover []
  (when (some? (<sub [:popover]))
    (re-frame/dispatch [:close-popover])))

(set! (.-onmouseup js/window) close-popover)
(set! (.-ondragstart js/window) close-popover)
