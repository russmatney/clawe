(ns clawe.doctor
  (:require
   [clawe.config :as clawe.config]))

(defn update-topbar-url []
  (str (clawe.config/doctor-base-url) "/topbar/update"))

(defn update-topbar []
  (slurp (update-topbar-url)))

(defn db-restart-conn []
  (slurp (str (clawe.config/doctor-base-url) "/db/restart-conn")))

(defn reload []
  (slurp (str (clawe.config/doctor-base-url) "/reload")))

(defn update-screenshots []
  (slurp (str (clawe.config/doctor-base-url) "/screenshots/update")))
