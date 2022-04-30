(ns clawe.git
  (:require
   [ralphie.git :as r.git]
   [defthing.db :as db]
   [defthing.defworkspace :as defworkspace]

   [babashka.fs :as fs]
   [wing.core :as w]))

(defn is-git-dir? [dir]
  (fs/exists? (str dir "/.git")))

(defn git-dirs
  "Fetches git-dirs for all :workspace/directories in the db"
  []
  (->>
    (db/query
      '[:find ?directory
        ;; TODO maybe repos/workspaces opt-in to git history?
        :where [?e :workspace/directory ?directory]])
    (map first)
    (filter is-git-dir?)))

(comment
  (count (git-dirs)))

(defn commits-for-git-dirs
  ([] (commits-for-git-dirs {}))
  ([{:keys [n]}]
   (let [n (or n 10)]
     (->>
       (git-dirs)
       (map #(r.git/commits-for-dir {:dir % :n n}))
       (apply concat)))))

(comment
  (count
    (commits-for-git-dirs {:n 1}))

  (->>
    (git-dirs)
    (map (fn [dir]
           (r.git/commits-for-dir {:dir dir :n 10}))))
  )

(defn get-db-id-for-commit [commit]
  (when (:git.commit/hash commit)
    (some-> (db/query
              '[:find [(pull ?e [:db/id])]
                :in $ ?hash
                :where
                ;; TODO maybe repos/workspaces opt-in to git history?
                [?e :git.commit/hash ?hash]]
              (:git.commit/hash commit))
            first
            :db/id)))

(comment
  (->>
    (db/query
      '[:find (pull ?e [*])
        :where [?e :git.commit/hash ?hash]])
    (map first)
    first
    )

  (get-db-id-for-commit
    (first
      (commits-for-git-dirs {:n 1}))
    )
  )

(defn with-db-git [commit]
  (let [db-id (get-db-id-for-commit commit)]
    (merge
      (when db-id {:db/id db-id})
      commit)))

(defn sync-commits-to-db
  ([] (sync-commits-to-db {}))
  ([opts]
   (->> (commits-for-git-dirs opts)
        (map with-db-git)
        db/transact)))

(comment
  (sync-commits-to-db {:n 15})

  (->>
    (commits-for-git-dirs)
    (take 2)
    (map with-db-git))

  (defworkspace/latest-db-workspaces))

(defn list-db-commits []
  (->>
    ;; TODO how to query unique by git.commit/hash?
    ;; TODO include :git.commit/hash in the schema with a uniqueness constraint
    ;; TODO clean up db (remove dupes)
    ;; TODO get timestamps into db and sort by most recent in this fn
    (db/query
      '[:find (pull ?e [*])
        :where
        [?e :git.commit/hash ?hash]])
    (map first)
    (w/distinct-by :git.commit/hash)
    ))

(comment
  (count
    (list-db-commits))
  )
