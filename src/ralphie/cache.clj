(ns ralphie.cache
  (:require
   [ralphie.config :as config]
   [babashka.fs :as fs]))


(defn cache-dir [] (str (config/project-dir) "/.cache"))

(defn cache-file
  "Returns a cache file for the given filename.
  Creates it and any dirs if the file does not exist."
  [filename]
  (let [file (-> (str (cache-dir) "/" filename) (fs/absolutize))]
    (when-not (fs/exists? file)
      (fs/create-dirs (fs/parent file))
      (fs/create-file file))
    (str file)))

(defn clear-file [filename]
  (fs/delete-if-exists filename))
