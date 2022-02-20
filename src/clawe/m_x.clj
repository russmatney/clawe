(ns clawe.m-x
  (:require
   [defthing.defcom :as defcom :refer [defcom]]
   [clawe.workspaces :as workspaces]
   [clawe.workspaces.create :as wsp.create]
   [ralphie.notify :as r.notify]
   [ralphie.rofi :as r.rofi]
   [ralphie.core :as r.core]
   [ralphie.git :as r.git]
   [defthing.defkbd :as defkbd]))



(defn m-x-commands
  ([] (m-x-commands nil))
  ([{:keys [wsp]}]
   (let [wsp (or wsp (workspaces/current-workspace))]
     (->>
       (concat
         [(when wsp {:rofi/label     "Create Workspace Client"
                     :rofi/on-select (fn [_]
                                       ;; TODO detect if workspace client is already open
                                       ;; wrap these nil-punning actions-list api
                                       (r.notify/notify "Creating client for workspace")
                                       (wsp.create/create-client wsp))})
          {:rofi/label     "Suggest more things here! <small> but don't get distracted </small>"
           :rofi/on-select (fn [_] (r.notify/notify "A quick doctor checkup?"
                                                    "Or the time of day?"))}

          (when (and wsp (r.git/repo? (workspaces/workspace-repo wsp)))
            {:rofi/label     (str "Fetch repo upstream: " (workspaces/workspace-repo wsp))
             :rofi/on-select (fn [_]
                               (r.notify/notify "Fetching upstream for workspace")
                               ;; TODO support fetching via ssh-agent
                               (r.git/fetch (workspaces/workspace-repo wsp)))})]
         (->>
           (defkbd/list-bindings)
           (map defkbd/->rofi))
         (->>
           (defcom/list-commands)
           (map r.core/defcom->rofi)))
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
