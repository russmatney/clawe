(ns clawe.db.pod
  (:require [babashka.pods :as pods]))

;; TODO defsys and configuration
(def clawe-db-filepath "/home/russ/russmatney/clawe/clawedb")

(def schema {:scratchpad.db/id
             {:db/valueType :db.type/string
              :db/unique :db.unique/identity}})

(defn transact [txs]
  (pods/load-pod "dtlv")
  (require '[pod.huahaiy.datalevin :as d])
  (let [conn (d/get-conn clawe-db-filepath schema)
        res (d/transact! conn txs)]
    (d/close conn)
    res))

(defn query [q args]
  (pods/load-pod "dtlv")
  (require '[pod.huahaiy.datalevin :as d])
  (let [conn (d/get-conn clawe-db-filepath schema)
        res
        (if args
          (d/q q (d/db conn) args)
          (d/q q (d/db conn)))]
    (d/close conn)
    res))

(comment
  (pods/load-pod "dtlv")
  (require '[pod.huahaiy.datalevin :as d])
  (let [conn (d/get-conn clawe-db-filepath schema)
        res (d/q '[:find ?e
                   :where
                   (or
                    [?e :name "Datalevin"]
                    [?e :last "value"])]
                 (d/db conn))]
    (d/close conn)
    res))


(defn temp-id []
  (pods/load-pod "dtlv")
  (require '[pod.huahaiy.datalevin :as d])
  (d/tempid 2))


;; (ns clawe.db.core)

;; (require '[babashka.pods :as pods])
;; (pods/load-pod "dtlv")
;; (require '[pod.huahaiy.datalevin :as d])

;; (comment
;;   (d/tempid 1)
;;   )

;; (defn dump-db []
;;   (d/tempid 2)
;;   )
