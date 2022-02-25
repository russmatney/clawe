(ns expo.db.serial
  (:require
   [tick.core :as t]
   [expo.db.schema :as db.schema]))


(defn encode-attr-value
  "app-value -> database-value"
  [[k v]]
  (cond
    (db.schema/date-attrs k)
    [k (t/inst v)]

    :else [k v]))

(defn encode-tx [tx]
  (cond
    (map? tx)
    (->> tx
         (map encode-attr-value)
         (into {})
         doall)

    :else tx))


(defn decode-attr-value
  "database-value -> app-value"
  [[k v]]
  (cond
    (db.schema/date-attrs k)
    [k (t/instant v)]

    :else [k v]))

(defn decode-entity [ent]
  (cond
    (map? ent)
    (->> ent
         (map decode-attr-value)
         (into {})
         doall)

    :else ent))
