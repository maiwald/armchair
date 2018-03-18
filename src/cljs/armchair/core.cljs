(ns armchair.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [armchair.events]
            [armchair.subs]
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
  (dev-setup)
  (mount-root))
