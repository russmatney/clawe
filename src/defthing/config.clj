(ns defthing.config
  (:require
   [systemic.core :as sys :refer [defsys]]
   [aero.core :as aero]
   [clojure.java.io :as io]
   [ralphie.zsh :as r.zsh]))

(defn ->config []
  (aero/read-config (io/resource "defthing.edn")))

(comment
  (->config))

(defsys *config* (->config))

(defn db-path []
  (sys/start! `*config*)

  #_(r.zsh/expand (:db-path *config*))
  "dtlv://datalevin:datalevin@localhost:8898/newdb")

(comment
  (db-path))
