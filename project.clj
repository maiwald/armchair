(defproject armchair "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [reagent "0.8.0"]
                 [re-frame "0.10.5"]]

  :plugins [[lein-cljsbuild "1.1.5"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.10"]
                   [figwheel-sidecar "0.5.16"]
                   [cider/piggieback "0.3.3"]
                   [day8.re-frame/re-frame-10x "0.2.1"]]
    :plugins      [[lein-figwheel "0.5.16"]]
    :source-paths ["src/cljs" "dev"]
    :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
    }}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "armchair.core/mount-root"}
     :compiler     {:main                 armchair.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload day8.re-frame-10x.preload]
                    :closure-defines      {"re_frame.trace.trace_enabled_QMARK_" true}
                    :external-config      {:devtools/config {:features-to-install :all}}
                    :optimizations        :none
                    }}

    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            armchair.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}
    ]}
  )
