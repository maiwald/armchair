(ns armchair.dialogue-editor.subs
  (:require [re-frame.core :refer [reg-sub]]
            [armchair.util :refer [where-map]]))

(reg-sub
  :dialogue-editor/npc-line
  :<- [:db-lines]
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[lines dialogues characters] [_ line-id]]
    (let [line (get lines line-id)
          character (get characters (:character-id line))
          dialogue (get dialogues (:dialogue-id line))]
      (merge (select-keys line [:text :info-ids])
             {:id line-id
              :initial-line? (= (:initial-line-id dialogue) line-id)
              :state (get-in dialogue [:states line-id])
              :character-color (:color character)
              :character-name (:display-name character)}))))

(reg-sub
  :dialogue-editor/player-line-options
  :<- [:db-lines]
  (fn [lines [_ line-id]]
    (get-in lines [line-id :options])))

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
