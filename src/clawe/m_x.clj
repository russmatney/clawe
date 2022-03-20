(ns clawe.m-x
  (:require
   [defthing.defcom :as defcom :refer [defcom]]
   [clawe.workspaces :as workspaces]
   [clawe.workspaces.create :as wsp.create]
   [ralphie.notify :as r.notify]
   [ralphie.rofi :as r.rofi]
   [ralphie.core :as r.core]
   [ralphie.git :as r.git]
   [ralphie.bb :as r.bb]
   [defthing.defkbd :as defkbd]
   [ralphie.tmux :as tmux]))

(defn godot-repo-actions [wsp]
  (when-let [dir (:workspace/directory wsp)]
    ;; TODO search for godot project file
    ;; could also be opt-in as a db field
    ;; i.e. run detection for projects, maybe part of installation
    ;; could even re-run installation as the same entry for
    ;; detection, doctor reports, git-status
    nil
    )
  )

(comment
  (workspaces/current-workspace)
  )

(defn kill-things [wsp]
  (concat
    (tmux/rofi-kill-opts))

  ;; TODO options to kill pids running in panes?
  ;; TODO options to kill browser tabs
  ;; TODO options to kill running clients
  ;; TODO options to kill running awm tags
  ;; TODO options to kill running workspaces (and clients within)
  )


(defn bb-tasks-for-wsp [wsp]
  (when-let [dir (:workspace/directory wsp)]
    (->> (r.bb/tasks dir)
         (map (fn [{:bb.task/keys [cmd description] :as task}]
                (assoc
                  task
                  :rofi/label (str "bb task: " cmd)
                  :rofi/description description
                  :rofi/on-select (fn [_]
                                    (println "bb task on-select" task wsp)
                                    (tmux/fire
                                      {:tmux.fire/cmd     (str "bb " cmd)
                                       :tmux.fire/session (:workspace/title wsp)}))))))))

(comment
  (->> (bb-tasks-for-wsp (workspaces/current-workspace))
       (r.rofi/rofi "hi")))

(defn m-x-commands
  ([] (m-x-commands nil))
  ([{:keys [wsp]}]
   (let [wsp (or wsp (workspaces/current-workspace))]
     (->>
       (concat
         [(when wsp {:rofi/label     "Create Workspace Client"
                     :rofi/on-select (fn [_]
                                       ;; TODO detect if workspace client is already open
                                       ;; wrap these in a nil-punning actions-list api
                                       (r.notify/notify "Creating client for workspace")
                                       (wsp.create/create-client wsp))})

          (when (and wsp (r.git/repo? (workspaces/workspace-repo wsp)))
            {:rofi/label     (str "Fetch repo upstream: " (workspaces/workspace-repo wsp))
             :rofi/on-select (fn [_]
                               (r.notify/notify "Fetching upstream for workspace")
                               ;; TODO support fetching via ssh-agent
                               (r.git/fetch (workspaces/workspace-repo wsp)))})]

         ;; TODO current workspace app suggestions
         ;; i.e. open godot, aseprite if in a godot-workspace

         ;; clone suggestions from open tabs and the clipboard
         (->>
           (r.git/rofi-clone-suggestions-fast)
           (map (fn [x] (assoc x :rofi/label (str "Clone: " (:rofi/label x))))))

         ;; run bb tasks for the current workspace
         (bb-tasks-for-wsp wsp)

         ;; all bindings
         (->> (defkbd/list-bindings) (map defkbd/->rofi))

         ;; all defcoms
         (->> (defcom/list-commands) (map r.core/defcom->rofi))

         ;; open a known workspace
         ;; TODO improve display, handling if wsp already open
         (->> (workspaces/open-workspace-rofi-options)
              (map #(assoc % :rofi/label (str "Open wsp: " (:rofi/label %)))))

         (kill-things wsp)
         )
       (remove nil?)))))

(comment
  (m-x-commands)
  (->>
    (m-x-commands)
    (filter :defcom/name)
    (filter (comp #(re-seq #"key" %) :defcom/name))
    (first)
    :rofi/on-select
    ((fn [f] (f)))))

(defcom m-x
  (let [wsp (workspaces/current-workspace)]

    ;; Notify with git status
    (when (and wsp (r.git/repo? (workspaces/workspace-repo wsp)))
      (r.notify/notify (str "Git Status: " (workspaces/workspace-repo wsp))
                       (->>
                         (r.git/status (workspaces/workspace-repo wsp))
                         (filter second)
                         (map first)
                         seq)))

    (->> (m-x-commands {:wsp wsp})
         (r.rofi/rofi {:require-match? true
                       :msg            "Clawe commands"}))))

(comment
  (defcom/exec m-x)
  )
