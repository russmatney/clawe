(ns expo.db.schema)

(defn schema
  []
  [])

(def date-attrs
  (->> (schema)
       (filter
         (comp #{:db.type/instant} :db/valueType))
       (map :db/ident)
       (into #{})))
