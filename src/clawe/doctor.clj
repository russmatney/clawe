(ns clawe.doctor
  (:require
   [clawe.config :as clawe.config]))

(defn update-topbar []
  (slurp (clawe.config/doctor-topbar-url)))
