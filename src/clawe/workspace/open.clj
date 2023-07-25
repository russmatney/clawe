(ns clawe.workspace.open
  (:require
   [clojure.string :as string]
   [ralphie.notify :as notify]
   [ralphie.zsh :as zsh]
   [ralphie.rofi :as rofi]

   [clawe.config :as clawe.config]
   [clawe.doctor :as clawe.doctor]
   [clawe.rules :as clawe.rules]
   [clawe.wm :as wm]
   ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspace opening/creation/installation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dir->repo-workspace
  "Creates a map used as the base for a workspace based on a git repo.
  Expects a path to a repo to be passed, where the last parts of the path
  are the org or user-name, then the repo-name.

  ex: `russmatney/clawe` or `/home/russ/teknql/wing`
  "
  [dir]
  (let [reversed  (-> dir (string/split #"/") reverse)
        repo-name (first reversed)
        user-name (second reversed)]
    (if (and repo-name user-name)
      {;; consider using just repo/short-path
       :workspace/title     repo-name
       :repo/name           repo-name
       :repo/user-name      user-name
       :repo/short-path     (str user-name "/" repo-name)
       :workspace/directory (str (clawe.config/home-dir) "/" user-name "/" repo-name)}
      (do
        (notify/notify "Missing repo-name or user-name for repo-path" dir)
        nil))))

(defn open-new-workspace
  "Creates a new tag and focuses it, and run the workspace's on-create hook."
  [wsp]
  (wm/open-new-workspace wsp)

  ;; TODO open clients for workspace
  ;; could be clients with matching workspace titles, or some other matching rule
  ;; (client.create/create-client "emacs")
  ;; (client.create/create-client "terminal")
  ;; (clawe.rules/clean-up-workspaces) ;; deletes empty workspace immediately after creating it
  (clawe.rules/sort-workspace-indexes)
  (clawe.doctor/update-topbar)
  wsp)

(comment
  (->> (wm/workspace-defs)
       (filter (comp #{"dino"} :workspace/title))
       first
       open-new-workspace
       )
  )


(defn create-workspace-def [repo-wsp]
  (clawe.config/update-workspace-def
    (:workspace/title repo-wsp)
    (-> repo-wsp
        ;; TODO could make more sense in clawe.config
        (update :workspace/directory
                #(string/replace % (clawe.config/home-dir) "~"))
        (select-keys [:workspace/directory]))))

(defn create-workspace-def-from-path
  "Converts the passed dir-path or repo-id into a repo-workspace,
  then merges it into the `resource/clawe.edn`."
  [repo-id]
  (-> repo-id dir->repo-workspace create-workspace-def))

(defn open-new-repo-wsp
  "Opens a passed "
  [local-repo]
  (-> local-repo create-workspace-def)
  (-> local-repo open-new-workspace))

(defn dir->rofi-open-workspace-opts [dir]
  (-> dir
      dir->repo-workspace
      ((fn [{:keys [repo/short-path] :as wsp}]
         ;; TODO detect overwriting workspace names?
         (merge wsp
                {:rofi/label     (str "Open wsp: " short-path)
                 :rofi/on-select open-new-repo-wsp})))))

(defn local-git-users []
  (let [h (str (clawe.config/home-dir) "/")]
    (->>
      (zsh/expand-many "~/*")
      (filter #(string/starts-with? % h))
      (map #(string/replace % h "")))))

(defn open-workspace-for-some-git-user
  "Prompts for selecting a local git user, then a repo to open."
  []
  (some->>
    (rofi/rofi {:msg "Select git user"} (local-git-users))
    ((fn [git-user] (zsh/expand-many (str "~/" git-user "/*"))))
    (map dir->rofi-open-workspace-opts)
    (rofi/rofi {:msg "Open workspace for which repo?"})))

(defn config-repo-roots []
  (let [wsp-defs (wm/workspace-defs)]
    (->> (clawe.config/repo-roots)
         (mapcat zsh/expand-many)
         ;; remove existing :workspace/directory
         (remove (fn [dir-path]
                   (->> wsp-defs (map :workspace/directory)
                        (filter #{dir-path}) first))))))

(defn wsp->rofi-opts
  [{:as wsp}]
  (let [title (:workspace/title wsp)
        dir   (:workspace/directory wsp)]
    {:rofi/label       (str "Open wsp: " title)
     :rofi/description (when dir dir)}))

(defn workspace-rofi-options []
  (->> (wm/workspace-defs)
       ;; TODO if already open?
       (map #(merge % (wsp->rofi-opts %)))))

(defn open-workspace-rofi-options []
  (concat
    (->>
      (workspace-rofi-options)
      (map #(assoc % :rofi/on-select open-new-workspace)))
    [{:rofi/label     "Open workspace for some repo"
      :rofi/on-select (fn [_] (open-workspace-for-some-git-user))}]
    (->>
      (config-repo-roots)
      (map dir->rofi-open-workspace-opts)
      (map #(assoc % :rofi/on-select open-new-repo-wsp)))))


(defn rofi-open-workspace
  ([] (rofi-open-workspace nil))
  ([_]
   (rofi/rofi "Open workspace" (open-workspace-rofi-options))))

(comment
  (rofi-open-workspace))
