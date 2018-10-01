(ns armchair.routes
  (:require [bidi.bidi :refer [path-for]]))

(def routes
  ["/" [["game" :game]
        ["locations" :locations]
        [["locations/" :id "/edit"] :location-edit]
        [["dialogue/" :id] :dialogue]
        ["characters" :characters]
        ["infos" :infos]]])

(def root (str "#" (path-for routes :game)))
