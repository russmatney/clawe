(ns clawe.workspaces
  (:require
   [defthing.defcom :refer [defcom] :as defcom]
   [defthing.defworkspace :as defworkspace]

   [ralphie.rofi :as rofi]
   [ralphie.notify :as notify]

   clawe.defs.workspaces
   [clawe.doctor :as clawe.doctor]

   [clojure.string :as string]
   [ralphie.zsh :as zsh]
   [clawe.workspace :as workspace]
   [clawe.wm :as wm]
   [clawe.rules :as rules]))

(def home-dir (zsh/expand "~"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Interactive workspace creation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn local-git-users []
  (let [h (str home-dir "/")]
    (->>
      (zsh/expand-many "~/*")
      (filter #(string/starts-with? % h))
      (map #(string/replace % h "")))))

(defn select-local-git-user []
  (rofi/rofi {:msg "Select git user"} (local-git-users)))

(defn path->rofi-obj [path]
  {:rofi/label (str "Install " path) :path path})

(defn do-install-workspaces
  "Walks through selecting one or more repos, converts them to workspaces,
  and add them to the db.

  ;; TODO move to 'open-workspace' rather than this install half-measure

  ;; TODO add options to show all workspaces for all git users.
  Right now you need to know the git user to install a particular repo."
  []
  (let [git-user   (select-local-git-user)
        repo-paths (zsh/expand-many (str home-dir "/" git-user "/*"))
        all-option {:rofi/label (str "Install all " git-user " workspaces")
                    :some/id    "all-option"}
        selected   (->> (concat [all-option] (->> repo-paths (map path->rofi-obj)))
                        (rofi/rofi {:msg "Install workspace for which repo?"}))]
    (cond
      (nil? selected) (notify/notify "No repo selected")

      (= (:some/id all-option) (:some/id selected))
      (do
        (notify/notify (str "installing " (count repo-paths) " workspaces for") git-user)
        (println "repo-paths" repo-paths)
        ;; TODO notify w/ datoms-txed result when osx can handle it
        (defworkspace/install-repo-workspaces repo-paths)
        (notify/notify "installed all workspaces for" git-user))

      :else
      (do
        (notify/notify "installing workspace" (:path selected))
        ;; TODO notify w/ datoms-txed result when osx can handle it
        (defworkspace/install-repo-workspaces (:path selected))
        (notify/notify "Installed workspace" (:path selected))))

    ;; restart doctor db connection after transacting
    (clawe.doctor/db-restart-conn)))

(defcom install-workspaces do-install-workspaces)

(comment
  (do-install-workspaces))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; New create workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-workspace
  "Creates a new tag and focuses it, and run the workspace's on-create hook."
  [wsp]
  (wm/focus-workspace wsp) ;; creates (ensures) workspace before focusing
  ;; TODO find or create client, and dedupe against toggle-app
  (rules/reset-workspace-indexes)
  (clawe.doctor/update-topbar)
  wsp)

(defn wsp->rofi-opts
  [{:as wsp}]
  (let [title (:workspace/title wsp)
        dir   (:workspace/directory wsp)]
    {:rofi/label       title
     :rofi/description (when dir dir)}))

(defn workspace-rofi-options []
  (->> (workspace/all-defs) (map #(merge % (wsp->rofi-opts %)))))

(defn open-workspace-rofi-options []
  (->>
    (workspace-rofi-options)
    (map #(assoc % :rofi/on-select (fn [wsp]
                                     (create-workspace wsp))))))
