(ns armchair.api
  "API client for communicating with the backend database"
  (:require [cljs.core.async :as async :refer [go <! put!]]
            [cljs.core.async.interop :refer-macros [<p!]]))

(def ^:private api-base-url "http://localhost:5300/api")

(defn- fetch-json
  "Fetches JSON data from the given URL"
  [url]
  (go
    (try
      (let [response (<p! (js/fetch url))
            json (<p! (.json response))]
        (if (.-ok response)
          {:success true :data (js->clj json :keywordize-keys true)}
          {:success false :error "HTTP error" :status (.-status response)}))
      (catch js/Error e
        {:success false :error (.-message e)}))))

(defn fetch-locations
  "Fetches all locations from the backend"
  []
  (fetch-json (str api-base-url "/locations")))

(defn fetch-location
  "Fetches a specific location by ID"
  [location-id]
  (fetch-json (str api-base-url "/locations/" location-id)))

(defn fetch-characters
  "Fetches all characters from the backend"
  []
  (fetch-json (str api-base-url "/characters")))

(defn fetch-character
  "Fetches a specific character by ID"
  [character-id]
  (fetch-json (str api-base-url "/characters/" character-id)))

(defn health-check
  "Checks if the API is healthy"
  []
  (fetch-json (str api-base-url "/health")))