(ns clawe.config
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [systemic.core :as sys :refer [defsys]]))

(defn ->config []
  (aero/read-config (io/resource "clawe.edn")))

(comment
  (->config))

(defsys *config* (->config))

(defn doctor-base-url []
  (sys/start! `*config*)
  (:doctor-base-url *config*))
