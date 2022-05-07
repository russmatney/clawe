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

(defn drop-complex-types
  "Ensures a map handed to a plasma stream can be transmitted
  without error. For now, removes functions."
  [wsp]
  (->> wsp
       (filter
         (fn [[k v]]
           (cond
             (fn? v) false
             ;; TODO consider whitelist of supported types
             :else   true)))
       (into {})))

(defn zp
  "Zero Pad numbers - takes a number and the length to pad to as arguments"
  [n c]
  (format (str "%0" c "d") n))
