(ns expo.config
  (:require
   [systemic.core :as sys :refer [defsys]]
   [aero.core :as aero]
   [clojure.java.io :as io]
   [ralphie.zsh :as zsh]))

(defn ->config []
  (aero/read-config (io/resource "expo.edn")))

(comment
  (->config))

(defsys *config* (->config))

(defn expo-db-path []
  (sys/start! `*config*)
  (-> *config* :expo-db-path zsh/expand))

(comment
  (expo-db-path))
