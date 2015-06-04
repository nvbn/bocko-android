(defproject bocko-android "0.1.3"
            :description "Render Bocko on Android surface"
            :url "https://github.com/nvbn/bocko-android"
            :license {:name "Eclipse Public License"
                      :url "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.7.0-beta3"]
                           [org.clojure/clojurescript "0.0-3269"]
                           [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                           [com.cognitect/transit-cljs "0.8.220"]
                           [bocko "0.3.0"]]
            :plugins [[lein-cljsbuild "1.0.6"]
                      [lein-figwheel "0.3.3"]]
            :profiles {:dev
                       {:cljsbuild {:builds {:min {:source-paths ["src"]
                                                   :compiler {:output-to "android/BockoAndroid/app/src/main/assets/main.js"
                                                              :optimizations :advanced
                                                              :pretty-print false}}
                                             :main {:source-paths ["src"]
                                                    :figwheel {:websocket-host "192.168.0.107"}
                                                    :compiler {:output-to "resources/public/compiled/main.js"
                                                               :output-dir "resources/public/compiled"
                                                               :asset-path "/compiled"
                                                               :main bocko-android.example
                                                               :source-map true
                                                               :optimizations :none
                                                               :pretty-print false}}}}
                        :figwheel {:nrepl-port 7888}}})
