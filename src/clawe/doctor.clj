(ns clawe.doctor
  (:require
   [clawe.config :as clawe.config]))

(defn update-topbar-url []
  (str (clawe.config/doctor-base-url) "/topbar/update"))

(defn update-topbar
  ([] (update-topbar nil))
  ([_] (slurp (update-topbar-url))))

(defn reload []
  (when-not (clawe.config/is-mac?)
    (slurp (str (clawe.config/doctor-base-url) "/reload"))))

(defn update-screenshots []
  (slurp (str (clawe.config/doctor-base-url) "/screenshots/update")))
