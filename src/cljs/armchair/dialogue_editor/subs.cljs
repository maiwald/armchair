(ns armchair.dialogue-editor.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [armchair.config :as config]
            [armchair.math :as m]
            [armchair.util :as u]))

(reg-sub
  :dialogue-editor/npc-line
  :<- [:db-lines]
  :<- [:db-dialogues]
  :<- [:db-characters]
  (fn [[lines dialogues characters] [_ line-id]]
    (let [{:keys [text character-id dialogue-id next-line-id]} (get lines line-id)
          character (get characters character-id)
          {:keys [initial-line-id]} (get dialogues dialogue-id)]
      {:id line-id
       :text text
       :initial-line? (= initial-line-id line-id)
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
    (let [{:keys [options]} (lines line-id)]
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
  :<- [:db-switches]
  :<- [:db-switch-values]
  (fn [[triggers switches switch-values] [_ trigger-id]]
    (let [{:keys [switch-id switch-value]} (triggers trigger-id)]
      {:switch-id switch-id
       :switch-name (get-in switches [switch-id :display-name])
       :switch-value (get-in switch-values [switch-value :display-name])})))

(reg-sub
  :dialogue-editor/trigger-node
  :<- [:db-lines]
  (fn [lines [_ trigger-id]]
    (let [{:keys [next-line-id trigger-ids]} (lines trigger-id)]
      {:connected? (some? next-line-id)
       :trigger-ids trigger-ids})))

(reg-sub
  :dialogue-editor/case-node
  :<- [:db-lines]
  :<- [:db-switches]
  :<- [:db-switch-values]
  (fn [[lines switches switch-values] [_ case-node-id]]
    (let [{:keys [switch-id clauses]} (lines case-node-id)
          {:keys [display-name value-ids]} (switches switch-id)]
      {:switch-name display-name
       :clauses (->> value-ids
                     (map (fn [id]
                            {:display-name (get-in switch-values [id :display-name])
                             :connected? (contains? clauses id)
                             :switch-value-id id}))
                     (sort-by :display-name))})))

(reg-sub
  :dialogue-editor/initial-line
  (fn [[_ dialogue-id]]
    (subscribe [:db/dialogue dialogue-id]))
  (fn [{:keys [synopsis initial-line-id]}]
    {:synopsis synopsis
     :connected? (some? initial-line-id)}))

(reg-sub
  :dialogue-editor/header
  (fn [[_ dialogue-id]]
    (subscribe [:db/dialogue dialogue-id]))
  (fn [{:keys [synopsis]}]
    {:synopsis synopsis}))

(reg-sub
  :dialogue-editor/dialogue
  (fn [[_ dialogue-id]]
    [(subscribe [:db/dialogue dialogue-id])
     (subscribe [:db-lines])
     (subscribe [:db-player-options])
     (subscribe [:db-switches])
     (subscribe [:db-switch-values])
     (subscribe [:ui/positions])])
  (fn [[dialogue lines player-options switches switch-values positions] [_ dialogue-id]]
    (when (some? dialogue)
      (let [dialogue-lines (u/where-map :dialogue-id dialogue-id lines)
            lines-by-kind (group-by :kind (vals dialogue-lines))]
        {:bounds (m/rect-resize
                   (m/containing-rect
                     (map positions (conj (keys dialogue-lines) dialogue-id)))
                   {:left 100
                    :right (+ config/line-width 100)
                    :top 100
                    :bottom 400})
         :lines (map (fn [{:keys [entity/id kind]}] {:id id :kind kind})
                     (vals dialogue-lines))
         :initial-line-connection (when-let [end (:initial-line-id dialogue)]
                                    (vector dialogue-id end))
         :line-connections (->> (concat (lines-by-kind :npc)
                                        (lines-by-kind :trigger))
                                (filter #(contains? % :next-line-id))
                                (map #(vector (:entity/id %) (:next-line-id %))))
         :case-connections (mapcat
                             (fn [{start :entity/id :keys [clauses switch-id]}]
                               (let [value-ids (->> (get-in switches [switch-id :value-ids])
                                                    (sort-by #(get-in switch-values [% :display-name])))]
                                 (->> value-ids
                                      (map-indexed
                                        (fn [index value-id]
                                          (if-let [end (get clauses value-id)]
                                            (vector start index end))))
                                      (remove nil?))))
                             (lines-by-kind :case))
         :option-connections (mapcat
                               (fn [{start :entity/id :keys [options]}]
                                 (->> options
                                      (map-indexed
                                        (fn [index option-id]
                                          (if-let [end (get-in player-options [option-id :next-line-id])]
                                            (vector start index end))))
                                      (remove nil?)))
                               (lines-by-kind :player))}))))
