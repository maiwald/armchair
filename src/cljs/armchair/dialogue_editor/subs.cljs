(ns armchair.dialogue-editor.subs
  (:require [re-frame.core :refer [reg-sub]]
            [armchair.util :refer [where-map map-values]]))

(reg-sub
  :dialogue-editor/dialogue-states
  :<- [:db-dialogues]
  (fn [dialogues]
    (->> (vals dialogues)
         (map (fn [{:keys [states synopsis]}]
                (map-values #(str synopsis ": " %) states)))
         (apply merge))))

(reg-sub
  :dialogue-editor/npc-line
  :<- [:db-lines]
  :<- [:db-dialogues]
  :<- [:dialogue-editor/dialogue-states]
  :<- [:db-characters]
  :<- [:db-infos]
  (fn [[lines dialogues dialogue-states characters infos] [_ line-id]]
    (let [{:keys [text character-id dialogue-id info-ids next-line-id state-triggers]} (get lines line-id)
          character (get characters character-id)
          {:keys [initial-line-id states]} (get dialogues dialogue-id)
          dialogue-states (->> (vals dialogues)
                               (map (fn [{:keys [states synopsis]}]
                                      (map-values #(str synopsis ": " %) states)))
                               (apply merge))]
      {:id line-id
       :text text
       :infos (map #(get-in infos [% :description]) info-ids)
       :initial-line? (= initial-line-id line-id)
       :state (get states line-id)
       :state-triggers (vals (select-keys dialogue-states state-triggers))
       :connected? (some? next-line-id)
       :character-color (:color character)
       :character-name (:display-name character)})))

(reg-sub
  :dialogue-editor/player-line-options
  :<- [:db-lines]
  :<- [:db-player-options]
  :<- [:db-infos]
  :<- [:dialogue-editor/dialogue-states]
  (fn [[lines player-options infos dialogue-states] [_ line-id]]
    (let [{:keys [options dialogue-id]} (lines line-id)]
      (mapv
        (fn [option-id]
          (let [{:keys [text required-info-ids state-triggers next-line-id]} (player-options option-id)]
            {:text text
             :required-infos (map #(get-in infos [% :description]) required-info-ids)
             :state-triggers (vals (select-keys dialogue-states state-triggers))
             :connected? (some? next-line-id)}))
        options))))

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
         :npc-connections (->> (lines-by-kind :npc)
                               (remove #(nil? (:next-line-id %)))
                               (map #(vector (:entity/id %) (:next-line-id %))))
         :player-connections (reduce
                               (fn [acc {start :entity/id :keys [options]}]
                                 (apply conj acc (->> options
                                                      (map-indexed (fn [index option-id]
                                                                     (vector start
                                                                             index
                                                                             (get-in player-options [option-id :next-line-id]))))
                                                      (remove (fn [[_ _ end]] (nil? end))))))
                               (list)
                               (lines-by-kind :player))}))))
