^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(ns notebooks.core
  (:require
   [babashka.fs :as fs]))

;; # Core

(defn ->f [path]
  {:path path
   :name (some-> path fs/file-name fs/split-ext first)})

(comment
  (->f *file*)
  (some-> *file* fs/file-name fs/split-ext first))

(def this-file *file*)

(defn notebooks []
  (->> this-file fs/parent fs/list-dir (map str) (map ->f)))
