(ns api.repos
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [clawe.git :as clawe.git]
   [defthing.db :as db]
   [babashka.fs :as fs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; db
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspace-git-dirs
  "Fetches git-dirs for all :workspace/directories in the db"
  []
  (->>
    (db/query
      '[:find ?directory
        :where
        ;; TODO what field should we pull?
        [?e :workspace/directory ?directory]])
    (map first)
    (filter #(fs/exists? (str % "/.git")))))

(defn dir->db-repo [dir]
  ;; TODO validation
  ;; TODO check for git-repo
  ;; TODO attach current status
  ;; TODO last-git-status timestamp
  {:doctor/type    :type/repo
   :repo/directory dir})

(defn sync-repos-to-db []
  (let [workspace-git-dirs (workspace-git-dirs)]
    (->> workspace-git-dirs
         (map dir->db-repo)
         (db/transact))))

(comment
  (sync-repos-to-db)
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
