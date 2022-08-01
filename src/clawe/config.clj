(ns clawe.config
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [systemic.core :as sys :refer [defsys]]
   [ralphie.zsh :as zsh]))

(defn calc-is-mac? []
  (boolean (#{"darwin21"} (zsh/expand "$OSTYPE"))))

(defn ->config []
  (->
    (aero/read-config (io/resource "clawe.edn"))
    (assoc :is-mac (calc-is-mac?)))
  )

(comment
  (->config))

(defsys *config* (->config))

(defn doctor-base-url []
  (sys/start! `*config*)
  (:doctor-base-url *config*))

(defn is-mac? []
  (sys/start! `*config*)
  (:is-mac *config*))
