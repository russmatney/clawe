(ns defthing.defworkspace
  (:require
   [defthing.core :as defthing]
   [defthing.db :as db]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspaces API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-workspaces
  "Lists in-memory workspaces (no db)."
  []
  (defthing/list-things :clawe/workspaces))

(defn get-workspace
  "Returns a matching in-memory workspace.
  Supports wsp as a map (workspace) or string workspace name"
  [wsp]
  (defthing/get-thing :clawe/workspaces
    (comp #{(if (map? wsp)
              (some wsp [:workspace/title :awesome/tag-name :name])
              wsp)}
          :name)))

(defn get-db-workspace
  "Gets a single workspace from the db."
  [w]
  (let [n (cond (string? w) w
                (map? w)    (some w [:name :workspace/title]))]
    (some->>
      (db/query
        '[:find [(pull ?e [*])]
          :in $ ?n
          :where
          [?e :name ?n]]
        n)
      first)))

(defn latest-db-workspaces
  "Lists the latest entity for each :workspace/title in the db."
  []
  (let [wsps (->>
               (db/query
                 '[:find (pull ?e [*])
                   :where
                   [?e :workspace/title ?workspace-title]])
               (map first))]
    ;; TODO refactor into a datalog constraint
    (->> wsps
         (group-by :workspace/title)
         (map (fn [[_k vs]]
                (->> vs
                     (sort-by :workspace/updated-at)
                     first))))))

(defn workspace-for-tx [w]
  (let [existing (get-db-workspace w)
        db-id    (:db/id existing)]
    (cond-> w
      true
      (assoc :workspace/updated-at (System/currentTimeMillis))

      (and db-id (not (:db/id w)))
      (assoc :db/id db-id))))

(defn sync-workspaces-to-db
  "Adds the passed workspaces to the db.
  If none are passed, the in-memory workspaces will be
  written to the db.
  ;; TODO this probably overwrites the user-overwrites...

  Supports passing a single workspace as a map.
  "
  ([] (sync-workspaces-to-db (list-workspaces)))
  ([ws]
   (let [ws    (cond
                 (map? ws) [ws]
                 :else     ws)
         w-txs (map workspace-for-tx ws)]
     (println w-txs)
     (db/transact w-txs))))

(defn install-repo-workspaces
  "For a list of paths to git-repos, creates a workspace
  and adds them to the db."
  [repo-paths]
  ;; TODO impl
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspace-title
  "Sets the workspace title using the :name (which defaults to the symbol).

  This only happens to work b/c defthing sets the {:name blah} attr with
  the passed symbol.
  "
  [{:keys [name]}]
  {:workspace/title name})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defworkspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro defworkspace [title & args]
  (apply defthing/defthing :clawe/workspaces title
         (conj args `workspace-title)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; usage examples
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (defworkspace my-workspace
    workspace-title
    {:some/meta :some/data}))
