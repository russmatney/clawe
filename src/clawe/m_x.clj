(ns clawe.m-x
  (:require
   [defthing.defkbd :as defkbd]
   [defthing.defcom :as defcom]

   [ralphie.rofi :as r.rofi]
   [ralphie.core :as r.core]
   [ralphie.git :as r.git]
   [ralphie.bb :as r.bb]
   [ralphie.tmux :as r.tmux]
   [ralphie.awesome :as r.awm]
   [ralphie.systemd :as r.systemd]
   [clawe.wm :as wm]
   [clawe.workspace.open :as workspace.open]
   [clawe.config :as clawe.config]
   [ralphie.rofi :as rofi]
   [clawe.toggle :as toggle]
   [clawe.client.create :as client.create]))

(defn kill-things [_wsp]
  (concat
    (r.tmux/rofi-kill-opts)
    (r.awm/rofi-kill-opts)
    )

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
                                     (r.tmux/fire
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

(defn client-defs []
  (->>
    (clawe.config/client-defs)
    (map (fn [d] (-> d (assoc :rofi/label (str "client-def: " (:client/key d))
                              :rofi/on-select
                              (fn [d]
                                (rofi/rofi
                                  {:msg "Client def action"}
                                  [{:rofi/label     "Show Fields"
                                    :rofi/on-select (fn [_] (show-fields d))}
                                   {:rofi/label     "Create Client"
                                    :rofi/on-select (fn [_] (client.create/create-client d))}
                                   {:rofi/label     "Toggle Client"
                                    :rofi/on-select (fn [_] (toggle/toggle d))}]))))))))

(defn workspace-defs []
  (->>
    (clawe.config/workspace-defs-with-titles)
    vals
    (map (fn [d] (-> d (assoc :rofi/label (str "wsp-def: " (:workspace/title d))
                              :rofi/description (:workspace/directory d)
                              :rofi/on-select
                              (fn [d]
                                (rofi/rofi {:msg (str d)} (def->rofi-fields d)))))))))

(defn m-x-commands
  ([] (m-x-commands nil))
  ([{:keys [wsp]}]
   (let [wsp (or wsp (wm/current-workspace))]
     (->>
       (concat
         (->>
           (r.git/rofi-clone-suggestions-fast)
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
           (r.systemd/rofi-service-opts #(str "Restart " %) :systemd/restart))
         (when-not (clawe.config/is-mac?)
           (r.systemd/rofi-service-opts #(str "Stop " %) :systemd/stop)))
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
          (r.rofi/rofi {:require-match? true
                        :msg            "Clawe commands"})))))

(comment
  (m-x))
