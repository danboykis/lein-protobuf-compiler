(ns leiningen.protobuf
  (:use [clojure.string :only [join]]
        [leiningen.javac :only [javac]]
        [leiningen.core.eval :only [eval-in-project]]
        [leiningen.core.user :only [leiningen-home]]
        [leiningen.core.main :only [abort]])
  (:require [clojure.java.io :as io]
            [fs.core :as fs]
            [fs.compression :as fs-zip]
            [conch.core :as sh]))

(defn exclusions-p [project]
  (if (:protobuf-exclude project)
    (apply some-fn
           (map #(fn [f]
                   (fs/child-of? % f)) (:protobuf-exclude project)))
    (constantly false)))

(defn proto-paths [project]
  (map io/file
       (get project :proto-paths "resources/proto")))

(defn proto-includes [project]
  (:protobuf-includes project))

(def ^{:dynamic true} *compile-protobuf?* true)

(defn dir! [f]
  (when-not (.exists f)
    (.mkdirs f))
  (assert (.isDirectory f))
  f)

(defn extract-dependencies
  "Extract all files proto depends on into dest."
  [project proto-path protos dest]
  (eval-in-project (dissoc project :prep-tasks)
                   [proto-path (.getPath proto-path)
                    dest       (.getPath dest)
                    protos     protos]
                   (ns __slask__ (:require [clojure.java.io :as io]))
                   (letfn [(dependencies [proto-file]
                             (when (.exists proto-file)
                               (for [line (line-seq (io/reader proto-file))
                                     :when (.startsWith line "import")]
                                 (second (re-matches #".*\"(.*)\".*" line)))))]
                     (loop [deps (mapcat #(dependencies (io/file proto-path %)) protos)]
                       (when-let [[dep & deps] (seq deps)]
                         (let [proto-file (io/file dest dep)]
                           (if (or (.exists (io/file proto-path dep))
                                   (.exists proto-file))
                             (recur deps)
                             (do (.mkdirs (.getParentFile proto-file))
                                 (when-let [resource (io/resource (str "proto/" dep))]
                                   (io/copy (io/reader resource) proto-file))
                                 (recur (concat deps (dependencies proto-file)))))))))))

(defn modtime [f]
  (let [files (if (fs/directory? f)
                (->> f io/file file-seq rest)
                [f])]
    (if (empty? files)
      0
      (apply max (map fs/mod-time files)))))

(defn proto-file? [file]
  (let [name (.getName file)]
    (and (.endsWith name ".proto")
         (not (.startsWith name ".")))))

(defn proto-files [excl dir]
  (for [file (rest (file-seq dir))
        :when (proto-file? file)
        :when (not (excl file))]
    (.substring (.getPath file) (inc (count (.getPath dir))))))

(defn- canonicalize
  [fn]
  (.getAbsoluteFile (io/file fn)))

(defn- include
  [fn]
  (str "-I" (canonicalize fn)))

(defn proto-include-args [project]
  (map include (proto-includes project)))

(defn protoc-command
  [project dest proto-path protos proto-dest]
  (concat ["protoc"
           "-I."
           (str "--java_out=" (.getAbsoluteFile dest))
           (include proto-dest)
           (include proto-path)]
          (proto-include-args project)
          (map str protos)))

(defn compile-idl
  "Create .java files from the provided .proto files."
  [project proto-path protos]
  (println " >> " protos)
  (let [target     (dir! (io/file (:target-path project)))
        dest       (dir! (io/file target "protosrc"))
        class-dest (dir! (io/file target "classes"))
        proto-dest (dir! (io/file target "proto"))]
    #_(extract-dependencies project proto-path protos proto-dest)
    (let [args (protoc-command project dest proto-path protos proto-dest)]
      (println " > " (join " " args))
      (let [result (apply sh/proc (concat args [:dir proto-path]))]
        (when-not (= (sh/exit-code result) 0)
          (abort "ERROR:" (sh/stream-to-string result :err)))))))

(defn compile-java
  "Create .class files from the generated .java files"
  [project]
  (binding [*compile-protobuf?* false]
    (let [target (io/file (:target-path project))
          dest   (dir! (io/file target "protosrc"))]
      (.mkdirs dest)
      (javac (assoc project
                    :java-source-paths [(.getPath dest)]
                    :javac-options ["-Xlint:none"])))))

(defn protobuf
  "Task for compiling protobuf libraries."
  [project]
  (let [excl (exclusions-p project)
        all-files (mapcat #(proto-files excl %) (proto-paths project))]
    (doseq [proto-path (proto-paths project)]
      (compile-idl project proto-path (proto-files excl proto-path)))
    (compile-java project)))
