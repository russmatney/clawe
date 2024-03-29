(ns db.config
  (:require
   [systemic.core :as sys :refer [defsys]]
   [aero.core :as aero]
   [clojure.java.io :as io]
   [ralphie.zsh :as zsh]))

(defn ->config []
  (aero/read-config (io/resource "db.edn")))

(comment
  (->config))

(defsys ^:dynamic *config* (->config))

(comment
  (sys/restart! `*config*)
  )

(defn db-path []
  (sys/start! `*config*)

  ;; fine to no-op if nothing to expand
  (zsh/expand (:db-path *config*)))

(comment
  (db-path))
