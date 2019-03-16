(ns lein-protobuf-compiler.plugin
  (:require [leiningen.javac :refer [javac]]
            [leiningen.jar :refer [jar]]
            [leiningen.protobuf :refer [protobuf *compile-protobuf?*]]
            [robert.hooke :refer [add-hook]]))

(defn hooks []
  (add-hook #'javac
            (fn [f & args]
              (when *compile-protobuf?*
                (protobuf (first args)))
              (apply f args))))
