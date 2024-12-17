(ns doctor.config
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [systemic.core :as sys :refer [defsys]]))


(defn ->config []
  (aero/read-config (io/resource "doctor.edn")))

(comment
  (->config))

;; restarting this system also restarts the nrepl server :/
(defsys ^:dynamic *config* (->config))

(defn server-port []
  (sys/start! `*config*)
  (:server/port *config*))

(defn nrepl-port []
  (sys/start! `*config*)
  (:nrepl/port *config*))
