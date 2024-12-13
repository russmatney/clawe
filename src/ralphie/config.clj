(ns ralphie.config
  (:require
   [babashka.fs :as fs]
   [clojure.string :as string]
   [ralphie.zsh :as zsh]))

(defn osx? [& _]
  (not (boolean (#{"Linux"} (System/getProperty "os.name")))))

(comment
  (osx?))

;; TODO this should all come via resources/*.edn and aero, maybe systemic as well
(defn home-dir [] (fs/home))

(defn project-dir [] (str (fs/home) "/russmatney/clawe"))

(defn github-username [] "russmatney")

(defn monitor []
  (or (System/getenv "MONITOR")
      "eDP-1"
      "HDMI-0"))

(defn trello-json-url
  "Returns a url for downloading my public trello board as json"
  []
  ""
  )
