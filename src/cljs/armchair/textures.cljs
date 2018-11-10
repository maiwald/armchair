(ns armchair.textures
  (:require [clojure.core.async :refer [go chan take! put! <!]]))

(def background-textures [:wall
                          :stairs
                          :red_wall-top-left
                          :red_wall-top
                          :red_wall_top-right
                          :red_wall-left
                          :red_wall-center
                          :red_wall-right
                          :red_wall-bottom-left
                          :red_wall-bottom
                          :red_wall-bottom-right
                          :house_arch_top
                          :house_bottom_left
                          :house_bottom_right
                          :house_door_bottom
                          :house_roof_middle
                          :house_roof_top
                          :house_roof_bottom-left
                          :house_roof_bottom-right
                          :house_roof_bottom
                          :house_roof_bottom2
                          :house_roof_middle-left
                          :house_roof_middle-right
                          :house_roof_top-left
                          :house_roof_top-right
                          :dirt
                          :grass_dirt_bottom-left
                          :grass_dirt_bottom
                          :grass_dirt_bottom-right
                          :grass_dirt_left
                          :grass_dirt_right
                          :grass_dirt_top-left
                          :grass_dirt_top
                          :grass_dirt_top-right
                          :grass
                          :grass_stone_bottom-left
                          :grass_stone_bottom
                          :grass_stone_bottom-right
                          :grass_stone_left
                          :grass_stone_right
                          :grass_stone_top-left
                          :grass_stone_top
                          :grass_stone_top-right
                          :stone_2
                          :stone_bush
                          :stone_grass_bottom-left
                          :stone_grass_bottom-right
                          :stone_grass_top-left
                          :stone_grass_top-right
                          :stone])

(def character-textures [:acid_blob :agent :agnes :aizul :angel :antaeus :asmodeus :azrael :azure_jelly :big_kobold :blork_the_orc :boggart :boris :brown_ooze :centaur :centaur-melee :centaur_warrior :centaur_warrior-melee :cerebov :crazy_yiuf :cyclops :daeva :dead_squirrel_idle :death_drake :deep_elf_annihilator :deep_elf_blademaster :deep_elf_conjurer :deep_elf_death_mage :deep_elf_demonologist :deep_elf_fighter :deep_elf_high_priest :deep_elf_knight :deep_elf_mage :deep_elf_master_archer :deep_elf_priest :deep_elf_soldier :deep_elf_sorcerer :deep_elf_summoner :deep_troll :deformed_elf :deformed_human :deformed_orc :demonspawn :dispater :dissolution :donald :dragon :droog_idle :duane :dwarf :edmund :elf :enchantress :ereshkigal :erica :erolcha :ettin :eustachio :eye_of_devastation :eye_of_draining :fire_drake :fire_giant :frances :francis :frederick :frost_giant :gastronok :geryon :giant_amoeba :giant_eyeball :giant_orange_brain :girl_idle :gloorx_vloq :glowing_shapeshifter :gnoll :goblin :golden_dragon :golden_eye :goth_idle :great_orb_of_eyes :greater_naga :griffon :grinder :grinder_cleaver :grum :guardian_serpent :gustav :halfling :harold :harpy :hell_knight :hi-tops_idle :hill_giant :hippogriff :hobgoblin :hugo :human :human_monk_ghost :human_slave :hydra1 :hydra2 :hydra3 :hydra4 :hydra5 :ice_beast :ice_dragon :ijyb :ilsuiw :ilsuiw_water :iron_dragon :iron_troll :iron_troll_monk_ghost :jelly :jessica :joseph :josephine :jozef :killer_klown :kirke :kobold :kobold_demonologist :lernaean_hydra :lindwurm :lom_lobon :louise :manticore :mara :margery :maud :maurice :menkaure :merfolk_aquamancer :merfolk_aquamancer_water :merfolk_fighter :merfolk_fighter_water :merfolk_impaler :merfolk_impaler_water :merfolk_javelineer :merfolk_javelineer_water :merfolk_plain :merfolk_plain_water :mermaid :mermaid_water :minotaur :mnoleg :mottled_dragon :murray :naga :naga_mage :naga_warrior :necromancer :nellie :nergalle :nessos :norbert :norris :ogre :ogre_mage :ooze :orb_guardian :orc :orc_high_priest :orc_knight :orc_priest :orc_sorcerer :orc_warlord :orc_warrior :orc_wizard :polyphemus :prince_ribbit :psyche :pulsating_lump :punker :purgy :quicksilver_dragon :rock_troll :rock_troll_monk_ghost :rourke :roxanne :royal_jelly :rupert :saint_roka :serpent_of_hell :shadow_dragon :shapeshifter :shining_eye :sigmund :siren :siren_water :snorg :sonja :sphinx :steam_dragon :stone_giant :storm_dragon :swamp_dragon :swamp_drake :terence :tiamat :titan :troll :two_headed_ogre :urug :vault_guard :wiglaf :wizard :wyvern :xtahua :yaktaur :yaktaur-melee :yaktaur_captain :yaktaur_captain-melee :yakuza_idle])

(def texture-set (set (-> [:player
                           :enemy
                           :arrow
                           :exit
                           :missing_texture]
                          (into background-textures)
                          (into character-textures))))

(defn texture-path [texture-name]
  (if (contains? texture-set texture-name)
    (str "images/" (name texture-name) ".png")
    (str "images/missing_texture.png")))

(defn load-textures [callback]
  (let [atlas (atom {})
        loaded (chan)]
    (run! (fn [texture-name]
            (let [image (js/Image.)]
              (set! (.-onload image) #(put! loaded [texture-name image]))
              (set! (.-src image) (texture-path texture-name))))
          texture-set)
    (take! (go
             (while (not= (count @atlas) (count texture-set))
               (let [[texture-name texture-image] (<! loaded)]
                 (swap! atlas assoc texture-name texture-image)))
             @atlas)
           callback)))
