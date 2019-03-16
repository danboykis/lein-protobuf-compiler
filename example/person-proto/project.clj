(defproject person-proto "0.1.0-SNAPSHOT"
  :description "Example proto project"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.google.protobuf/protobuf-java "3.5.1"]
                 [org.clojars.ghaskins/protobuf "3.4.0-1"]]

  :protoc "/usr/local/bin/protoc"
  :proto-paths ["resources/proto"]
  :plugins [[com.danboykis/lein-protobuf-compiler "0.0.1"]]

  :profiles {:uberjar {:main com.danboykis.PersonRunner
                       :uberjar-name "person-runner-uber.jar"
                       :auto-clean false
                       :aot :all}})
