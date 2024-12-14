(ns ralphie.config
  (:require
   [babashka.fs :as fs]))

(defn osx? [& _]
  (boolean (#{"Mac OS X"} (System/getProperty "os.name"))))

(comment (osx?))

(defn home-dir [] (fs/home))

;; TODO cut off with env var
(defn project-dir [] (str (fs/home) "/russmatney/clawe"))

;; TODO cut off with env var
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
