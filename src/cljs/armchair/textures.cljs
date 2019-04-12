(ns armchair.textures
  (:require [clojure.core.async :refer [go chan take! put! <!]]
            [armchair.util :as u]
            [com.rpl.specter
             :refer [multi-path MAP-VALS]
             :refer-macros [select]]))

(def sprite-sheets
  (merge
    {:guy_sprite_sheet {[0 0] :guy_up_walking1
                        [1 0] :guy_up_idle
                        [2 0] :guy_up_walking2
                        [0 1] :guy_right_walking1
                        [1 1] :guy_right_idle
                        [2 1] :guy_right_walking2
                        [0 2] :guy_down_walking1
                        [1 2] :guy_down_idle
                        [2 2] :guy_down_walking2
                        [0 3] :guy_left_walking1
                        [1 3] :guy_left_idle
                        [2 3] :guy_left_walking2}
     :adobe {[3 2] :house_arch_top
             [0 4] :house_bottom_left
             [1 4] :house_bottom
             [2 4] :house_bottom_right
             [0 3] :house_middle-left
             [2 3] :house_middle-right
             [3 0] :house_window1
             [3 1] :house_window2
             [1 1] :house_roof_middle
             [0 2] :house_roof_bottom-left
             [1 2] :house_roof_bottom
             [2 2] :house_roof_bottom-right
             [1 3] :house_roof_bottom2
             [0 1] :house_roof_middle-left
             [2 1] :house_roof_middle-right
             [0 0] :house_roof_top-left
             [1 0] :house_roof_top
             [2 0] :house_roof_top-right
             [5 4] :brick_house_bottom-left
             [6 4] :brick_house_bottom
             [7 4] :brick_house_bottom-right
             [5 3] :brick_house_middle-left
             [6 3] :brick_house_middle
             [7 3] :brick_house_middle-right
             [5 1] :brick_house_roof_middle-left
             [6 1] :brick_house_roof_middle
             [7 1] :brick_house_roof_middle-right
             [5 0] :brick_house_roof_top-left
             [6 0] :brick_house_roof_top
             [7 0] :brick_house_roof_top-right
             [5 2] :brick_house_top-left
             [6 2] :brick_house_top
             [7 2] :brick_house_top-right
             [4 1] :brick_house_window1
             [4 0] :brick_house_window2
             [3 4] :house_door_bottom}
     :PathAndObjects_0 {[9 2] :grass_dirt_bottom-left
                        [10 2] :grass_dirt_bottom
                        [11 2] :grass_dirt_bottom-right
                        [9 1] :grass_dirt_left
                        [11 1] :grass_dirt_right
                        [9 0] :grass_dirt_top-left
                        [10 0] :grass_dirt_top
                        [11 0] :grass_dirt_top-right
                        [1 11] :grass
                        [0 2] :grass_stone_bottom-left
                        [1 2] :grass_stone_bottom
                        [2 2] :grass_stone_bottom-right
                        [0 1] :grass_stone_left
                        [2 1] :grass_stone_right
                        [0 0] :grass_stone_top-left
                        [1 0] :grass_stone_top
                        [2 0] :grass_stone_top-right
                        [10 1] :dirt
                        [1 1] :stone
                        [2 3] :stone_2
                        [2 4] :stone_bush
                        [0 4] :stone_grass_bottom-left
                        [1 4] :stone_grass_bottom-right
                        [0 3] :stone_grass_top-left
                        [1 3] :stone_grass_top-right}
     :misc {[21 0] :wall
            [20 0] :stairs
            [17 0] :red_wall-top-left
            [18 0] :red_wall-top
            [19 0] :red_wall_top-right
            [15 0] :red_wall-left
            [14 0] :red_wall-center
            [16 0] :red_wall-right
            [11 0] :red_wall-bottom-left
            [13 0] :red_wall-bottom
            [12 0] :red_wall-bottom-right
            [1 0] :dngn_closed_door
            [3 0] :dngn_unseen
            [2 0] :dngn_open_door
            [4 0] :gate_closed_left
            [5 0] :gate_closed_middle
            [6 0] :gate_closed_right
            [7 0] :gate_open_left
            [8 0] :gate_open_middle
            [9 0] :gate_open_right}
     :32x32_map_tile_v3.1 {[5 21] :water1
                           [3 21] :water2
                           [10 1] :wood_panel1
                           [10 2] :wood_panel2
                           [7 1] :wood_panel3}
     :characters {[0 0] :acid_blob
                  [1 0] :agent
                  [2 0] :agnes
                  [3 0] :aizul
                  [0 1] :angel
                  [1 1] :antaeus
                  [2 1] :asmodeus
                  [3 1] :azrael
                  [0 2] :azure_jelly
                  [1 2] :big_kobold
                  [2 2] :blork_the_orc
                  [3 2] :boggart
                  [0 3] :boris
                  [1 3] :brown_ooze
                  [2 3] :centaur
                  [3 3] :centaur-melee
                  [0 4] :centaur_warrior
                  [1 4] :centaur_warrior-melee
                  [2 4] :cerebov
                  [3 4] :crazy_yiuf
                  [0 5] :cyclops
                  [1 5] :daeva
                  [2 5] :dead_squirrel_idle
                  [3 5] :death_drake
                  [0 6] :deep_elf_annihilator
                  [1 6] :deep_elf_blademaster
                  [2 6] :deep_elf_conjurer
                  [3 6] :deep_elf_death_mage
                  [0 7] :deep_elf_demonologist
                  [1 7] :deep_elf_fighter
                  [2 7] :deep_elf_high_priest
                  [3 7] :deep_elf_knight
                  [0 8] :deep_elf_mage
                  [1 8] :deep_elf_master_archer
                  [2 8] :deep_elf_priest
                  [3 8] :deep_elf_soldier
                  [0 9] :deep_elf_sorcerer
                  [1 9] :deep_elf_summoner
                  [2 9] :deep_troll
                  [3 9] :deformed_elf
                  [0 10] :deformed_human
                  [1 10] :deformed_orc
                  [2 10] :demonspawn
                  [3 10] :dispater
                  [0 11] :dissolution
                  [1 11] :donald
                  [2 11] :dragon
                  [3 11] :droog_idle
                  [0 12] :duane
                  [1 12] :dwarf
                  [2 12] :edmund
                  [3 12] :elf
                  [0 13] :enchantress
                  [1 13] :ereshkigal
                  [2 13] :erica
                  [3 13] :erolcha
                  [0 14] :ettin
                  [1 14] :eustachio
                  [2 14] :eye_of_devastation
                  [3 14] :eye_of_draining
                  [0 15] :fire_drake
                  [1 15] :fire_giant
                  [2 15] :frances
                  [3 15] :francis
                  [0 16] :frederick
                  [1 16] :frost_giant
                  [2 16] :gastronok
                  [3 16] :geryon
                  [0 17] :giant_amoeba
                  [1 17] :giant_eyeball
                  [2 17] :giant_orange_brain
                  [3 17] :girl_idle
                  [0 18] :gloorx_vloq
                  [1 18] :glowing_shapeshifter
                  [2 18] :gnoll
                  [3 18] :goblin
                  [0 19] :golden_dragon
                  [1 19] :golden_eye
                  [2 19] :goth_idle
                  [3 19] :great_orb_of_eyes
                  [0 20] :greater_naga
                  [1 20] :griffon
                  [2 20] :grinder
                  [3 20] :grum
                  [0 21] :guardian_serpent
                  [1 21] :gustav
                  [2 21] :halfling
                  [3 21] :harold
                  [0 22] :harpy
                  [1 22] :hell_knight
                  [2 22] :hi-tops_idle
                  [3 22] :hill_giant
                  [0 23] :hippogriff
                  [1 23] :hobgoblin
                  [2 23] :hugo
                  [3 23] :human
                  [0 24] :human_monk_ghost
                  [1 24] :human_slave
                  [2 24] :hydra5
                  [3 24] :ice_beast
                  [0 25] :ice_dragon
                  [1 25] :ijyb
                  [2 25] :ilsuiw
                  [3 25] :ilsuiw_water
                  [0 26] :iron_dragon
                  [1 26] :iron_troll
                  [2 26] :iron_troll_monk_ghost
                  [3 26] :jelly
                  [0 27] :jessica
                  [1 27] :joseph
                  [2 27] :josephine
                  [3 27] :jozef
                  [0 28] :killer_klown
                  [1 28] :kirke
                  [2 28] :kobold
                  [3 28] :kobold_demonologist
                  [0 29] :lernaean_hydra
                  [1 29] :lindwurm
                  [2 29] :lom_lobon
                  [3 29] :louise
                  [0 30] :manticore
                  [1 30] :mara
                  [2 30] :margery
                  [3 30] :maud
                  [0 31] :maurice
                  [1 31] :menkaure
                  [2 31] :merfolk_aquamancer
                  [3 31] :merfolk_aquamancer_water
                  [0 32] :merfolk_fighter
                  [1 32] :merfolk_fighter_water
                  [2 32] :merfolk_impaler
                  [3 32] :merfolk_impaler_water
                  [0 33] :merfolk_javelineer
                  [1 33] :merfolk_javelineer_water
                  [2 33] :merfolk_plain
                  [3 33] :merfolk_plain_water
                  [0 34] :mermaid
                  [1 34] :mermaid_water
                  [2 34] :minotaur
                  [3 34] :mnoleg
                  [0 35] :mottled_dragon
                  [1 35] :murray
                  [2 35] :naga
                  [3 35] :naga_mage
                  [0 36] :naga_warrior
                  [1 36] :necromancer
                  [2 36] :nellie
                  [3 36] :nergalle
                  [0 37] :nessos
                  [1 37] :norbert
                  [2 37] :norris
                  [3 37] :ogre
                  [0 38] :ogre_mage
                  [1 38] :ooze
                  [2 38] :orb_guardian
                  [3 38] :orc
                  [0 39] :orc_high_priest
                  [1 39] :orc_knight
                  [2 39] :orc_priest
                  [3 39] :orc_sorcerer
                  [0 40] :orc_warlord
                  [1 40] :orc_warrior
                  [2 40] :orc_wizard
                  [3 40] :polyphemus
                  [0 41] :prince_ribbit
                  [1 41] :psyche
                  [2 41] :pulsating_lump
                  [3 41] :punker
                  [0 42] :purgy
                  [1 42] :quicksilver_dragon
                  [2 42] :rock_troll
                  [3 42] :rock_troll_monk_ghost
                  [0 43] :rourke
                  [1 43] :roxanne
                  [2 43] :royal_jelly
                  [3 43] :rupert
                  [0 44] :saint_roka
                  [1 44] :serpent_of_hell
                  [2 44] :shadow_dragon
                  [3 44] :shapeshifter
                  [0 45] :shining_eye
                  [1 45] :sigmund
                  [2 45] :siren
                  [3 45] :siren_water
                  [0 46] :snorg
                  [1 46] :sonja
                  [2 46] :sphinx
                  [3 46] :steam_dragon
                  [0 47] :stone_giant
                  [1 47] :storm_dragon
                  [2 47] :swamp_dragon
                  [3 47] :swamp_drake
                  [0 48] :terence
                  [1 48] :tiamat
                  [2 48] :titan
                  [3 48] :troll
                  [0 49] :two_headed_ogre
                  [1 49] :urug
                  [2 49] :vault_guard
                  [3 49] :wiglaf
                  [0 50] :wizard
                  [1 50] :wyvern
                  [2 50] :xtahua
                  [3 50] :yaktaur
                  [0 51] :yaktaur-melee
                  [1 51] :yaktaur_captain
                  [2 51] :yaktaur_captain-melee
                  [3 51] :yakuza_idle}}))

(def sprite-lookup
  (into {}
        (for [[file sprites] sprite-sheets
              [tile sprite] sprites]
          [sprite [file (u/tile->coord tile)]])))

(def texture-set (set (-> [:arrow
                           :exit
                           :missing_texture]
                          (into (keys sprite-sheets)))))

(def background-textures
  (sort (select [(multi-path :adobe
                             :misc
                             :PathAndObjects_0
                             :32x32_map_tile_v3.1)
                 MAP-VALS] sprite-sheets)))

(def character-textures
  (sort (select [:characters MAP-VALS] sprite-sheets)))

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
