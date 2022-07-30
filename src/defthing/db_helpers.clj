(ns defthing.db-helpers
  (:require
   [taoensso.timbre :as log]
   [wing.core :as w]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; transact helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def converted-type-map
  ;; TODO bb compatability
  ;; {ZonedDateTime t/inst}
  {}
  )

(defn convert-matching-types [map-tx]
  (->> map-tx
       (map (fn [[k v]]
              (if-let [convert-fn (get converted-type-map (type v))]
                [k (convert-fn v)]
                [k v])))
       (into {})))

(def supported-types
  (->> [6 1.0 "hi" :some-keyword true
        (java.lang.Integer. 3)
        #uuid "8992970d-6c3a-4a3a-b35d-dc5cd28f1484"
        ;; (t/inst)
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
                       (log/debug "unsupported type" t v k)
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
                        :some-vector   [3]}))

(defn drop-unsupported-vals
  "Drops unsupported map vals. Drops nil. Only `supported` types
  get through."
  ([map-tx] (drop-unsupported-vals nil map-tx))
  ([opts map-tx]
   (let [supported-keys   (supported-type-keys opts map-tx)
         [to-tx rejected] (w/partition-keys map-tx supported-keys)]
     (when (and (seq rejected) (:log-rejected opts))
       (log/debug "defthing db transact rejected vals" rejected))
     (->> to-tx
          ;; might be unnecessary b/c it's not 'supported'
          (remove (comp nil? second))
          (into {})))))
