(ns armchair.dialogue-editor.subs
  (:require [re-frame.core :refer [reg-sub]]
            [armchair.util :refer [where-map map-values]]))

(reg-sub
  :dialogue-editor/npc-line
  :<- [:db-lines]
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[lines dialogues characters] [_ line-id]]
    (let [{:keys [text character-id dialogue-id next-line-id]} (get lines line-id)
          character (get characters character-id)
          {:keys [initial-line-id states]} (get dialogues dialogue-id)]
      {:id line-id
       :text text
       :initial-line? (= initial-line-id line-id)
       :state (get states line-id)
       :connected? (some? next-line-id)
       :character-color (:color character)
       :character-name (:display-name character)})))

(reg-sub
  :dialogue-editor/player-line-options
  :<- [:db-lines]
  :<- [:db-player-options]
  (fn [[lines player-options] [_ line-id]]
    (let [{:keys [options dialogue-id]} (lines line-id)]
      (mapv
        (fn [option-id]
          (let [{:keys [text next-line-id]} (player-options option-id)]
            {:text text
             :connected? (some? next-line-id)}))
        options))))

(reg-sub
  :dialogue-editor/trigger-node
  :<- [:db-lines]
  :<- [:db-dialogues]
  (fn [[lines dialogues] [_ trigger-id]]
    (let [{:keys [next-line-id triggers]} (lines trigger-id)]
      {:connected? (some? next-line-id)
       :triggers (map (fn [{:keys [kind id value]}]
                        {:switch-kind kind
                         :switch-id id
                         :switch-name (get-in dialogues [id :synopsis])
                         :switch-value (get-in dialogues [id :states value] "Initial Line")})
                      triggers)})))

(reg-sub
  :dialogue-editor/dialogue
  :<- [:db-lines]
  :<- [:db-player-options]
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[lines player-options dialogues characters positions] [_ dialogue-id]]
    (if-let [dialogue (get dialogues dialogue-id)]
      (let [dialogue-lines (where-map :dialogue-id dialogue-id lines)
            lines-by-kind (group-by :kind (vals dialogue-lines))]
        {:npc-line-ids (map :entity/id (lines-by-kind :npc))
         :player-line-ids (map :entity/id (lines-by-kind :player))
         :trigger-node-ids (map :entity/id (lines-by-kind :trigger))
         :line-connections (->> (concat (lines-by-kind :npc)
                                        (lines-by-kind :trigger))
                                (filter #(some? (:next-line-id %)))
                                (map #(vector (:entity/id %) (:next-line-id %))))
         :option-connections (reduce
                               (fn [acc {start :entity/id :keys [options]}]
                                 (apply conj acc (->> options
                                                      (map-indexed (fn [index option-id]
                                                                     (vector start
                                                                             index
                                                                             (get-in player-options [option-id :next-line-id]))))
                                                      (filter (fn [[_ _ end]] (some? end))))))
                               (list)
                               (lines-by-kind :player))}))))
