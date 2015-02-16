(ns leiningen.protobuf
  (:use [clojure.string :only [join]]
        [leiningen.javac :only [javac]]
        [leinjacker.eval :only [in-project]]
        [leiningen.core.user :only [leiningen-home]]
        [leiningen.core.main :only [abort]])
  (:require [clojure.java.io :as io]
            [fs.core :as fs]
            [fs.compression :as fs-zip]
            [conch.core :as sh]))

(defn proto-path [project]
  (io/file (get project :proto-path "resources/proto")))

(defn proto-includes [project]
  (:protobuf-includes project))

(def ^{:dynamic true} *compile-protobuf?* true)

(defn target [project]
  (doto (io/file (:target-path project))
    .mkdirs))

(defn extract-dependencies
  "Extract all files proto depends on into dest."
  [project proto-path protos dest]
  (in-project (dissoc project :prep-tasks)
    [proto-path (.getPath proto-path)
     dest (.getPath dest)
     protos protos]
    (ns (:require [clojure.java.io :as io]))
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

(defn proto-files [dir]
  (for [file (rest (file-seq dir)) :when (proto-file? file)]
    (.substring (.getPath file) (inc (count (.getPath dir))))))

(defn- canonicalize
  [fn]
  (.getAbsoluteFile (io/file fn)))

(defn- include
  [fn]
  (str "-I" (canonicalize fn)))

(defn proto-include-args [project]
  (map include (proto-includes project)))

(defn compile-protobuf
  "Create .java and .class files from the provided .proto files."
  ([project protos]
     (compile-protobuf project protos (io/file (target project) "protosrc")))
  ([project protos dest]
     (let [target     (target project)
           class-dest (io/file target "classes")
           proto-dest (io/file target "proto")
           proto-path (proto-path project)]
       (when (or (> (modtime proto-path) (modtime dest))
                 (> (modtime proto-path) (modtime class-dest)))
         (binding [*compile-protobuf?* false]
           (.mkdirs dest)
           (extract-dependencies project proto-path protos proto-dest)
           (doseq [proto protos]
             (let [args (list* "protoc" proto (str "--java_out=" (.getAbsoluteFile dest))
                               "-I."
                               (include proto-dest)
                               (include proto-path)
                               (proto-include-args project))]
               (println " > " (join " " args))
               (let [result (apply sh/proc (concat args [:dir proto-path]))]
                 (when-not (= (sh/exit-code result) 0)
                   (abort "ERROR:" (sh/stream-to-string result :err))))))
           (javac (assoc project
                    :java-source-paths [(.getPath dest)]
                    :javac-options ["-Xlint:none"])))))))

(defn protobuf
  "Task for compiling protobuf libraries."
  [project & files]
  (let [files (or (seq files)
                  (proto-files (proto-path project)))]
    (compile-protobuf project files)))
