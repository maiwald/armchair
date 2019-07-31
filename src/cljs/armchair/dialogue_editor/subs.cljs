(ns armchair.dialogue-editor.subs
  (:require [re-frame.core :refer [reg-sub]]
            [armchair.config :as config]
            [armchair.util :as u]))

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
  :<- [:db-switches]
  :<- [:db-switch-values]
  (fn [[lines player-options switches switch-values] [_ line-id]]
    (let [{:keys [options dialogue-id]} (lines line-id)]
      (mapv
        (fn [option-id]
          (let [{:keys [text condition next-line-id]} (player-options option-id)]
            {:text text
             :conditions (map (fn [{:keys [switch-id operator switch-value-id]}]
                                {:switch (-> switch-id switches :display-name)
                                 :operator (-> operator config/condition-operators :display-name)
                                 :value (-> switch-value-id switch-values :display-name)})
                              (:terms condition))
             :condition-conjunction (-> condition
                                        :conjunction
                                        config/condition-conjunctions
                                        :display-name)
             :connected? (some? next-line-id)}))
        options))))

(reg-sub
  :dialogue-editor/trigger
  :<- [:db-triggers]
  :<- [:db-dialogues]
  :<- [:db-switches]
  :<- [:db-switch-values]
  (fn [[triggers dialogues switches switch-values] [_ trigger-id]]
    (let [{:keys [switch-kind switch-id switch-value]} (triggers trigger-id)]
      (merge
        {:switch-kind switch-kind
         :switch-id switch-id}
        (case switch-kind
          :dialogue-state
          {:switch-name (get-in dialogues [switch-id :synopsis])
           :switch-value (get-in dialogues [switch-id :states switch-value] "Initial Line")}
          :switch
          {:switch-name (get-in switches [switch-id :display-name])
           :switch-value (get-in switch-values [switch-value :display-name])})))))

(reg-sub
  :dialogue-editor/trigger-node
  :<- [:db-lines]
  (fn [lines [_ trigger-id]]
    (let [{:keys [next-line-id trigger-ids]} (lines trigger-id)]
      {:connected? (some? next-line-id)
       :trigger-ids trigger-ids})))

(reg-sub
  :dialogue-editor/dialogue
  :<- [:db-lines]
  :<- [:db-player-options]
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[lines player-options dialogues characters positions] [_ dialogue-id]]
    (if-let [dialogue (get dialogues dialogue-id)]
      (let [dialogue-lines (u/where-map :dialogue-id dialogue-id lines)
            lines-by-kind (group-by :kind (vals dialogue-lines))]
        {:npc-line-ids (map :entity/id (lines-by-kind :npc))
         :player-line-ids (map :entity/id (lines-by-kind :player))
         :trigger-node-ids (map :entity/id (lines-by-kind :trigger))
         :line-connections (->> (concat (lines-by-kind :npc)
                                        (lines-by-kind :trigger))
                                (filter #(contains? % :next-line-id))
                                (map #(vector (:entity/id %) (:next-line-id %))))
         :option-connections (mapcat
                               (fn [{start :entity/id :keys [options]}]
                                 (->> options
                                      (map-indexed
                                        (fn [index option-id]
                                          (if-let [end (get-in player-options [option-id :next-line-id])]
                                            (vector start index end))))
                                      (remove nil?)))
                               (lines-by-kind :player))}))))
