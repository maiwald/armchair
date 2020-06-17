(ns armchair.routes
  (:require [re-frame.core :refer [dispatch dispatch-sync]]
            [bidi.bidi :refer [match-route path-for]]))

(set! (.-onpopstate js/window)
      (fn [] (dispatch [:show-page (subs js/location.hash 1)])))

(def routes
  ["/" [["game" :game]
        ["locations" :locations]
        [["locations/" :id "/edit"] :location-edit]
        [["dialogues/" :id "/edit"] :dialogue-edit]]])

(def root (str "#" (path-for routes :locations)))

(defn page-data [url]
  (let [{:keys [handler route-params]} (match-route routes url)]
    {:page-name handler
     :page-params route-params}))

(defn >navigate [& args]
  (let [url (apply path-for (into [routes] args))]
    (js/history.pushState #js{} "" (str "#" url))
    (dispatch-sync [:close-inspector])
    (dispatch-sync [:show-page url])
    nil))
