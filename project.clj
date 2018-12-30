(defproject armchair "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.439"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.6"]
                 [tailrecursion/cljs-priority-map "1.2.1"]
                 [cljsjs/react-select "1.2.1-1"]
                 [cljsjs/filesaverjs "1.3.3-0"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [bidi "2.1.5"]]

  :plugins [[lein-cljsbuild "1.1.5"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"
                        "resources/public/compiled"]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.10"]
                   [figwheel-sidecar "0.5.18"]
                   [cider/piggieback "0.3.10"]
                   [day8.re-frame/re-frame-10x "0.3.6-react16"]]
    :plugins      [[lein-figwheel "0.5.18"]
                   [lein-ancient "0.6.15"]]
    :source-paths ["src/cljs" "dev"]
    :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}}


  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "armchair.core/mount-root"}
     :compiler     {:main                 armchair.core
                    :output-to            "resources/public/compiled/js/app.js"
                    :output-dir           "resources/public/compiled/js/out"
                    :asset-path           "compiled/js/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload day8.re-frame-10x.preload]
                    :closure-defines      {"re_frame.trace.trace_enabled_QMARK_" true}
                    :external-config      {:devtools/config
                                           {:features-to-install [:formatters :hints]}}
                    :optimizations        :none}}

    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            armchair.core
                    :output-to       "build/compiled/js/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}]})
