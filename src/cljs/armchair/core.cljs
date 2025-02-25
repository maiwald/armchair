(ns armchair.core
  (:require [reagent.dom :refer [render]]
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
            [armchair.location-map.events]
            [armchair.location-map.subs]
            [armchair.location-previews]
            [armchair.game.subs]
            [armchair.util :refer [>evt]]
            [armchair.views :as views]
            [armchair.config :as config]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (render [views/root]
          (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (re-frame/dispatch-sync [:load-storage-state])
  (dev-setup)
  (mount-root))

;; Undo/Redo key bindings

(set! (.-onkeyup js/window)
      (fn [e]
        (when (= "Escape" (.-code e))
          (>evt [:close-modal])
          (>evt [:cancel-dragging]))
        (when ^boolean (.-ctrlKey e)
          (condp = (.-code e)
            "KeyZ" (>evt [:undo])
            "KeyY" (>evt [:redo])
            nil))))
