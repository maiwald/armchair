(ns armchair.data-loader
  "Efficient data loading utilities with caching and async handling"
  (:require [cljs.core.async :as async :refer [go <!]]
            [re-frame.core :refer [dispatch]]
            [armchair.api :as api]))

(def ^:private cache (atom {}))
(def ^:private loading-states (atom #{}))

(defn- cache-key [type id]
  (if id
    [type id]
    [type :all]))

(defn- is-loading? [type id]
  (contains? @loading-states (cache-key type id)))

(defn- set-loading! [type id loading?]
  (let [key (cache-key type id)]
    (if loading?
      (swap! loading-states conj key)
      (swap! loading-states disj key))))

(defn- get-from-cache [type id]
  (get @cache (cache-key type id)))

(defn- set-cache! [type id data]
  (swap! cache assoc (cache-key type id) data))

(defn load-with-cache
  "Loads data with caching support, similar to React Query"
  ([type fetch-fn success-event]
   (load-with-cache type nil fetch-fn success-event))
  ([type id fetch-fn success-event]
   (let [cached-data (get-from-cache type id)]
     (if (and cached-data (not (is-loading? type id)))
       ;; Return cached data immediately
       (dispatch [success-event cached-data])
       ;; Load from API
       (when-not (is-loading? type id)
         (set-loading! type id true)
         (go
           (let [result (<! (fetch-fn))]
             (set-loading! type id false)
             (if (:success result)
               (let [data (:data result)]
                 (set-cache! type id data)
                 (dispatch [success-event data]))
               (js/console.error "Failed to load" type ":" (:error result))))))))))

;; Convenience functions for common use cases
(defn load-locations-cached []
  (load-with-cache :locations api/fetch-locations :locations-loaded))

(defn load-characters-cached []
  (load-with-cache :characters api/fetch-characters :characters-loaded))

(defn load-location-cached [location-id]
  (load-with-cache :location location-id 
                   #(api/fetch-location location-id) 
                   :location-loaded))

(defn load-character-cached [character-id]
  (load-with-cache :character character-id 
                   #(api/fetch-character character-id) 
                   :character-loaded))

;; Cache management
(defn clear-cache! 
  "Clear all cached data"
  []
  (reset! cache {}))

(defn invalidate-cache! 
  "Invalidate specific cache entries"
  [type id]
  (swap! cache dissoc (cache-key type id)))

(defn get-cache-stats
  "Get cache statistics for debugging"
  []
  {:cached-items (count @cache)
   :loading-items (count @loading-states)
   :cache-keys (keys @cache)})