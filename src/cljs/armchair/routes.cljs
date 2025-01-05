(ns armchair.routes
  (:require [re-frame.core :refer [dispatch-sync]]
            [bidi.bidi :refer [match-route path-for]]))

(def routes
  ["/" [["locations" :locations]
        [["locations/" :id "/edit"] :location-edit]
        [["dialogues/" :id "/edit"] :dialogue-edit]]])

(defn page-data [url]
  (let [{:keys [handler route-params]} (match-route routes url)]
    {:page-name handler
     :page-params route-params}))

(defn >navigate [& args]
  (let [url (apply path-for (into [routes] args))]
    (dispatch-sync [:close-inspector])
    (dispatch-sync [:show-page url])
    nil))
