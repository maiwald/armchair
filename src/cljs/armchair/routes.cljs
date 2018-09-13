(ns armchair.routes
  (:require [bidi.bidi :refer [path-for]]))

(def routes
  ["/" [["game" :game]
        ["locations" :locations]
        [["dialogue/" :id] :dialogue]
        ["characters" :characters]
        ["infos" :infos]]])

(def root (str "#" (path-for routes :game)))
