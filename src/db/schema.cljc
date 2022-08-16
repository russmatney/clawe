(ns db.schema
  (:require [datascript.core :as d]))

(def schema
  { ;; uuids
   :topbar/id
   {:db/unique :db.unique/identity}
   :test/id
   {:db/unique :db.unique/identity}
   :misc/id
   {:db/unique :db.unique/identity}

   ;; unique string ids

   :commit/hash
   {:db/unique :db.unique/identity}
   :repo/directory
   {:db/unique :db.unique/identity}
   :commit/repo
   {:db/valueType :db.type/ref}

   :lichess.game/id
   {:db/unique :db.unique/identity}

   :file/full-path
   {:db/unique :db.unique/identity}

   ;; :time/rn
   ;; {:db/valueType :db.type/instant}

   ;; org attrs
   :org/id
   {:db/unique :db.unique/identity}
   :org/fallback-id
   {:db/unique :db.unique/identity}

   ;; manys
   :org/link-text
   {:db/cardinality :db.cardinality/many}
   :org/linked-from
   {:db/cardinality :db.cardinality/many
    :db/valueType   :db.type/ref}

   :org/links-to
   {:db/cardinality :db.cardinality/many
    :db/valueType   :db.type/ref}
   :org/parents
   {:db/cardinality :db.cardinality/many
    :db/valueType   :db.type/ref}

   ;; TODO how to do many, but still unique?
   :org/parent-names
   {:db/cardinality :db.cardinality/many}

   ;; TODO one day create unique tag entities with metadata
   :org/tags
   {:db/cardinality :db.cardinality/many}
   ;; TODO one day create unique url entities with metadata
   :org/urls
   {:db/cardinality :db.cardinality/many}})

(comment
  (d/empty-db schema))
