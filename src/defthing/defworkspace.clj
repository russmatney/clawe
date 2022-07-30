(ns defthing.defworkspace
  (:require
   [clojure.string :as string]
   [defthing.core :as defthing]
   [defthing.db :as db]
   [ralphie.zsh :as zsh]
   [ralphie.notify :as notify]))

(def home-dir (zsh/expand "~"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspace-defaults
  "Sets the workspace title using the :name (which defaults to the symbol).

  This only happens to work b/c defthing sets the {:name blah} attr with
  the passed symbol.
  "
  [{:keys [name]}]
  {:workspace/title     name
   :workspace/directory home-dir
   :doctor/type         :type/workspace})

(defn absolute-workspace-directory
  "Expects at least a :workspace/directory as a string relative to $HOME.

  {:workspace/directory    \"russmatney/dotfiles\"}
  =>
  {:workspace/directory    \"/home/russ/russmatney/dotfiles\",
  "
  [{:workspace/keys [directory]}]
  (when-not (string/starts-with? directory "/")
    {:workspace/directory (str home-dir "/" directory)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fetch in-memory workspaces
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fetch workspaces from the db
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
          [?e :workspace/title ?n]
          ;; doctor type? clawe type?
          [?e :doctor/type :type/workspace]
          ;; [?e :type :clawe/workspaces]
          ]
        n)
      first)))

(comment
  (get-db-workspace "clawe"))

(defn latest-db-workspaces
  "Lists the latest entity for each :workspace/title in the db."
  []
  (->>
    (db/query
      '[:find (pull ?e [*])
        :where
        [?e :workspace/title ?workspace-title]])
    (map first)))

(comment
  (count
    (latest-db-workspaces))

  (def --w
    (->>
      (db/query
        '[:find (pull ?e [*])
          :where
          [?e :workspace/title ?workspace-title]])

      (map first)
      (filter :awesome.tag/name)
      ;; (filter (comp #{"protomoon-two"} :workspace/title))
      ;; (filter (comp #{"a-little-game-called-mario"} :workspace/title))
      first))

  ;; TODO create helper for doing this
  (db/transact [[:db/retract (:db/id --w) :awesome.tag/name]
                [:db/retract (:db/id --w) :awesome.tag/index]])

  (declare sync-workspaces-to-db)

  ;; does not work
  (sync-workspaces-to-db (assoc --w :awesome.tag/name nil))
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sync/write workspaces to db
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspace-for-tx
  "Prepares the workspace to be upserted."
  [w]
  (-> w
      ;; TODO dissociate awm/yabai attrs?
      ;; TODO filter to our desired db-workspace schema
      (assoc :workspace/updated-at (System/currentTimeMillis))))

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
   (let [ws    (if (map? ws) [ws] ws)
         w-txs (map workspace-for-tx ws)]
     (db/transact w-txs))))

(comment
  (sync-workspaces-to-db)

  (def w
    (->> (latest-db-workspaces)
         (filter (comp #{"ink"} :name))
         first))

  (sync-workspaces-to-db
    (assoc w :test-val "NEWWW")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Build and store repo workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn path->repo-workspace
  "Creates a map used as the base for a workspace based on a git repo.
  Expects a path to a repo to be passed, where the last parts of the path
  are the org or user-name, then the repo-name.

  ex: `russmatney/clawe` or `/home/russ/teknql/wing`
  "
  [path]
  (let [reversed  (-> path (string/split #"/") reverse)
        repo-name (first reversed)
        user-name (second reversed)]
    (if (and repo-name user-name)
      (merge
        (defthing/initial-thing :clawe/workspaces repo-name)
        ;; TODO consider :workspace/readme, :workspace/initial-file
        {;; TODO may need something smart for forks/collisions on :workspace/title
         ;; consider using just repo/short-path
         :workspace/title     repo-name
         :repo/name           repo-name
         :repo/user-name      user-name
         :repo/short-path     (str user-name "/" repo-name)
         ;; TODO maybe git/repo should be short-path?
         :git/repo            (str home-dir "/" user-name "/" repo-name)
         :workspace/directory (str home-dir "/" user-name "/" repo-name)})
      (do
        (notify/notify "Missing repo-name or user-name for repo-path" path)
        nil))))

(defn install-repo-workspaces
  "For a list of paths to git-repos, creates a workspace
  and adds them to the db."
  [repo-paths]
  (let [paths (if (string? repo-paths) [repo-paths] repo-paths)]
    (println "installing wsps for paths" repo-paths)
    (->> paths
         (map path->repo-workspace)
         (remove nil?)
         sync-workspaces-to-db)))

(comment
  (install-repo-workspaces #{"russmatney/dontexist"})
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defworkspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro defworkspace [title & args]
  (apply defthing/defthing {:thing-key :clawe/workspaces
                            :post-ops  [`absolute-workspace-directory]}
         title (conj args `workspace-defaults)))
