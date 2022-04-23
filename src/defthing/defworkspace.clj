(ns defthing.defworkspace
  (:require
   [defthing.core :as defthing]
   [defthing.db :as db]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspaces API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-workspaces
  "Lists in-memory workspaces (defined via `defworkspace` macro)."
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

(defn workspace-for-tx
  "Prepares the workspace to be upserted.
  If there is no `:db/id` on the passed workspace,
  a lookup is attempted to find one."
  [w]
  (let [db-id (:db/id w)
        db-id (when-not db-id (:db/id (get-db-workspace w)))]

    (cond->
        (assoc w :workspace/updated-at (System/currentTimeMillis))
      db-id (assoc :db/id db-id))))

(defn sync-workspaces-to-db
  "Adds the passed workspaces to the db.
  If none are passed, the in-memory workspaces will be
  written to the db.

  ;; TODO this probably overwrites the user-overwrites...
  ;; maybe we can somehow skip those fields?

  Supports passing a single workspace as a map, or a list
  of workspace maps.
  "
  ([] (sync-workspaces-to-db (list-workspaces)))
  ([ws]
   (let [ws    (cond
                 (map? ws) [ws]
                 :else     ws)
         w-txs (map workspace-for-tx ws)]
     (db/transact w-txs))))

(comment
  (def w
    (->> (latest-db-workspaces)
         (filter (comp #{"test-workspace"} :name))
         first
         ))

  (sync-workspaces-to-db
    (assoc w :test-val "NEWWW")))

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
