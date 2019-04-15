(ns armchair.local-storage
  (:require [re-frame.core :refer [after]]
            [cognitect.transit :as t]
            [armchair.db :refer [serialize-db deserialize-db]]))

(def DB-KEY "armchair-data")
(def storage (.-localStorage js/window))

(defn set-data [value]
  (try (.setItem storage DB-KEY value)))

(defn get-data []
  (.getItem storage DB-KEY))

(def store
  (after #(set-data (serialize-db %))))
