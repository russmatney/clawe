(ns api.repos
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [clawe.git :as clawe.git]))

(defn ->repo [path]
  {:repo/path path})

(defn active-repos []
  (->>
    (clawe.git/git-dirs)
    (map ->repo)))

(comment
  (clawe.git/git-dirs)
  (clawe.git/list-db-commits))

(defsys *repos-stream*
  :start (s/stream)
  :stop (s/close! *repos-stream*))

(defn push-repo-update []
  (s/put! *repos-stream* (active-repos)))

(defn fetch-commits [repo]
  (clawe.git/sync-commits-to-db {:dirs [(:repo/path repo)]}))
