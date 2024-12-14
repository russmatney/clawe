(ns ralphie.config
  (:require
   [babashka.fs :as fs]))

;; TODO consider load via aero + systemic

(defn osx? [& _]
  (boolean (#{"Mac OS X"} (System/getProperty "os.name"))))

(comment (osx?))

(defn home-dir [] (fs/home))

;; TODO cut off with env var
(defn project-dir [] (str (fs/home) "/russmatney/clawe"))

;; TODO cut off with env var
(defn github-username [] "russmatney")

(defn trello-json-url
  "Returns a url for downloading my public trello board as json"
  []
  "")

(defn wallpapers-dir []
  (str (fs/home) "/Dropbox/wallpapers"))

(defn screenshots-dir []
  (str (fs/home) "/Screenshots"))

(defn game-clips-dir []
  (str (fs/home) "/Dropbox/game-assets/game-clips"))
