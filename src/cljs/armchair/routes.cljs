(ns armchair.routes
  (:require [bidi.bidi :refer [path-for]]))

(def routes
  ["/" [["game" :game]
        ["locations" :locations]
        [["locations/" :id "/edit"] :location-edit]
        ["dialogues" :dialogues]
        [["dialogues/" :id "/edit"] :dialogue-edit]
        ["characters" :characters]
        ["infos" :infos]]])

(def root (str "#" (path-for routes :game)))
