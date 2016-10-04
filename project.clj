(defproject elfeed-cljsrn "0.1.0-SNAPSHOT"
  :description "Elfeed mobile client built with clojurescript and react native"
  :url "http://github.com/areina/elfeed-cljsrn"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha11"]
                 [org.clojure/clojurescript "1.9.225"]
                 [reagent "0.6.0" :exclusions [cljsjs/react cljsjs/react-dom cljsjs/react-dom-server]]
                 [re-frame "0.8.0"]
                 [day8.re-frame/async-flow-fx "0.0.6"]
                 [day8.re-frame/http-fx "0.0.4"]]
  :plugins [[lein-cljsbuild "1.1.4"]]
  :clean-targets ["target/" "index.ios.js" "index.android.js"]
  :aliases {"prod-build" ^{:doc "Recompile code with prod profile."}
            ["do" "clean"
             ;; NO IOS VERSION AT THIS MOMENT
             ;; ["with-profile" "prod" "cljsbuild" "once" "ios"]
             ["with-profile" "prod" "cljsbuild" "once" "android"]]}
  :profiles
  {:dev
   {:dependencies [[figwheel-sidecar "0.5.7"]
                   [com.cemerick/piggieback "0.2.1"]]
    :plugins [[lein-figwheel "0.5.7"]]
    :source-paths ["src" "env/dev"]
    :cljsbuild    {:builds [{:id "ios"
                             :source-paths ["src" "env/dev"]
                             :figwheel true
                             :compiler {:output-to     "target/ios/not-used.js"
                                        :main          "env.ios.main"
                                        :output-dir    "target/ios"
                                        :optimizations :none}}
                            {:id "android"
                             :source-paths ["src" "env/dev"]
                             :figwheel true
                             :compiler {:main          "env.android.main"
                                        :output-to     "target/android/not-used.js"
                                        :output-dir    "target/android"
                                        :verbose true
                                        :optimizations :none}}]}
    :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
   :prod {:cljsbuild {:builds [{:id "ios"
                                :source-paths ["src" "env/prod"]
                                :compiler     {:output-to     "index.ios.js"
                                               :main          "env.ios.main"
                                               :output-dir    "target/ios"
                                               :static-fns    true
                                               :optimize-constants true
                                               :optimizations :simple
                                               :closure-defines {"goog.DEBUG" false}}}
                               {:id "android"
                                :source-paths ["src" "env/prod"]
                                :compiler     {:output-to     "index.android.js"
                                               :main          "env.android.main"
                                               :output-dir    "target/android"
                                               :static-fns    true
                                               :optimize-constants true
                                               :optimizations :simple
                                               :closure-defines {"goog.DEBUG" false}}}]}}})
