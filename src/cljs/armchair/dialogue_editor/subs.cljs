(ns armchair.dialogue-editor.subs
  (:require [re-frame.core :refer [reg-sub]]
            [armchair.util :refer [where-map]]))

(reg-sub
  :dialogue-editor/line
  :<- [:db-lines]
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[lines dialogues characters] [_ line-id]]
    (let [line (get lines line-id)
          character (get characters (:character-id line))
          dialogue (get dialogues (:dialogue-id line))]
      (merge (select-keys line [:kind :text :options :info-ids])
             {:id line-id
              :initial-line? (= (:initial-line-id dialogue) line-id)
              :character-color (:color character)
              :character-name (:display-name character)}))))

(reg-sub
  :dialogue-editor/dialogue
  :<- [:db-lines]
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[lines dialogues characters positions] [_ dialogue-id]]
    (let [dialogue (get dialogues dialogue-id)
          dialogue-lines (where-map :dialogue-id dialogue-id lines)
          lines-by-kind (group-by :kind (vals dialogue-lines))]
      {:line-ids (keys dialogue-lines)
       :npc-connections (->> (lines-by-kind :npc)
                             (remove #(nil? (:next-line-id %)))
                             (map #(vector (:entity/id %) (:next-line-id %))))
       :player-connections (reduce
                             (fn [acc {start :entity/id :keys [options]}]
                               (apply conj acc (->> options
                                                    (remove #(nil? (:next-line-id %)))
                                                    (map-indexed (fn [index {end :next-line-id}]
                                                                   (vector start index end))))))
                             (list)
                             (lines-by-kind :player))})))
