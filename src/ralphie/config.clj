(ns ralphie.config
  (:require
   [babashka.fs :as fs]
   [clojure.string :as string]
   [ralphie.zsh :as zsh]))

(def osx? (boolean (string/includes? (zsh/expand "$OSTYPE") "darwin")))
(comment osx?)


;; TODO this should all come via resources/*.edn and aero, maybe systemic as well
(defn home-dir [] (fs/home))

(defn project-dir [] (str (fs/home) "/russmatney/clawe"))

(defn github-username [] "russmatney")

(defn monitor []
  (or (System/getenv "MONITOR")
      "HDMI-0"
      "eDP-1"))

(def cache-dir (str (project-dir) "/.cache"))

(defn cache-file
  "Returns a cache file for the given filename.
  Creates it and any dirs if the file does not exist."
  [filename]
  (let [file (-> (str cache-dir "/" filename) (fs/absolutize))]
    (when-not (fs/exists? file)
      (fs/create-dirs (fs/parent file))
      (fs/create-file file))
    (str file)))
