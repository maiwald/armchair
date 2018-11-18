(ns armchair.dummy-data)

(def dummy-data
  {:player {:location-id #uuid "121fb127-fbc8-44b9-ba62-2ca2517b6995" :location-position [2 11]} :dialogues {#uuid "c89f82cf-80f1-4cab-b31e-e95cc9699ac2" {:synopsis "Hugo's Dialogue", :character-id #uuid "c05f9046-a148-4001-9549-b0a3ea22d205", :initial-line-id #uuid "70285bef-32ab-4769-8314-2c46572a21bc", :location-id #uuid "121fb127-fbc8-44b9-ba62-2ca2517b6995", :entity/type :dialogue, :entity/id #uuid "c89f82cf-80f1-4cab-b31e-e95cc9699ac2", :location-position [7 11], :states {#uuid "d972a092-0990-4331-80af-b3fc889a6eb1" "Hugo is annoyed"}}, #uuid "4d5f7ff4-e987-4ef9-8881-b27b6f8898ae" {:synopsis "Gustav's Dialogue", :character-id #uuid "455e8d64-2345-4014-af28-ba4eff184af4", :initial-line-id #uuid "72e4a967-5561-4f06-b6a2-2eedc7961662", :location-id #uuid "121fb127-fbc8-44b9-ba62-2ca2517b6995", :entity/type :dialogue, :entity/id #uuid "4d5f7ff4-e987-4ef9-8881-b27b6f8898ae", :location-position [6 7]}, #uuid "ef35e0ba-b8df-4ea9-a7a5-14b68d1b1b5e" {:initial-line-id #uuid "6da79e65-57cf-413b-a40c-7671834bed41", :location-id #uuid "e5b01f7e-d6d1-4e29-8623-f0500b1e933a", :character-id #uuid "3834c55d-ba4c-4211-91ee-3887dd620076", :synopsis "Conni im Erdgeschoss", :entity/type :dialogue, :entity/id #uuid "ef35e0ba-b8df-4ea9-a7a5-14b68d1b1b5e", :location-position [-1 4]}, #uuid "25c2aec7-5274-453b-af6c-b4fec3383055" {:initial-line-id #uuid "2155a8a7-8a2c-4cdc-9a91-5ddd9f9523d7", :location-id #uuid "e5b01f7e-d6d1-4e29-8623-f0500b1e933a", :character-id #uuid "429550bd-ed60-4e2b-9d15-73b66adc959a", :synopsis "Grogg erzählt dir was", :entity/type :dialogue, :entity/id #uuid "25c2aec7-5274-453b-af6c-b4fec3383055", :location-position [-6 -2]}, #uuid "8ef966d8-b9fd-43d6-961a-f6e2eb93ea5b" {:initial-line-id #uuid "1a738242-06af-4131-a5d6-0fc3ecd89cb8", :location-id #uuid "8594a767-8036-409d-81eb-104c799cf26e", :character-id #uuid "cf9fe9f5-2bd8-44da-a113-97b4c271e1dc", :synopsis "Stay off the lawn!", :entity/type :dialogue, :entity/id #uuid "8ef966d8-b9fd-43d6-961a-f6e2eb93ea5b", :location-position [0 5]}, #uuid "ad38f19b-8b83-4192-b712-748ba236bee9" {:initial-line-id #uuid "f55e6520-4b1d-4a77-8b4e-3c3dfae1e5c6", :location-id #uuid "121fb127-fbc8-44b9-ba62-2ca2517b6995", :character-id #uuid "33e7e5d7-7619-43cb-abfb-979a638dbf27", :synopsis "Zeolith asks an important question", :entity/type :dialogue, :entity/id #uuid "ad38f19b-8b83-4192-b712-748ba236bee9", :location-position [17 8]}}, :characters {#uuid "c05f9046-a148-4001-9549-b0a3ea22d205" {:display-name "Hugo", :color "rgba(255, 0, 0, .6)", :texture :hugo, :entity/type :character, :entity/id #uuid "c05f9046-a148-4001-9549-b0a3ea22d205"}, #uuid "455e8d64-2345-4014-af28-ba4eff184af4" {:display-name "Gustav", :color "rgba(92, 154, 9, 0.8)", :texture :gustav, :entity/type :character, :entity/id #uuid "455e8d64-2345-4014-af28-ba4eff184af4"}, #uuid "3834c55d-ba4c-4211-91ee-3887dd620076" {:color "blue", :display-name "Conni", :texture :rourke, :entity/type :character, :entity/id #uuid "3834c55d-ba4c-4211-91ee-3887dd620076"}, #uuid "429550bd-ed60-4e2b-9d15-73b66adc959a" {:color "magenta", :display-name "Grog the Green", :texture :agent, :entity/type :character, :entity/id #uuid "429550bd-ed60-4e2b-9d15-73b66adc959a"}, #uuid "cf9fe9f5-2bd8-44da-a113-97b4c271e1dc" {:color "brown", :display-name "Lawn Monitor", :texture :goth_idle, :entity/type :character, :entity/id #uuid "cf9fe9f5-2bd8-44da-a113-97b4c271e1dc"}, #uuid "33e7e5d7-7619-43cb-abfb-979a638dbf27" {:color "grey", :display-name "Zeolith", :texture :dead_squirrel_idle, :entity/type :character, :entity/id #uuid "33e7e5d7-7619-43cb-abfb-979a638dbf27"}}, :ui/positions {#uuid "2155a8a7-8a2c-4cdc-9a91-5ddd9f9523d7" [177 101], #uuid "26f80b81-624b-45e2-ac60-e175c536eb96" [1807 363], #uuid "7083feaf-6d1b-4b07-8839-8a74f358ef56" [1108 342], #uuid "5ada4923-bdf4-4824-8f71-1616c20a4220" [819 259], #uuid "1a738242-06af-4131-a5d6-0fc3ecd89cb8" [99 144], #uuid "4943fa61-2ab4-427d-8374-f7f64c332e64" [779 238], #uuid "bb15ec59-73be-4f3f-b50e-da7ee9a404b2" [789 425], #uuid "6a184af8-d74f-4de2-a334-b4e365468627" [773 87], #uuid "dfd58d89-edc2-4d89-8b69-00a02619b928" [601 154], #uuid "e5b01f7e-d6d1-4e29-8623-f0500b1e933a" [515 103], #uuid "0a1f5277-9a57-453d-818a-a81dbfc50ec7" [1183 344], #uuid "45f08f2a-e092-4631-82af-b2c0c4a8d859" [180 152], #uuid "69ccf8a7-aefa-4f53-abdc-519f16e7a9bb" [407 151], #uuid "70dfa956-6636-4132-8409-e0db4ce021f5" [368 236], #uuid "0563e42e-192f-4bc1-8685-dddfe4441387" [501 82], #uuid "2ff562a4-810d-41ea-93ae-20d8b30c669f" [877 485], #uuid "c170a7b9-c559-430d-b925-9b2ab1c18314" [1470 140], #uuid "fc976991-b044-4144-b29f-8b16d135e6ba" [908 133], #uuid "373d56f5-4241-463a-b607-b603c131d582" [720 329], #uuid "72e4a967-5561-4f06-b6a2-2eedc7961662" [107 151], #uuid "4e51340d-3194-4e9c-b1a6-96fe4aa4ef16" [496 252], #uuid "1b2ae81c-90a8-496a-8a80-e02dc240030a" [707 151], #uuid "121fb127-fbc8-44b9-ba62-2ca2517b6995" [265 240], #uuid "81084b75-6d62-44c1-bc55-9a2316149f28" [466 103], #uuid "8594a767-8036-409d-81eb-104c799cf26e" [88 367], #uuid "ee2b5ed2-4087-4394-b2f5-73038bd723ce" [1808 106], #uuid "1fd72660-9648-4b52-b24d-5e22ad4ddcf7" [1444 552], #uuid "70285bef-32ab-4769-8314-2c46572a21bc" [42 190], #uuid "a79f1948-367c-442d-b82f-49d3386693ce" [-54 213], #uuid "5a6186c6-d480-40a0-8606-eae839cf8188" [1076 74], #uuid "92169461-360c-40ec-a981-51c66ccdd9d2" [490 417], #uuid "f55e6520-4b1d-4a77-8b4e-3c3dfae1e5c6" [-361 201], #uuid "62fa2808-e3b4-45f3-8146-e63867066db7" [1189 132], #uuid "c803660e-1afd-498a-9867-f10e78fa865a" [1444 389], #uuid "d972a092-0990-4331-80af-b3fc889a6eb1" [1475 346], #uuid "021a6be5-5d51-4492-811a-6298e719de35" [327 181], #uuid "ead89f38-23bd-4d8c-bb5d-f66e953cec9d" [1188 483], #uuid "6562c2f7-f629-4885-a065-7af9adf360ea" [1436 96], #uuid "6da79e65-57cf-413b-a40c-7671834bed41" [-251 163]}, :lines {#uuid "2155a8a7-8a2c-4cdc-9a91-5ddd9f9523d7" {:next-line-id #uuid "81084b75-6d62-44c1-bc55-9a2316149f28", :dialogue-id #uuid "25c2aec7-5274-453b-af6c-b4fec3383055", :entity/type :line, :entity/id #uuid "2155a8a7-8a2c-4cdc-9a91-5ddd9f9523d7", :kind :npc, :character-id #uuid "429550bd-ed60-4e2b-9d15-73b66adc959a", :text "Hey my friend. I see you finally got in the club."}, #uuid "26f80b81-624b-45e2-ac60-e175c536eb96" {:kind :player, :dialogue-id #uuid "25c2aec7-5274-453b-af6c-b4fec3383055", :options [{:text "My man!", :next-line-id nil}], :entity/type :line, :entity/id #uuid "26f80b81-624b-45e2-ac60-e175c536eb96"}, #uuid "7083feaf-6d1b-4b07-8839-8a74f358ef56" {:kind :player, :dialogue-id #uuid "25c2aec7-5274-453b-af6c-b4fec3383055", :options [{:text "I don't know. How many weapons does one need, in order to rob a bank?", :next-line-id #uuid "6562c2f7-f629-4885-a065-7af9adf360ea"} {:text "I don't know. I need a realiable weapon for hunting. ", :next-line-id #uuid "c803660e-1afd-498a-9867-f10e78fa865a"} {:text "Gimme that Glock!", :next-line-id #uuid "1fd72660-9648-4b52-b24d-5e22ad4ddcf7"}], :entity/type :line, :entity/id #uuid "7083feaf-6d1b-4b07-8839-8a74f358ef56"}, #uuid "5ada4923-bdf4-4824-8f71-1616c20a4220" {:kind :player, :dialogue-id #uuid "ef35e0ba-b8df-4ea9-a7a5-14b68d1b1b5e", :options [{:text "WWHHHY!", :next-line-id nil}], :entity/type :line, :entity/id #uuid "5ada4923-bdf4-4824-8f71-1616c20a4220"}, #uuid "1a738242-06af-4131-a5d6-0fc3ecd89cb8" {:next-line-id nil, :dialogue-id #uuid "8ef966d8-b9fd-43d6-961a-f6e2eb93ea5b", :entity/type :line, :entity/id #uuid "1a738242-06af-4131-a5d6-0fc3ecd89cb8", :kind :npc, :character-id #uuid "cf9fe9f5-2bd8-44da-a113-97b4c271e1dc", :text "Stay off the lawn!"}, #uuid "4943fa61-2ab4-427d-8374-f7f64c332e64" {:next-line-id nil, :dialogue-id #uuid "25c2aec7-5274-453b-af6c-b4fec3383055", :entity/type :line, :entity/id #uuid "4943fa61-2ab4-427d-8374-f7f64c332e64", :kind :npc, :character-id #uuid "429550bd-ed60-4e2b-9d15-73b66adc959a", :text "Oh, I see you pretentious fuck. Take a hike!"}, #uuid "bb15ec59-73be-4f3f-b50e-da7ee9a404b2" {:next-line-id #uuid "7083feaf-6d1b-4b07-8839-8a74f358ef56", :dialogue-id #uuid "25c2aec7-5274-453b-af6c-b4fec3383055", :entity/type :line, :entity/id #uuid "bb15ec59-73be-4f3f-b50e-da7ee9a404b2", :kind :npc, :character-id #uuid "429550bd-ed60-4e2b-9d15-73b66adc959a", :text "You said it good sir! Alright, how many weapons do you need?"}, #uuid "6a184af8-d74f-4de2-a334-b4e365468627" {:next-line-id #uuid "5a6186c6-d480-40a0-8606-eae839cf8188", :dialogue-id #uuid "25c2aec7-5274-453b-af6c-b4fec3383055", :entity/type :line, :entity/id #uuid "6a184af8-d74f-4de2-a334-b4e365468627", :kind :npc, :character-id #uuid "429550bd-ed60-4e2b-9d15-73b66adc959a", :text "You say it!"}, #uuid "dfd58d89-edc2-4d89-8b69-00a02619b928" {:next-line-id #uuid "fc976991-b044-4144-b29f-8b16d135e6ba", :info-ids #{#uuid "458cbae8-2bce-4309-a849-34108f3624c3"}, :dialogue-id #uuid "c89f82cf-80f1-4cab-b31e-e95cc9699ac2", :entity/type :line, :entity/id #uuid "dfd58d89-edc2-4d89-8b69-00a02619b928", :kind :npc, :character-id #uuid "c05f9046-a148-4001-9549-b0a3ea22d205", :text "I am Hugo. And you?"}, #uuid "0a1f5277-9a57-453d-818a-a81dbfc50ec7" {:next-line-id nil, :info-ids #{}, :dialogue-id #uuid "c89f82cf-80f1-4cab-b31e-e95cc9699ac2", :entity/type :line, :entity/id #uuid "0a1f5277-9a57-453d-818a-a81dbfc50ec7", :kind :npc, :character-id #uuid "c05f9046-a148-4001-9549-b0a3ea22d205", :state-triggers #{#uuid "d972a092-0990-4331-80af-b3fc889a6eb1"}, :text "Fine, be a jerk."}, #uuid "45f08f2a-e092-4631-82af-b2c0c4a8d859" {:kind :player, :dialogue-id #uuid "ef35e0ba-b8df-4ea9-a7a5-14b68d1b1b5e", :options [{:text "\"You nod, while you walk on by, raising your fist\"", :next-line-id #uuid "0563e42e-192f-4bc1-8685-dddfe4441387"} {:text "Hey, Ho! Thanks a Lot!", :next-line-id #uuid "4e51340d-3194-4e9c-b1a6-96fe4aa4ef16"} {:text "My Man!", :next-line-id #uuid "92169461-360c-40ec-a981-51c66ccdd9d2"}], :entity/type :line, :entity/id #uuid "45f08f2a-e092-4631-82af-b2c0c4a8d859"}, #uuid "69ccf8a7-aefa-4f53-abdc-519f16e7a9bb" {:dialogue-id #uuid "4d5f7ff4-e987-4ef9-8881-b27b6f8898ae", :kind :player, :options [{:text "Who are you?", :next-line-id #uuid "1b2ae81c-90a8-496a-8a80-e02dc240030a"}], :entity/type :line, :entity/id #uuid "69ccf8a7-aefa-4f53-abdc-519f16e7a9bb"}, #uuid "70dfa956-6636-4132-8409-e0db4ce021f5" {:next-line-id #uuid "373d56f5-4241-463a-b607-b603c131d582", :dialogue-id #uuid "ad38f19b-8b83-4192-b712-748ba236bee9", :entity/type :line, :entity/id #uuid "70dfa956-6636-4132-8409-e0db4ce021f5", :kind :npc, :character-id #uuid "33e7e5d7-7619-43cb-abfb-979a638dbf27", :text "\"Zeolith's hand hovers over the buttin. A satisfied smirk crosses his face.\""}, #uuid "0563e42e-192f-4bc1-8685-dddfe4441387" {:next-line-id nil, :dialogue-id #uuid "ef35e0ba-b8df-4ea9-a7a5-14b68d1b1b5e", :entity/type :line, :entity/id #uuid "0563e42e-192f-4bc1-8685-dddfe4441387", :kind :npc, :character-id #uuid "3834c55d-ba4c-4211-91ee-3887dd620076", :text "\"Conni raises his fist and bumbs your fist. \"Cool guy\" he mutters\""}, #uuid "2ff562a4-810d-41ea-93ae-20d8b30c669f" {:next-line-id #uuid "ead89f38-23bd-4d8c-bb5d-f66e953cec9d", :dialogue-id #uuid "c89f82cf-80f1-4cab-b31e-e95cc9699ac2", :entity/type :line, :entity/id #uuid "2ff562a4-810d-41ea-93ae-20d8b30c669f", :kind :npc, :character-id #uuid "c05f9046-a148-4001-9549-b0a3ea22d205", :text "Whaaaaaaaaaaat!?"}, #uuid "c170a7b9-c559-430d-b925-9b2ab1c18314" {:next-line-id nil, :info-ids #{}, :dialogue-id #uuid "c89f82cf-80f1-4cab-b31e-e95cc9699ac2", :entity/type :line, :entity/id #uuid "c170a7b9-c559-430d-b925-9b2ab1c18314", :kind :npc, :character-id #uuid "c05f9046-a148-4001-9549-b0a3ea22d205", :text "Anyway, ...bye!"}, #uuid "fc976991-b044-4144-b29f-8b16d135e6ba" {:dialogue-id #uuid "c89f82cf-80f1-4cab-b31e-e95cc9699ac2", :kind :player, :options [{:text "I am also Hugo! But for the sake of testing I keep talking way beyond what could possible fit into this box.", :next-line-id #uuid "62fa2808-e3b4-45f3-8146-e63867066db7"} {:text "That's none of your business!", :next-line-id #uuid "0a1f5277-9a57-453d-818a-a81dbfc50ec7"}], :entity/type :line, :entity/id #uuid "fc976991-b044-4144-b29f-8b16d135e6ba"}, #uuid "373d56f5-4241-463a-b607-b603c131d582" {:kind :player, :dialogue-id #uuid "ad38f19b-8b83-4192-b712-748ba236bee9", :options [{:text "WHHHYYY!", :next-line-id nil}], :entity/type :line, :entity/id #uuid "373d56f5-4241-463a-b607-b603c131d582"}, #uuid "72e4a967-5561-4f06-b6a2-2eedc7961662" {:next-line-id #uuid "69ccf8a7-aefa-4f53-abdc-519f16e7a9bb", :dialogue-id #uuid "4d5f7ff4-e987-4ef9-8881-b27b6f8898ae", :entity/type :line, :entity/id #uuid "72e4a967-5561-4f06-b6a2-2eedc7961662", :kind :npc, :character-id #uuid "455e8d64-2345-4014-af28-ba4eff184af4", :text "Yes?"}, #uuid "4e51340d-3194-4e9c-b1a6-96fe4aa4ef16" {:next-line-id #uuid "5ada4923-bdf4-4824-8f71-1616c20a4220", :dialogue-id #uuid "ef35e0ba-b8df-4ea9-a7a5-14b68d1b1b5e", :entity/type :line, :entity/id #uuid "4e51340d-3194-4e9c-b1a6-96fe4aa4ef16", :kind :npc, :character-id #uuid "3834c55d-ba4c-4211-91ee-3887dd620076", :text "The entry went up by 500 Gold just now"}, #uuid "1b2ae81c-90a8-496a-8a80-e02dc240030a" {:next-line-id nil, :dialogue-id #uuid "4d5f7ff4-e987-4ef9-8881-b27b6f8898ae", :entity/type :line, :entity/id #uuid "1b2ae81c-90a8-496a-8a80-e02dc240030a", :kind :npc, :character-id #uuid "455e8d64-2345-4014-af28-ba4eff184af4", :text "I am Gustav!"}, #uuid "81084b75-6d62-44c1-bc55-9a2316149f28" {:kind :player, :dialogue-id #uuid "25c2aec7-5274-453b-af6c-b4fec3383055", :options [{:text "Yea bitch, took a lot of work man!", :next-line-id #uuid "6a184af8-d74f-4de2-a334-b4e365468627"} {:text "Thank you kind sir, it was a hassle of epic proportions.", :next-line-id #uuid "4943fa61-2ab4-427d-8374-f7f64c332e64"} {:text "Fuck you!", :next-line-id #uuid "bb15ec59-73be-4f3f-b50e-da7ee9a404b2"}], :entity/type :line, :entity/id #uuid "81084b75-6d62-44c1-bc55-9a2316149f28"}, #uuid "ee2b5ed2-4087-4394-b2f5-73038bd723ce" {:kind :player, :dialogue-id #uuid "25c2aec7-5274-453b-af6c-b4fec3383055", :options [{:text "WHHHYY!", :next-line-id nil}], :entity/type :line, :entity/id #uuid "ee2b5ed2-4087-4394-b2f5-73038bd723ce"}, #uuid "1fd72660-9648-4b52-b24d-5e22ad4ddcf7" {:next-line-id #uuid "26f80b81-624b-45e2-ac60-e175c536eb96", :dialogue-id #uuid "25c2aec7-5274-453b-af6c-b4fec3383055", :entity/type :line, :entity/id #uuid "1fd72660-9648-4b52-b24d-5e22ad4ddcf7", :kind :npc, :character-id #uuid "429550bd-ed60-4e2b-9d15-73b66adc959a", :text "Alright player, calm done. Here you go."}, #uuid "70285bef-32ab-4769-8314-2c46572a21bc" {:next-line-id #uuid "021a6be5-5d51-4492-811a-6298e719de35", :dialogue-id #uuid "c89f82cf-80f1-4cab-b31e-e95cc9699ac2", :entity/type :line, :entity/id #uuid "70285bef-32ab-4769-8314-2c46572a21bc", :kind :npc, :character-id #uuid "c05f9046-a148-4001-9549-b0a3ea22d205", :text "Hey, who are you?"}, #uuid "a79f1948-367c-442d-b82f-49d3386693ce" {:kind :player, :dialogue-id #uuid "ad38f19b-8b83-4192-b712-748ba236bee9", :options [{:text "What the hell you talking bout?", :next-line-id #uuid "70dfa956-6636-4132-8409-e0db4ce021f5"} {:text "Yes, indeed…..quite.", :next-line-id #uuid "70dfa956-6636-4132-8409-e0db4ce021f5"} {:text "Man! Why you gotta push that button. Man. For Real!", :next-line-id #uuid "70dfa956-6636-4132-8409-e0db4ce021f5"}], :entity/type :line, :entity/id #uuid "a79f1948-367c-442d-b82f-49d3386693ce"}, #uuid "5a6186c6-d480-40a0-8606-eae839cf8188" {:kind :player, :dialogue-id #uuid "25c2aec7-5274-453b-af6c-b4fec3383055", :options [{:text "I need weapons for a bank heist!", :next-line-id #uuid "6562c2f7-f629-4885-a065-7af9adf360ea"}], :entity/type :line, :entity/id #uuid "5a6186c6-d480-40a0-8606-eae839cf8188"}, #uuid "92169461-360c-40ec-a981-51c66ccdd9d2" {:next-line-id nil, :dialogue-id #uuid "ef35e0ba-b8df-4ea9-a7a5-14b68d1b1b5e", :entity/type :line, :entity/id #uuid "92169461-360c-40ec-a981-51c66ccdd9d2", :kind :npc, :character-id #uuid "3834c55d-ba4c-4211-91ee-3887dd620076", :text "My Man!"}, #uuid "f55e6520-4b1d-4a77-8b4e-3c3dfae1e5c6" {:next-line-id #uuid "a79f1948-367c-442d-b82f-49d3386693ce", :dialogue-id #uuid "ad38f19b-8b83-4192-b712-748ba236bee9", :entity/type :line, :entity/id #uuid "f55e6520-4b1d-4a77-8b4e-3c3dfae1e5c6", :kind :npc, :character-id #uuid "33e7e5d7-7619-43cb-abfb-979a638dbf27", :text "Good morning fine Sir! It is a beautiful day to push the button isn't it?"}, #uuid "62fa2808-e3b4-45f3-8146-e63867066db7" {:next-line-id #uuid "c170a7b9-c559-430d-b925-9b2ab1c18314", :dialogue-id #uuid "c89f82cf-80f1-4cab-b31e-e95cc9699ac2", :entity/type :line, :entity/id #uuid "62fa2808-e3b4-45f3-8146-e63867066db7", :kind :npc, :character-id #uuid "c05f9046-a148-4001-9549-b0a3ea22d205", :text "What a strange coincidence! Two Hugos. Who would have thought."}, #uuid "c803660e-1afd-498a-9867-f10e78fa865a" {:next-line-id #uuid "26f80b81-624b-45e2-ac60-e175c536eb96", :dialogue-id #uuid "25c2aec7-5274-453b-af6c-b4fec3383055", :entity/type :line, :entity/id #uuid "c803660e-1afd-498a-9867-f10e78fa865a", :kind :npc, :character-id #uuid "429550bd-ed60-4e2b-9d15-73b66adc959a", :text "Here that this hunting rifle. It'll get the \"job\" done ;)"}, #uuid "d972a092-0990-4331-80af-b3fc889a6eb1" {:entity/id #uuid "d972a092-0990-4331-80af-b3fc889a6eb1", :entity/type :line, :kind :npc, :character-id #uuid "c05f9046-a148-4001-9549-b0a3ea22d205", :dialogue-id #uuid "c89f82cf-80f1-4cab-b31e-e95cc9699ac2", :text "I'm not talking to you anymore...", :next-line-id nil}, #uuid "021a6be5-5d51-4492-811a-6298e719de35" {:dialogue-id #uuid "c89f82cf-80f1-4cab-b31e-e95cc9699ac2", :kind :player, :options [{:text "I could ask you the same.", :next-line-id #uuid "dfd58d89-edc2-4d89-8b69-00a02619b928"} {:text "My name does not matter.", :next-line-id #uuid "0a1f5277-9a57-453d-818a-a81dbfc50ec7"} {:text "Silence! Hugo, you must come with me at once! The fate of the world is at stake.", :next-line-id #uuid "2ff562a4-810d-41ea-93ae-20d8b30c669f", :required-info-ids #{#uuid "458cbae8-2bce-4309-a849-34108f3624c3"}}], :entity/type :line, :entity/id #uuid "021a6be5-5d51-4492-811a-6298e719de35"}, #uuid "ead89f38-23bd-4d8c-bb5d-f66e953cec9d" {:next-line-id nil, :dialogue-id #uuid "c89f82cf-80f1-4cab-b31e-e95cc9699ac2", :entity/type :line, :entity/id #uuid "ead89f38-23bd-4d8c-bb5d-f66e953cec9d", :kind :npc, :character-id #uuid "c05f9046-a148-4001-9549-b0a3ea22d205", :text "How do you know my name!?"}, #uuid "6562c2f7-f629-4885-a065-7af9adf360ea" {:next-line-id #uuid "ee2b5ed2-4087-4394-b2f5-73038bd723ce", :dialogue-id #uuid "25c2aec7-5274-453b-af6c-b4fec3383055", :entity/type :line, :entity/id #uuid "6562c2f7-f629-4885-a065-7af9adf360ea", :kind :npc, :character-id #uuid "429550bd-ed60-4e2b-9d15-73b66adc959a", :text "Oh Jessus!"}, #uuid "6da79e65-57cf-413b-a40c-7671834bed41" {:next-line-id #uuid "45f08f2a-e092-4631-82af-b2c0c4a8d859", :dialogue-id #uuid "ef35e0ba-b8df-4ea9-a7a5-14b68d1b1b5e", :entity/type :line, :entity/id #uuid "6da79e65-57cf-413b-a40c-7671834bed41", :kind :npc, :character-id #uuid "3834c55d-ba4c-4211-91ee-3887dd620076", :text "Alright you can go in."}}, :infos {#uuid "458cbae8-2bce-4309-a849-34108f3624c3" {:description "Hugo's Name is Hugo", :entity/type :info, :entity/id #uuid "458cbae8-2bce-4309-a849-34108f3624c3"}}, :locations {#uuid "121fb127-fbc8-44b9-ba62-2ca2517b6995" {:connection-triggers {[5 6] #uuid "e5b01f7e-d6d1-4e29-8623-f0500b1e933a", [0 12] #uuid "8594a767-8036-409d-81eb-104c799cf26e"}, :background {[18 7] :stone_2, [13 2] :red_wall_top-right, [0 1] :wall, [18 13] :wall, [1 10] :stone_2, [14 0] :wall, [10 9] :stone_2, [6 8] :stone_grass_bottom-left, [13 11] :grass_stone_bottom, [6 9] :stone, [5 7] :stone_2, [18 8] :house_roof_top-right, [5 9] :stone_2, [21 9] :grass, [20 10] :wall, [0 10] :wall, [0 8] :wall, [16 13] :wall, [21 7] :grass, [9 1] :grass_dirt_top, [8 13] :wall, [4 3] :house_roof_top-left, [17 1] :grass, [18 9] :house_roof_bottom-right, [4 11] :grass_stone_bottom, [18 2] :grass, [14 10] :stone_grass_top-left, [3 4] :dirt, [1 2] :grass_dirt_left, [0 5] :wall, [0 11] :stone, [19 3] :wall, [0 0] :wall, [9 7] :grass, [5 12] :grass, [5 3] :house_roof_top, [24 7] :wall, [13 13] :wall, [23 8] :grass, [2 2] :dirt, [16 2] :wall, [22 4] :grass_stone_right, [1 5] :grass, [3 2] :dirt, [4 6] :house_bottom_left, [16 11] :house_bottom_left, [21 5] :wall, [8 12] :grass, [24 3] :wall, [9 11] :wall, [8 5] :house_roof_bottom-right, [13 10] :stone, [14 12] :grass, [1 11] :stone, [7 1] :grass_dirt_top-left, [0 7] :wall, [21 2] :stone_2, [7 9] :stone, [20 9] :grass, [2 4] :grass_dirt_bottom, [13 12] :grass, [23 3] :grass, [21 11] :stone, [2 12] :stone, [10 1] :grass_dirt_top, [23 7] :grass, [13 9] :stone, [22 2] :wall, [10 6] :grass, [7 2] :dirt, [4 2] :dirt, [21 0] :wall, [12 6] :grass, [4 10] :stone, [12 2] :red_wall-top-left, [24 9] :wall, [1 3] :grass_dirt_left, [10 11] :wall, [22 1] :grass_stone_right, [19 8] :wall, [5 5] :house_roof_bottom, [2 13] :wall, [5 6] :house_door_bottom, [2 3] :dirt, [15 12] :grass, [11 3] :wall, [8 1] :grass_dirt_top, [14 3] :grass, [18 12] :grass, [11 7] :grass, [0 13] :wall, [11 9] :stone_2, [23 1] :grass, [21 13] :wall, [0 9] :wall, [9 6] :grass, [15 3] :grass, [5 2] :grass_dirt_top, [16 7] :stone_2, [23 6] :grass, [9 3] :dirt, [9 0] :wall, [2 11] :stone, [12 0] :wall, [17 6] :grass_stone_top, [12 13] :wall, [4 7] :grass_stone_left, [8 3] :dirt, [3 1] :grass_dirt_top, [18 6] :stone_grass_bottom-right, [17 2] :grass, [14 11] :grass_stone_bottom-right, [22 9] :grass, [19 10] :wall, [18 11] :house_bottom_right, [9 13] :wall, [4 13] :wall, [21 10] :grass_stone_top, [7 0] :wall, [12 7] :grass, [17 9] :stairs, [20 6] :stone_2, [0 2] :wall, [15 2] :wall, [2 9] :wall, [19 9] :grass, [18 1] :grass, [4 9] :stone_grass_bottom-right, [12 9] :stone_2, [15 8] :stone_2, [24 4] :wall, [7 13] :wall, [5 1] :wall, [17 8] :house_roof_top, [20 4] :stone_2, [20 5] :stone_2, [19 4] :wall, [3 0] :wall, [14 2] :grass, [2 7] :house_roof_bottom-left, [12 12] :grass, [13 5] :house_door_bottom, [11 4] :wall, [6 13] :wall, [11 10] :stone, [15 4] :red_wall_top-right, [20 0] :stone_2, [21 8] :wall, [16 4] :grass, [22 10] :wall, [18 4] :grass, [13 8] :grass_stone_top, [0 6] :wall, [6 12] :grass, [8 8] :wall, [4 5] :house_roof_bottom-left, [16 3] :grass, [24 11] :wall, [20 8] :wall, [1 1] :grass_dirt_top-left, [15 7] :stone_grass_bottom-right, [23 9] :grass, [15 9] :stone_2, [23 13] :wall, [6 0] :wall, [15 11] :grass, [10 10] :stone, [12 4] :red_wall-left, [12 3] :red_wall-left, [10 7] :grass, [8 7] :grass, [12 5] :red_wall-bottom-left, [16 12] :grass, [9 5] :grass, [24 1] :wall, [19 5] :wall, [7 4] :house_roof_top-left, [11 12] :wall, [14 4] :red_wall-top-left, [1 12] :stone, [18 3] :grass, [14 13] :wall, [8 4] :house_roof_top-right, [13 1] :house_roof_bottom-right, [12 8] :grass_stone_top, [7 7] :grass, [22 6] :wall, [16 10] :house_roof_middle-left, [16 9] :house_roof_bottom-left, [16 1] :grass, [3 3] :dirt, [8 11] :wall, [14 7] :grass_stone_top-left, [20 12] :wall, [5 0] :wall, [12 10] :stone, [23 10] :wall, [11 13] :wall, [15 6] :grass_stone_top-left, [10 12] :grass, [18 5] :wall, [5 4] :house_roof_middle, [13 0] :wall, [21 12] :stone, [3 11] :stone_grass_top-left, [15 0] :wall, [2 6] :house_roof_middle-left, [17 4] :wall, [1 13] :wall, [18 0] :wall, [6 11] :grass_stone_bottom, [14 1] :grass, [10 3] :dirt, [9 9] :stone, [24 0] :wall, [20 7] :stone_grass_top-left, [7 12] :grass, [11 8] :grass_stone_top, [16 6] :grass_stone_top, [9 4] :dirt, [17 7] :stone_2, [19 12] :grass, [22 11] :stone, [6 3] :house_roof_top-right, [23 12] :stone, [21 6] :stone_grass_top-left, [22 8] :wall, [14 5] :red_wall-bottom-left, [3 5] :house_roof_top-right, [22 7] :wall, [8 6] :house_bottom_right, [22 13] :wall, [11 0] :wall, [3 9] :wall, [11 11] :wall, [6 6] :house_bottom_right, [15 10] :grass_stone_bottom, [17 12] :grass, [1 4] :grass_dirt_bottom-left, [19 1] :wall, [23 5] :grass, [12 1] :house_roof_bottom-left, [6 7] :grass_stone_right, [1 7] :grass, [4 8] :grass_stone_left, [24 12] :wall, [1 8] :grass, [24 13] :wall, [11 1] :wall, [19 13] :wall, [11 5] :wall, [23 4] :grass, [19 0] :wall, [19 7] :stone_2, [13 3] :red_wall-right, [0 3] :wall, [3 12] :grass_stone_right, [16 0] :wall, [24 10] :wall, [3 10] :stone, [6 5] :house_roof_bottom-right, [1 9] :wall, [24 6] :wall, [20 13] :wall, [10 2] :dirt, [2 10] :stone, [19 2] :wall, [7 10] :stone_2, [14 9] :stone, [5 10] :stone, [19 6] :stone_2, [10 4] :dirt, [2 1] :grass_dirt_top, [7 8] :grass_stone_top, [4 1] :grass_dirt_top-right, [22 5] :wall, [16 8] :house_roof_top-left, [5 11] :grass_stone_bottom, [24 5] :wall, [7 5] :house_roof_bottom-left, [3 6] :house_roof_middle-right, [0 12] :stone, [17 0] :wall, [22 0] :wall, [24 2] :wall, [3 13] :wall, [15 1] :grass, [14 6] :grass, [5 8] :stone_2, [3 8] :grass, [6 1] :wall, [7 11] :grass_stone_bottom, [8 0] :wall, [9 10] :stone, [6 10] :stone, [1 6] :grass, [15 5] :red_wall-bottom-right, [10 0] :wall, [4 4] :house_roof_middle-left, [14 8] :stone_grass_bottom-right, [22 3] :grass_stone_right, [16 5] :grass, [17 11] :stairs, [13 6] :grass, [21 1] :stone_2, [17 3] :grass, [22 12] :stone, [5 13] :wall, [3 7] :house_roof_bottom-right, [1 0] :wall, [9 8] :wall, [8 10] :stone_2, [4 12] :grass, [18 10] :house_roof_middle-right, [10 5] :grass, [9 12] :grass, [11 6] :grass, [13 4] :red_wall-right, [20 2] :stone_2, [7 3] :grass_dirt_left, [21 3] :stone_2, [2 0] :wall, [2 8] :grass, [11 2] :wall, [8 9] :stone, [15 13] :wall, [23 2] :wall, [20 1] :stone_2, [4 0] :wall, [21 4] :stone_2, [17 5] :wall, [23 11] :stone, [12 11] :wall, [13 7] :grass, [10 8] :grass_stone_top, [24 8] :wall, [6 2] :grass_dirt_top, [20 11] :grass_stone_left, [8 2] :dirt, [19 11] :grass, [6 4] :house_roof_middle-right, [20 3] :stone_2, [2 5] :house_roof_top-left, [9 2] :dirt, [17 13] :wall, [0 4] :wall, [7 6] :house_bottom_left, [10 13] :wall, [23 0] :wall, [17 10] :house_roof_middle}, :entity/type :location, :entity/id #uuid "121fb127-fbc8-44b9-ba62-2ca2517b6995", :dimension [[0 0] [24 13]], :display-name "Park - Camp", :walk-set #{[18 7] [1 10] [10 9] [6 8] [13 11] [6 9] [5 7] [5 9] [21 9] [21 7] [9 1] [17 1] [4 11] [18 2] [14 10] [3 4] [1 2] [0 11] [9 7] [5 12] [23 8] [2 2] [22 4] [1 5] [3 2] [8 12] [13 10] [14 12] [1 11] [7 1] [21 2] [7 9] [20 9] [2 4] [13 12] [23 3] [21 11] [2 12] [10 1] [23 7] [13 9] [10 6] [7 2] [4 2] [12 6] [4 10] [1 3] [22 1] [5 6] [2 3] [15 12] [8 1] [14 3] [18 12] [11 7] [11 9] [23 1] [9 6] [15 3] [5 2] [16 7] [23 6] [9 3] [2 11] [17 6] [4 7] [8 3] [3 1] [18 6] [17 2] [14 11] [22 9] [21 10] [12 7] [17 9] [20 6] [19 9] [18 1] [4 9] [12 9] [15 8] [20 4] [20 5] [14 2] [12 12] [13 5] [11 10] [20 0] [16 4] [18 4] [13 8] [6 12] [16 3] [1 1] [15 7] [23 9] [15 9] [15 11] [10 10] [10 7] [8 7] [16 12] [9 5] [1 12] [18 3] [12 8] [7 7] [16 1] [3 3] [14 7] [12 10] [15 6] [10 12] [21 12] [3 11] [6 11] [14 1] [10 3] [9 9] [20 7] [7 12] [11 8] [16 6] [9 4] [17 7] [19 12] [22 11] [23 12] [21 6] [15 10] [17 12] [1 4] [23 5] [6 7] [1 7] [4 8] [1 8] [23 4] [19 7] [3 12] [3 10] [10 2] [2 10] [7 10] [14 9] [5 10] [19 6] [10 4] [2 1] [7 8] [4 1] [5 11] [0 12] [15 1] [14 6] [5 8] [3 8] [7 11] [9 10] [6 10] [1 6] [14 8] [22 3] [16 5] [17 11] [13 6] [21 1] [17 3] [22 12] [8 10] [4 12] [10 5] [9 12] [11 6] [20 2] [7 3] [21 3] [2 8] [8 9] [20 1] [21 4] [23 11] [13 7] [10 8] [6 2] [20 11] [8 2] [19 11] [20 3] [9 2] [17 10]}}, #uuid "8594a767-8036-409d-81eb-104c799cf26e" {:connection-triggers {[7 -1] #uuid "121fb127-fbc8-44b9-ba62-2ca2517b6995"}, :background {[0 1] :grass, [2 -3] :red_wall_top-right, [7 -2] :stone_2, [4 3] :grass_stone_right, [1 -4] :house_roof_bottom, [-3 -2] :grass, [5 -5] :grass, [3 4] :stone_grass_top-left, [1 2] :stone_grass_bottom-right, [0 5] :stone_2, [0 0] :grass, [-1 1] :grass, [4 -2] :grass, [5 3] :grass, [0 -5] :house_roof_middle, [2 2] :stone_2, [-2 5] :wall, [1 5] :stone_2, [0 -3] :red_wall-top, [1 -2] :red_wall-center, [3 2] :stone_2, [4 6] :grass, [-1 -3] :red_wall-top, [7 1] :grass_stone_bottom-right, [-4 -2] :wall, [2 4] :stone_2, [-1 3] :grass, [-2 3] :grass, [6 -2] :stone_2, [-4 3] :wall, [7 2] :grass, [4 2] :stone_grass_top-left, [1 3] :stone_2, [5 5] :wall, [5 6] :grass, [2 3] :stone_2, [-2 -3] :red_wall-top-left, [-3 5] :wall, [-2 0] :grass, [-4 5] :wall, [5 2] :grass_stone_bottom, [-1 5] :wall, [7 -3] :stone_grass_bottom-right, [3 1] :stone_grass_bottom-right, [7 0] :stone_grass_top-left, [-3 -1] :grass, [-4 1] :wall, [-4 0] :wall, [0 2] :grass_stone_top-left, [4 -4] :grass, [-3 4] :grass, [7 -4] :grass_stone_top-left, [7 -5] :grass, [5 1] :stone_2, [3 0] :grass_stone_top-left, [2 -4] :house_roof_bottom-right, [-4 2] :wall, [5 -1] :stone_grass_bottom-right, [0 6] :stone_2, [-3 1] :grass, [4 -3] :grass, [-2 -2] :red_wall-left, [-1 6] :grass_stone_left, [4 5] :wall, [0 -4] :house_roof_bottom, [-1 -5] :house_roof_middle, [1 1] :grass_stone_top-left, [6 0] :stone_2, [7 -1] :stone_2, [-2 -4] :house_roof_bottom-left, [-4 4] :wall, [3 -2] :grass, [7 4] :grass, [6 -5] :grass, [-3 -5] :grass, [-3 -3] :grass, [-1 2] :grass, [-1 -4] :house_roof_bottom, [3 3] :stone_2, [4 -1] :grass_stone_top-left, [-1 4] :grass_stone_top-left, [5 0] :stone_2, [-1 -2] :red_wall-center, [-3 6] :grass, [-2 1] :grass, [5 4] :grass, [2 6] :stone_2, [-3 0] :grass, [-4 -5] :wall, [-2 2] :grass, [-2 4] :grass, [5 -2] :grass_stone_left, [3 -4] :grass, [6 3] :grass, [3 5] :wall, [-4 -3] :wall, [3 -5] :grass, [2 -2] :red_wall-right, [-4 -4] :wall, [3 -3] :grass, [6 6] :grass, [6 -3] :grass_stone_top, [2 -5] :house_roof_middle-right, [1 4] :stone_2, [6 -1] :stone_2, [1 -5] :house_roof_middle, [4 -5] :grass, [-4 6] :grass, [5 -3] :grass_stone_top-left, [0 3] :grass_stone_left, [1 -3] :red_wall-top, [6 5] :wall, [5 -4] :grass, [-3 2] :grass, [2 1] :grass_stone_top, [6 -4] :grass, [4 1] :stone_2, [7 5] :wall, [3 6] :grass_stone_right, [6 1] :stone_grass_top-left, [1 6] :stone_2, [2 -1] :red_wall-bottom-right, [-3 3] :grass, [3 -1] :grass, [1 -1] :red_wall-bottom, [4 4] :grass_stone_bottom-right, [-2 6] :grass, [-2 -1] :red_wall-bottom-left, [1 0] :grass, [-2 -5] :house_roof_middle-left, [-3 -4] :grass, [7 3] :grass, [0 -2] :house_arch_top, [-4 -1] :wall, [2 0] :grass, [-1 0] :grass, [4 0] :stone_grass_bottom-right, [-1 -1] :red_wall-bottom, [6 2] :grass_stone_bottom-right, [6 4] :grass, [2 5] :stone_2, [0 4] :stone_grass_bottom-right, [7 6] :grass, [0 -1] :house_door_bottom}, :entity/type :location, :entity/id #uuid "8594a767-8036-409d-81eb-104c799cf26e", :dimension [[-4 -5] [7 6]], :display-name "Park - Entrance", :walk-set #{[7 -2] [4 3] [3 4] [1 2] [0 5] [0 0] [-1 1] [2 2] [1 5] [3 2] [7 1] [2 4] [6 -2] [4 2] [1 3] [2 3] [-3 5] [-2 0] [5 2] [7 -3] [3 1] [7 0] [0 2] [-3 4] [7 -4] [5 1] [3 0] [5 -1] [0 6] [-3 1] [-1 6] [1 1] [6 0] [7 -1] [3 3] [4 -1] [-1 4] [5 0] [-3 6] [-2 1] [2 6] [-2 2] [5 -2] [6 -3] [1 4] [6 -1] [5 -3] [0 3] [-3 2] [2 1] [4 1] [3 6] [6 1] [1 6] [-3 3] [4 4] [-2 6] [-1 0] [4 0] [6 2] [2 5] [0 4]}}, #uuid "e5b01f7e-d6d1-4e29-8623-f0500b1e933a" {:connection-triggers {[-1 8] #uuid "121fb127-fbc8-44b9-ba62-2ca2517b6995"}, :background {[-8 3] :wall, [0 1] :stone, [2 -3] :wall, [-6 4] :stone, [-7 7] :stone, [-5 -1] :grass_dirt_bottom, [-7 -3] :grass_dirt_top, [0 8] :wall, [-6 7] :stone, [-5 4] :wall, [-8 2] :stone, [1 -4] :wall, [-3 -2] :dirt, [1 2] :stone, [0 5] :stone, [-9 2] :wall, [0 0] :red_wall-center, [-9 1] :wall, [-1 1] :stone, [-1 7] :stone, [2 2] :wall, [-2 5] :house_roof_top, [-8 -3] :grass_dirt_top-left, [1 5] :stone, [0 -3] :grass_dirt_top, [1 -2] :grass_dirt_right, [-6 8] :wall, [-6 3] :wall, [-1 -3] :grass_dirt_top, [-5 1] :stone, [-4 -2] :dirt, [0 7] :stone, [-5 -2] :dirt, [2 4] :wall, [-1 3] :wall, [-2 3] :wall, [-4 3] :wall, [1 3] :wall, [-7 6] :stone, [2 3] :wall, [-2 -3] :grass_dirt_top, [-6 -2] :dirt, [-5 2] :stone, [-7 -2] :dirt, [-3 5] :house_roof_top, [-2 0] :red_wall-center, [-4 5] :house_roof_top, [-1 5] :stone, [-7 -4] :wall, [-3 -1] :grass_dirt_bottom, [-4 1] :stone, [-5 6] :wall, [-4 0] :red_wall-center, [-8 5] :stone, [0 2] :stone, [-3 4] :stone, [-8 0] :red_wall-center, [-9 4] :wall, [2 7] :wall, [2 -4] :wall, [-4 2] :stone, [-9 3] :wall, [0 6] :stone_bush, [-3 1] :stone, [-2 -2] :dirt, [-1 6] :stone, [0 -4] :wall, [-8 6] :stone, [1 1] :stone, [-2 -4] :wall, [-4 4] :stone_2, [-6 -3] :grass_dirt_top, [-3 -3] :grass_dirt_top, [-7 -1] :dirt, [-1 2] :stone, [-1 -4] :wall, [-8 -4] :wall, [-1 4] :stone, [-8 7] :stone, [-6 1] :stone, [-6 6] :stone, [-1 -2] :dirt, [-9 -1] :wall, [-9 -2] :wall, [-2 8] :wall, [-3 6] :stone, [-2 1] :stone, [-4 7] :stone, [2 6] :wall, [-9 0] :wall, [-3 0] :red_wall-center, [-5 -4] :wall, [-8 -2] :grass_dirt_left, [-5 5] :wall, [-5 -3] :grass_dirt_top, [-5 7] :wall, [-2 2] :stone, [-2 4] :stone, [-1 8] :stone, [-9 7] :wall, [-4 -3] :grass_dirt_top, [2 -2] :wall, [-4 -4] :wall, [-9 8] :wall, [1 4] :stone_2, [-7 2] :stone, [-7 3] :house_door_bottom, [1 7] :stone, [-7 1] :stone_2, [1 8] :wall, [-4 6] :stone, [-6 0] :red_wall-center, [-3 7] :stone, [0 3] :stone_2, [1 -3] :grass_dirt_top-right, [-9 5] :wall, [-6 -1] :grass_dirt_bottom, [-9 -3] :wall, [-7 8] :wall, [-5 3] :wall, [-8 1] :stone, [-3 2] :stone, [2 1] :wall, [-4 8] :wall, [-6 -4] :wall, [-5 8] :wall, [-8 4] :stone, [1 6] :stone, [2 -1] :wall, [-3 3] :wall, [1 -1] :grass_dirt_bottom-right, [-2 6] :stone, [-7 5] :stone, [-2 -1] :grass_dirt_bottom, [1 0] :red_wall-center, [-7 0] :stairs, [-3 -4] :wall, [0 -2] :dirt, [-4 -1] :grass_dirt_bottom, [2 0] :wall, [2 8] :wall, [-7 4] :stone, [-1 0] :red_wall-center, [-6 5] :stone, [-9 -4] :wall, [-2 7] :stone, [-1 -1] :grass_dirt_bottom, [-8 -1] :grass_dirt_bottom-left, [-9 6] :wall, [-3 8] :wall, [-8 8] :wall, [2 5] :wall, [-6 2] :stone, [0 4] :stone_2, [-5 0] :red_wall-center, [0 -1] :grass_dirt_bottom}, :entity/type :location, :entity/id #uuid "e5b01f7e-d6d1-4e29-8623-f0500b1e933a", :dimension [[-9 -4] [2 8]], :display-name "Taverne Erdgeschoss", :walk-set #{[0 1] [-6 4] [-7 7] [-5 -1] [-7 -3] [-6 7] [-8 2] [-3 -2] [1 2] [0 5] [-1 1] [-1 7] [-8 -3] [1 5] [0 -3] [1 -2] [-1 -3] [-5 1] [-4 -2] [0 7] [-5 -2] [-7 6] [-2 -3] [-6 -2] [-5 2] [-7 -2] [-1 5] [-3 -1] [-4 1] [-8 5] [0 2] [-3 4] [-4 2] [0 6] [-3 1] [-2 -2] [-1 6] [-8 6] [1 1] [-4 4] [-6 -3] [-3 -3] [-7 -1] [-1 2] [-1 4] [-8 7] [-6 1] [-6 6] [-1 -2] [-3 6] [-2 1] [-4 7] [-8 -2] [-5 -3] [-2 2] [-2 4] [-1 8] [-4 -3] [1 4] [-7 2] [-7 3] [1 7] [-7 1] [-4 6] [-3 7] [0 3] [1 -3] [-6 -1] [-8 1] [-3 2] [-8 4] [1 6] [1 -1] [-2 6] [-7 5] [-2 -1] [-7 0] [0 -2] [-4 -1] [-7 4] [-6 5] [-2 7] [-1 -1] [-8 -1] [-6 2] [0 4] [0 -1]}}}, :location-connections #{#{#uuid "121fb127-fbc8-44b9-ba62-2ca2517b6995" #uuid "8594a767-8036-409d-81eb-104c799cf26e"} #{#uuid "121fb127-fbc8-44b9-ba62-2ca2517b6995" #uuid "e5b01f7e-d6d1-4e29-8623-f0500b1e933a"}}, :current-page nil})
