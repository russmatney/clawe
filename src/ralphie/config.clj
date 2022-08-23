(ns ralphie.config
  (:require [ralphie.sh :refer [expand]]))

;; TODO this should all come via resources/*.edn and aero, maybe systemic as well
(defn home-dir [] (expand "~"))

(defn project-dir [] (expand "~/russmatney/ralphie"))

(defn github-username [] "russmatney")

(defn monitor []
  (or (System/getenv "MONITOR")
      "HDMI-0"
      "eDP-1"))
