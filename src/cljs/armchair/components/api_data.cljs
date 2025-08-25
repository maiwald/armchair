(ns armchair.components.api-data
  "Components for displaying data loaded from the API"
  (:require [re-frame.core :refer [subscribe]]
            [armchair.components :as c]))

(defn locations-list
  "Displays list of locations from the API"
  []
  (let [locations @(subscribe [:db-locations])]
    [:div {:class "p-4"}
     [:h2 {:class "text-xl font-bold mb-4"} "Locations from Database"]
     (if (empty? locations)
       [:p {:class "text-gray-500"} "No locations loaded"]
       [:div {:class "space-y-2"}
        (for [[id location] locations]
          ^{:key id}
          [:div {:class "border p-3 rounded bg-white shadow-sm"}
           [:div {:class "font-semibold"} (:display-name location)]
           [:div {:class "text-sm text-gray-600"} 
            "ID: " (str id)]
           [:div {:class "text-sm text-gray-600"}
            "Type: " (str (:entity/type location))]
           (when (:bounds location)
             [:div {:class "text-sm text-gray-500"}
              "Bounds: " (str (:bounds location))])])])]))

(defn characters-list
  "Displays list of characters from the API"
  []
  (let [characters @(subscribe [:db-characters])]
    [:div {:class "p-4"}
     [:h2 {:class "text-xl font-bold mb-4"} "Characters from Database"]
     (if (empty? characters)
       [:p {:class "text-gray-500"} "No characters loaded"]
       [:div {:class "space-y-2"}
        (for [[id character] characters]
          ^{:key id}
          [:div {:class "border p-3 rounded bg-white shadow-sm"}
           [:div {:class "font-semibold"} (:display-name character)]
           [:div {:class "text-sm text-gray-600"} 
            "ID: " (str id)]
           [:div {:class "text-sm text-gray-600"}
            "Type: " (str (:entity/type character))]
           [:div {:class "text-sm"}
            "Color: " 
            [:span {:style {:background-color (:color character)
                            :padding "2px 6px"
                            :border-radius "3px"
                            :color "white"
                            :font-size "12px"}}
             (:color character)]]
           (when (:sprite character)
             [:div {:class "text-sm text-gray-500"}
              "Sprite: " (str (:sprite character))])])])]))

(defn api-data-view
  "Main component showing API data"
  []
  [:div {:class "flex flex-col h-full overflow-hidden"}
   [:div {:class "flex-1 overflow-y-auto"}
    [:div {:class "grid grid-cols-1 lg:grid-cols-2 gap-4 h-full"}
     [locations-list]
     [characters-list]]]])