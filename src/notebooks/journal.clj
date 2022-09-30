(ns notebooks.journal
  {:nextjournal.clerk/toc        true
   :nextjournal.clerk/visibility {:code :hide}}
  (:require [babashka.fs :as fs]
            [clojure.string :as string]))

;; # Journal

(comment
  (println "eval me!

welcome to the journal repl!

"))

(defn set-extension [path ext]
  (-> path (string/replace (fs/extension path) ext)))

(def journal-clerk-file
  (-> *file* (set-extension "clj")))

(def journal-org-file
  (-> *file*
      (string/replace "/russmatney/clawe/src/notebooks" "/todo")
      (set-extension "org")))
