(ns leiningen.protobuf
  (:require [clojure.java.io :as io]
            [clojure.string :refer [join]]
            [leiningen.javac :refer [javac]]
            [leiningen.core.eval :refer [eval-in-project]]
            [leiningen.core.user :refer [leiningen-home]]
            [leiningen.core.main :refer [abort info]]
            [me.raynes.fs :as fs]
            [me.raynes.conch.low-level :as llsh])
  (:import [java.io File]))

(defn exclusions-p [{:keys [protobuf-exclude] :as project}]
  (if protobuf-exclude
    (apply some-fn
           (map #(fn [f]
                   (fs/child-of? % f)) protobuf-exclude))
    (constantly false)))

(defn proto-paths [{:keys [proto-paths]
                    :or {proto-paths ["resources/proto"]}}]
  {:pre [(coll? proto-paths)]}
  (into [] (map io/file) proto-paths))

(def compile-protobuf? (volatile! true))

(defn dir! [f]
  (when-not (.exists f)
    (.mkdirs f))
  (assert (.isDirectory f))
  f)

(defn proto-file? [file]
  (let [name (.getName file)]
    (and (.endsWith name ".proto")
         (not (.startsWith name ".")))))

(defn proto-files [excl dir]
  (into [] (comp
             (filter proto-file?)
             (remove excl)
             (map (fn [^File file] (.substring (.getPath file) (inc (count (.getPath dir)))))))
        (rest (file-seq dir))))

(defn- canonicalize
  [file-name]
  (.getAbsoluteFile (io/file file-name)))

(defn- include
  [file-name]
  (str "-I" (canonicalize file-name)))

(defn proto-include-args [{:keys [protobuf-includes]}]
  (mapv include protobuf-includes))

(defn protoc-command
  [{:keys [protoc] :or {protoc "protoc"} :as project} dest proto-path protos proto-dest]
  (into [] cat [[protoc
                 "-I."
                 (str "--java_out=" (.getAbsoluteFile dest))
                 (include proto-dest)
                 (include proto-path)]
                (proto-include-args project)
                (mapv str protos)]))

(defn compile-idl
  "Create .java files from the provided .proto files."
  [project proto-path protos]
  (info " >> " protos)
  (let [target     (dir! (io/file (:target-path project)))
        dest       (dir! (io/file target "protosrc"))
        proto-dest (dir! (io/file target "proto"))]

    (let [args (protoc-command project dest proto-path protos proto-dest)]
      (info " > " (join " " args))
      (let [result (apply llsh/proc (concat args [:dir proto-path]))]
        (when-not (= (llsh/exit-code result) 0)
          (abort "ERROR:" (llsh/stream-to-string result :err)))))))

(defn compile-java
  "Create .class files from the generated .java files"
  [project]
  (vreset! compile-protobuf? false)
  (let [target (io/file (:target-path project))
        dest   (dir! (io/file target "protosrc"))]
    (.mkdirs dest)
    (javac (assoc project
                  :java-source-paths [(.getPath dest)]
                  :javac-options ["-Xlint:none"]))))

(defn protobuf
  "Task for compiling protobuf libraries."
  [project]
  (let [excl (exclusions-p project)]
    (doseq [proto-path (proto-paths project)]
      (compile-idl project proto-path (proto-files excl proto-path)))
    (compile-java project)))
