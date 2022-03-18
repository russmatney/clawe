(ns util
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]))

(defn read-file
  "Parses a file in the same dir as this is called.
  Expects a simple fname input, ex: `(input \"input.txt\")`
  Returns a seq of lines.
  "
  [fname]
  (-> *file*
      io/file
      .getParent
      (str "/" fname)
      slurp
      string/split-lines
      (#(map string/trim %))))

(defn partition-by-newlines [lines]
  (->> lines
       (partition-by #{""})
       (remove (comp #{""} first))))
