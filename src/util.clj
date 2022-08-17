(ns util
  (:require
   [babashka.process :refer [$ check]]
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
         (fn [[_k v]]
           (cond
             (fn? v) false
             :else   true)))
       (into {})))

(defn zp
  "Zero Pad numbers - takes a number and the length to pad to as arguments"
  [n c]
  (format (str "%0" c "d") n))

(comment
  (zp 5 3) ;; => "005"
  )

(defn get-cp
  "Builds a classpath in a directory."
  [dir]
  (-> ^{:dir dir}
      ($ clojure -Spath)
      check :out slurp))

(defn ensure-uuid [id]
  (cond
    (string? id)
    (java.util.UUID/fromString id)

    (uuid? id)
    id))

(comment
  (ensure-uuid "hi")
  (ensure-uuid #uuid "59782969-8B9A-4C98-9AE4-2282FF0A2A1F")
  (ensure-uuid "59782969-8B9A-4C98-9AE4-2282FF0A2A1F"))
