(ns util
  (:require
   #?@(:clj [[babashka.process :refer [$ check]]
             [clojure.java.io :as io]
             [clojure.string :as string]]
       :cljs [[goog.string :as gstring]
              [goog.string.format]]))
  (:import
   #?@(:clj [[java.nio.file Path]])))

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
           (#(map string/trim %)))]
      :cljs
      [(println "No cljs impl for read-file: " fname)]))

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
             ;;
             :else   true)))
       (map
         (fn [[k v]]
           (cond
             #?@(:clj
                 ;; TODO this might be more expensive than it's worth
                 [(instance? Path v) [k (str v)]])
             :else [k v])))
       (into {})))

(defn zp
  "Zero Pad numbers - takes a number and the length to pad to as arguments"
  [x n]
  #?@(:clj [(format (str "%0" n "d") x)]
      :cljs [(gstring/format (str "%0" n "d") x)]))

(comment
  (zp 5 3)) ;; => "005"

(defn get-cp
  "Builds a classpath in a directory."
  [dir]
  #?@(:clj
      [(-> ^{:dir dir}
           ($ clojure -Spath)
           check :out slurp)]
      :cljs [(println "No cljs impl for get-cp: " dir)]))

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
  (ensure-uuid "59782969-8B9A-4C98-9AE4-2282FF0A2A1F"))

(defn expand-coll-group-bys
  "Expands a cardinality-many group-by result.
  Note that the resulting lists will have duplicate elements."
  [items-grouped-by]
  (reduce
    (fn [item-groups-by-label [labels items]]
      (let [labels (cond
                     (not (coll? labels)) labels
                     ;; make sure empty collections move to nil here
                     ;; so they get grouped with other nil labels (with missing keys, for ex)
                     (empty? labels)      nil
                     :else                labels)]
        (if (coll? labels)
          ;; update for each label in `labels`
          (reduce
            #(update %1 %2 concat items)
            item-groups-by-label
            labels)
          ;; update to support nil coming from group-by or empty-colls
          (update item-groups-by-label labels concat items))))
    {}
    items-grouped-by))

(comment
  (->>
    [{:ks #{1 2 3} :n :a}
     {:ks #{2 3 4} :n :b}
     {:ks #{3 4 5} :n :c}
     {:ks #{} :n :d}
     {:ks nil :n :e}
     {:n :f}
     ]
    (group-by :ks)
    (map (fn [[k xs]]
           [k (->> xs (map :n))])))

  (->>
    [{:ks 1 :n :a}
     {:ks 2 :n :b}
     {:ks 1 :n :c}
     {:ks #{} :n :d}
     {:ks nil :n :e}
     {:n :f}]
    (group-by :ks)
    (expand-coll-group-bys)
    (map (fn [[k xs]]
           [k (->> xs (map :n))]))
    (into {}))

  (->>
    [{:ks #{1 2 3} :n :a}
     {:ks #{2 3 4} :n :b}
     {:ks #{3 4 5} :n :c}
     {:ks #{} :n :d}
     {:ks nil :n :e}
     {:n :f}]
    (group-by :ks)
    (expand-coll-group-bys)
    (map (fn [[k xs]]
           [k (->> xs (map :n))]))
    (into {})))


(defn label->comparable-int [p]
  (cond
    ;; TODO cljc impl?
    (and p (string? p)) (.charCodeAt p)
    (int? p)            p
    (keyword? p)        (label->comparable-int (name p))

    ;; TODO compare dates

    ;; some high val
    :else 1000))

(defn clamp [x small large] (if (< x small) small (min x large)))

(comment
  (clamp -1 0 5)
  (clamp 3 0 5)
  (clamp 6 0 5))
