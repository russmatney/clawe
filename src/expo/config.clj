(ns expo.config
  (:require
   [systemic.core :as sys :refer [defsys]]
   [aero.core :as aero]
   [clojure.java.io :as io]))

(defn ->config []
  (aero/read-config (io/resource "expo.edn")))

(comment
  (->config))

(defsys *config* (->config))
