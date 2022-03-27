(ns doctor.util)

(defn drop-complex-types
  "Ensures a map handed to a plasma stream can be transmitted
  without error. For now, removes functions."
  [wsp]
  (->> wsp
       (filter
         (fn [[k v]]
           (cond
             (fn? v)
             (do (println "dropping k-v" (type v) k v)
                 false)
             ;; TODO consider whitelist of supported types
             :else true)))
       (into {})))
