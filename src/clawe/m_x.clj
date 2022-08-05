(ns clawe.m-x
  (:require
   [defthing.defkbd :as defkbd]
   [defthing.defcom :as defcom :refer [defcom]]

   [ralphie.rofi :as r.rofi]
   [ralphie.core :as r.core]
   [ralphie.git :as r.git]
   [ralphie.bb :as r.bb]
   [ralphie.tmux :as r.tmux]
   [ralphie.awesome :as r.awm]
   [ralphie.systemd :as r.systemd]
   [clawe.wm :as wm]
   [clawe.workspace.open :as workspace.open]
   [clawe.config :as clawe.config]))

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

(defn m-x-commands
  ([] (m-x-commands nil))
  ([{:keys [wsp]}]
   (let [wsp (or wsp (wm/current-workspace))]
     (->>
       (concat
         ;; TODO include open-wsp on these
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

(defn do-m-x
  ([] (do-m-x nil))
  ([_]
   (let [wsp (wm/current-workspace)]
     (->> (m-x-commands {:wsp wsp})
          (r.rofi/rofi {:require-match? true
                        :msg            "Clawe commands"})))))

(defcom m-x (do-m-x))
