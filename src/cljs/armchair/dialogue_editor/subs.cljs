(ns armchair.dialogue-editor.subs
  (:require [re-frame.core :refer [reg-sub]]
            [armchair.util :refer [where-map]]))

(reg-sub
  :dialogue-editor/npc-line
  :<- [:db-lines]
  :<- [:db-dialogues]
  :<- [:db-characters]
  :<- [:db-infos]
  (fn [[lines dialogues characters infos] [_ line-id]]
    (let [{:keys [text character-id dialogue-id info-ids next-line-id]} (get lines line-id)
          character (get characters character-id)
          {:keys [initial-line-id states]} (get dialogues dialogue-id)]
      {:id line-id
       :text text
       :infos (map #(get-in infos [% :description]) info-ids)
       :initial-line? (= initial-line-id line-id)
       :state (get states line-id)
       :connected? (some? next-line-id)
       :character-color (:color character)
       :character-name (:display-name character)})))

(reg-sub
  :dialogue-editor/player-line-options
  :<- [:db-lines]
  :<- [:db-infos]
  (fn [[lines infos] [_ line-id]]
    (mapv
      (fn [{:keys [text required-info-ids next-line-id]}]
        {:text text
         :required-infos (map #(get-in infos [% :description]) required-info-ids)
         :connected? (some? next-line-id)})
      (get-in lines [line-id :options]))))

(reg-sub
  :dialogue-editor/dialogue
  :<- [:db-lines]
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[lines dialogues characters positions] [_ dialogue-id]]
    (let [dialogue (get dialogues dialogue-id)
          dialogue-lines (where-map :dialogue-id dialogue-id lines)
          lines-by-kind (group-by :kind (vals dialogue-lines))]
      {:npc-line-ids (map :entity/id (lines-by-kind :npc))
       :player-line-ids (map :entity/id (lines-by-kind :player))
       :npc-connections (->> (lines-by-kind :npc)
                             (remove #(nil? (:next-line-id %)))
                             (map #(vector (:entity/id %) (:next-line-id %))))
       :player-connections (reduce
                             (fn [acc {start :entity/id :keys [options]}]
                               (apply conj acc (->> options
                                                    (map-indexed (fn [index {end :next-line-id}]
                                                                   (vector start index end)))
                                                    (remove (fn [[_ _ end]] (nil? end))))))
                             (list)
                             (lines-by-kind :player))})))
