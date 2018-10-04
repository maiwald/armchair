(ns armchair.routes
  (:require [re-frame.core :refer [dispatch]]
            [bidi.bidi :refer [path-for]]))

(set! (.-onpopstate js/window)
      (fn [e] (dispatch [:show-page (subs js/location.hash 1)])))

(def routes
  ["/" [["game" :game]
        ["locations" :locations]
        [["locations/" :id "/edit"] :location-edit]
        ["dialogues" :dialogues]
        [["dialogues/" :id "/edit"] :dialogue-edit]
        ["characters" :characters]
        ["infos" :infos]]])

(def root (str "#" (path-for routes :game)))

(defn >navigate [& args]
  (let [url (apply path-for (into [routes] args))]
    (js/history.pushState #js{} "" (str "#" url))
    (dispatch [:show-page url])))
