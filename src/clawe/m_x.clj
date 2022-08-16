(ns clawe.m-x
  (:require
   [defthing.defkbd :as defkbd]
   [defthing.defcom :as defcom]

   [ralphie.rofi :as rofi]
   [ralphie.core :as r.core]
   [ralphie.git :as git]
   [ralphie.bb :as r.bb]
   [ralphie.tmux :as tmux]
   [ralphie.systemd :as systemd]
   [clawe.wm :as wm]
   [clawe.workspace.open :as workspace.open]
   [clawe.config :as clawe.config]
   [clawe.toggle :as toggle]
   [clawe.client.create :as client.create]))

(defn kill-things [_wsp]
  (concat
    (tmux/rofi-kill-opts))

  ;; TODO options to kill pids running in panes?
  ;; TODO options to kill browser tabs
  ;; TODO options to kill running workspaces (and clients within)
  )


(defn bb-tasks-for-wsp
  ([] (bb-tasks-for-wsp (wm/current-workspace)))
  ([wsp]
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
                                       {:tmux.fire/cmd       (str "bb " cmd)
                                        :tmux.fire/session   (:workspace/title wsp)
                                        :tmux.fire/directory dir})))))))))

(defn def->rofi-fields [def]
  (->> def
       (map (fn [[k val]]
              {:rofi/label (str "["  k " " val "]")}))))

(defn show-fields [def]
  (rofi/rofi
    {:msg (str def)}
    (def->rofi-fields def)))

(defn client-def->actions [d]
  [{:rofi/label     "Show Fields"
    :rofi/on-select (fn [_] (show-fields d))}
   {:rofi/label     "(re)Create Client"
    :rofi/on-select (fn [_] (client.create/create-client d))}
   {:rofi/label     "Toggle Client"
    :rofi/on-select (fn [_] (toggle/toggle d))}
   {:rofi/label     "Hide Client"
    :rofi/on-select (fn [_] (wm/hide-client d))}
   {:rofi/label     "Show Client"
    :rofi/on-select (fn [_] (wm/show-client d))}
   {:rofi/label     "Focus Client"
    :rofi/on-select (fn [_] (wm/focus-client d))}])

(defn client-action-rofi [d]
  (rofi/rofi
    {:msg "Client def actions"}
    (client-def->actions d)))

(defn client-defs []
  (->>
    (clawe.config/client-defs)
    (map (fn [d] (-> d (assoc :rofi/label (str "client-def: " (:client/key d))
                              :rofi/on-select (fn [_] (client-action-rofi d))))))))

(defn wsp-def->actions [wsp]
  [{:rofi/label     "Show Fields"
    :rofi/on-select (fn [_] (show-fields wsp))}
   {:rofi/label     "Open Workspace"
    :rofi/on-select (fn [_] (workspace.open/open-new-workspace wsp))}
   {:rofi/label     "Close Workspace"
    :rofi/on-select (fn [_] (wm/delete-workspace wsp))}
   {:rofi/label     "Focus Workspace"
    :rofi/on-select (fn [_] (wm/focus-workspace wsp))}])

(defn wsp-action-rofi [wsp]
  (rofi/rofi
    {:msg "Workspace def actions"}
    (wsp-def->actions wsp)))

(defn workspace-defs []
  (->>
    (clawe.config/workspace-defs-with-titles)
    vals
    (map (fn [w] (-> w (assoc :rofi/label (str "wsp-def: " (:workspace/title w))
                              :rofi/description (:workspace/directory w)
                              :rofi/on-select
                              (fn [w] (wsp-action-rofi w))))))))

(defn m-x-commands
  ([] (m-x-commands nil))
  ([{:keys [wsp]}]
   (let [wsp (or wsp (wm/current-workspace))]
     (->>
       (concat
         (->>
           (git/rofi-clone-suggestions-fast)
           (map (fn [x]
                  (-> x
                      (assoc :rofi/label (str "Clone: " (:rofi/label x)))
                      (update :rofi/on-select
                              (fn [f]
                                ;; return a function wrapping the existing on-select
                                (fn [arg]
                                  (when-let [repo-id (:repo-id x)]
                                    (workspace.open/create-workspace-def-from-path repo-id))
                                  (f arg))))))))

         ;; run bb tasks for the current workspace
         (bb-tasks-for-wsp wsp)

         ;; open a known workspace
         (workspace.open/open-workspace-rofi-options)

         (client-defs)
         (workspace-defs)

         ;; all bindings
         (->> (defkbd/list-bindings) (map defkbd/->rofi))

         ;; all defcoms
         (->> (defcom/list-commands) (map r.core/defcom->rofi))

         ;; kill tmux/tags/clients
         (when-not (clawe.config/is-mac?)
           (kill-things wsp))

         ;; systemd stops/restart
         (when-not (clawe.config/is-mac?)
           (systemd/rofi-service-opts #(str "Restart " %) :systemd/restart))
         (when-not (clawe.config/is-mac?)
           (systemd/rofi-service-opts #(str "Stop " %) :systemd/stop)))
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

(defn m-x
  "Reun rofi with commands created in `m-x-commands`."
  ([] (m-x nil))
  ([_]
   (let [wsp (wm/current-workspace)]
     (->> (m-x-commands {:wsp wsp})
          (rofi/rofi {:require-match? true
                      :msg            "Clawe commands"})))))

(comment
  (m-x))
