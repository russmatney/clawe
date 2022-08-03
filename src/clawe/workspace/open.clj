(ns clawe.workspace.open
  (:require
   [clawe.wm :as wm]
   [ralphie.notify :as notify]
   [clojure.string :as string]
   [clawe.config :as clawe.config]
   [clawe.doctor :as clawe.doctor]
   [ralphie.zsh :as zsh]
   [ralphie.rofi :as rofi]
   [clawe.workspace :as workspace]))

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
  ;; creates (ensures) workspace before focusing
  (wm/focus-workspace wsp)
  ;; TODO find or create client, and dedupe against toggle-app
  ;; this is close, but the focus ends up wrong after the new wsp gets moved
  #_(rules/reset-workspace-indexes)
  (clawe.doctor/update-topbar) ;; no-op on mac for now
  wsp)

(defn create-workspace-def [repo-wsp]
  (clawe.config/update-workspace-def
    (:workspace/title repo-wsp)
    (-> repo-wsp
        ;; TODO could make more sense in clawe.config
        (update :workspace/directory
                #(string/replace % (clawe.config/home-dir) "~"))
        (select-keys [:workspace/directory]))))

(defn open-new-repo-wsp [local-repo]
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
  (let [wsp-defs (workspace/all-defs)]
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
  (->> (workspace/all-defs)
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