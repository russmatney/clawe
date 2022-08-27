(ns util
  (:require
   #?@(:clj [[babashka.process :refer [$ check]]
             [clojure.java.io :as io]
             [clojure.string :as string]])))

(defn read-file
  "Parses a file in the same dir as this is called.
  Expects a simple fname input, ex: `(input \"input.txt\")`
  Returns a seq of lines.
  "
  [fname]
  #?@(:clj
      [(-> *file*
           io/file
           .getParent
           (str "/" fname)
           slurp
           string/split-lines
           (#(map string/trim %)))]))

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
  #?@(:clj
      [(format (str "%0" c "d") n)]))

(comment
  (zp 5 3)) ;; => "005"

(defn get-cp
  "Builds a classpath in a directory."
  [dir]
  #?@(:clj
      [(-> ^{:dir dir}
           ($ clojure -Spath)
           check :out slurp)]))

(defn ensure-uuid [id]
  (cond
    (string? id)

    #?@(:clj
        [(java.util.UUID/fromString id)]
        :cljs
        [nil])

    (uuid? id)
    id))

(comment
  (ensure-uuid "hi")
  (ensure-uuid #uuid "59782969-8B9A-4C98-9AE4-2282FF0A2A1F")
  (ensure-uuid "59782969-8B9A-4C98-9AE4-2282FF0A2A1F")
  )

(defn expand-coll-group-bys
  "Expands a cardinality-many group-by result.
  Note that the resulting lists will have duplicate elements.
  "
  [items-grouped-by]
  (reduce
    (fn [item-groups-by-label [labels items]]
      (if (coll? labels)
        ;; update for each label
        (reduce
          #(update %1 %2 concat items)
          item-groups-by-label
          labels)
        ;; simple case, just passing through
        (assoc item-groups-by-label labels items)))
    {}
    items-grouped-by))

(comment
  (->>
    [{:ks #{1 2 3} :n :a}
     {:ks #{2 3 4} :n :b}
     {:ks #{3 4 5} :n :c}]
    (group-by :ks)
    (map (fn [[k xs]]
           [k (->> xs (map :n))])))

  (->>
    [{:ks #{1 2 3} :n :a}
     {:ks #{2 3 4} :n :b}
     {:ks #{3 4 5} :n :c}]
    (group-by :ks)
    (expand-coll-group-bys)
    (map (fn [[k xs]]
           [k (->> xs (map :n))]))
    (into {})))
