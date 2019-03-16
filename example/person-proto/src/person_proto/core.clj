(ns person-proto.core
  (:gen-class :name com.danboykis.PersonRunner)
  (:require [flatland.protobuf.core :as pb])
  (:import [com.danboykis.people People$Person]))

(def Person (pb/protodef People$Person))

(defn -main [& args]
  (let [p (pb/protobuf Person :id 4 :name "Bob" :email "bob@example.com")
        new-p (assoc p :name "Bill" :likes ["climbing" "running" "jumping"])
        b (pb/protobuf-dump new-p)]
    (println "Proto to bytes: " (into [] b))
    (println "Back to proto: " (pb/protobuf-load Person b))))

