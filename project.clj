(defproject com.danboykis/lein-protobuf-compiler "0.0.3"
  :description       "Leiningen plugin for compiling protocol buffers."
  :license           {:name "Eclipse Public License"
                      :url "http://www.eclipse.org/legal/epl-v10.html"}
  :url               "https://github.com/danboykis/lein-protobuf-compiler"
  :dependencies      [[me.raynes/fs "1.4.6" :exclusions [org.clojure/clojure]]
                      [me.raynes/conch "0.8.0" :exclusions [org.clojure/clojure]]]
  :eval-in-leiningen true
  :checksum-deps     true

  :repositories {"clojars.org" {:url "https://repo.clojars.org"}})
