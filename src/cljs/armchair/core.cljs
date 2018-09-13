(ns armchair.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [armchair.events]
            [armchair.subs]
            [armchair.routes :refer [root]]
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
    (js/history.replaceState #js{} "" root))
  (re-frame/dispatch-sync [:show-page (subs js/location.hash 1)])
  (dev-setup)
  (mount-root))
