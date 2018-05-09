(ns armchair.game)

(defn start-game [context]
  (.log js/console "start-game" context))

(defn end-game []
  (.log js/console "end-game"))
