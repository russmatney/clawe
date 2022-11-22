(ns doctor.config
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [systemic.core :as sys :refer [defsys]]))

(defn ->config []
  (aero/read-config (io/resource "doctor.edn")))

(comment
  (->config))

(defsys ^:dynamic *config* (->config))
