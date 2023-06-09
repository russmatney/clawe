(ns ralphie.config
  (:require
   [babashka.fs :as fs]))

;; TODO this should all come via resources/*.edn and aero, maybe systemic as well
(defn home-dir [] (fs/home))

(defn project-dir [] (str (fs/home) "/russmatney/clawe"))

(defn github-username [] "russmatney")

(defn monitor []
  (or (System/getenv "MONITOR")
      "HDMI-0"
      "eDP-1"))
