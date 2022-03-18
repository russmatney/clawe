(ns ink.core
  (:require
   [util :as util]))


(comment
  ;; toying with inkle's ink writing language
  (def example-ink
    (util/partition-by-newlines
      (util/read-file "example.ink")))

  (println example-ink)

  )
