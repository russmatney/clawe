(ns clawe.doctor
  (:require
   [clawe.config :as clawe.config]))

(defn update-topbar []
  (slurp (str (clawe.config/doctor-base-url) "/topbar/update")))

(defn db-restart-conn []
  (slurp (str (clawe.config/doctor-base-url) "/db/restart-conn")))
