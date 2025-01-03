(ns db.helpers
  (:require
   [taoensso.telemere :as log]
   [wing.core :as w]
   [tick.core :as t]
   [dates.tick :as dates.tick])
  (:import
   [java.time ZonedDateTime]
   [java.io File]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; transact helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def converted-type-map
  {ZonedDateTime                    t/inst
   ;; convert lazy seqs into vecs
   (type (->> (repeat 5) (take 3))) vec
   File                             str})

(defn convert-matching-types [map-tx]
  (->> map-tx
       (map (fn [[k v]]
              (if-let [convert-fn (get converted-type-map (type v))]
                [k (convert-fn v)]
                [k v])))
       (into {})))

(comment
  (type (->> (repeat 5) (take 4000)))
  (def res
    (convert-matching-types
      {:some-zdt (dates.tick/now)
       :some     "val"
       :lazy     (->> (repeat 5) (take 4000))
       }))
  (vector? (:lazy res)))

(def supported-types
  (->> [6 1.0 "hi" :some-keyword true
        (java.lang.Integer. 3)
        #uuid "8992970d-6c3a-4a3a-b35d-dc5cd28f1484"
        (t/inst)
        #{"some" "set" :of/things 5}
        ["a" :vector 6]]
       (map type)
       (into #{})))

(defn supported-type-keys
  ([m] (supported-type-keys nil m))
  ([opts m]
   (->>
     m
     (filter (fn [[k v]]
               (let [t (type v)]
                 (if (supported-types t)
                   true
                   (do
                     (when-not (nil? v)
                       (log/log! {:level :warn :data {:type t :val v :key k}}
                                 "unsupported type")
                       (when (:on-unsupported-type opts)
                         ((:on-unsupported-type opts) m)))
                     nil)))))
     (map first))))

(comment
  (supported-type-keys {:hello         "goodbye"
                        :some-int      5
                        :some-neg-int  -7
                        :some-java-int (java.lang.Integer. 3)
                        :some-float    1.0
                        :some-bool     false
                        :some-keyword  :keyword
                        :some-uuid     #uuid "8992970d-6c3a-4a3a-b35d-dc5cd28f1484"
                        :some-fn       (fn [] (print "complexity!"))
                        :some-set      #{"hi" "there"}
                        :some-vector   [3]
                        :some-inst     (t/inst)
                        :some-zdt      (dates.tick/now)
                        nil            "nil-str"}))

(defn drop-unsupported-vals
  "Drops unsupported map vals. Drops nil. Only `supported` types
  get through."
  ([map-tx] (drop-unsupported-vals nil map-tx))
  ([opts map-tx]
   (let [supported-keys   (supported-type-keys opts map-tx)
         [to-tx rejected] (w/partition-keys map-tx supported-keys)]
     (when (and (seq rejected) (:log-rejected opts))
       (log/log! :debug ["db transact rejected vals" rejected]))
     (->> to-tx
          ;; might be unnecessary b/c it's not 'supported'
          (remove (comp nil? second))
          (remove (comp nil? first)) ;; may want to log this
          (into {})))))
