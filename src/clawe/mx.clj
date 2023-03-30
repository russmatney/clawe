(ns clawe.mx
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
   [clawe.client.create :as client.create]
   [clawe.doctor :as doctor]
   #_[notebooks.core :as notebooks]
   [ralphie.notify :as notify]
   [ralphie.browser :as browser]
   [ralphie.emacs :as emacs]
   [ralphie.clipboard :as clipboard]
   [org-crud.markdown :as org-crud.markdown]
   [org-crud.parse :as org-crud.parse]
   [clojure.string :as string]
   [cheshire.core :as json]
   [ralphie.re :as re]))

(defn clipboard-org->markdown
  ([] (clipboard-org->markdown nil))
  ([_]
   (->> (clipboard/get-clip "clipboard")
        string/split-lines
        org-crud.parse/parse-lines
        (org-crud.markdown/item->md-body {:drop-id-links true})
        (string/join "\n")
        clipboard/set-clip)))

(defcom/defcom convert-clipboard-org-to-markdown clipboard-org->markdown)

(defn clipboard-json->edn
  ([] (clipboard-json->edn nil))
  ([_]
   (let [json-blob (-> (clipboard/get-clip "clipboard") (string/trim))
         ;; for now assumes we're grabbing a partial object, so a leading
         ;; string means we'll wrap the whole thing as an object
         json-blob (if (re-seq #"^\"" json-blob)
                     (str "{" json-blob "}")
                     json-blob)]
     (->> json-blob
          (#(json/parse-string % true))
          str
          clipboard/set-clip))))

(defcom/defcom convert-clipboard-json-to-edn clipboard-json->edn)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; kill
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn kill-things [_wsp]
  (concat
    (tmux/rofi-kill-opts))

  ;; TODO options to kill pids running in panes?
  ;; TODO options to kill browser tabs
  ;; TODO options to kill running workspaces (and clients within)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; bb tasks
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn deps-git-urls
  ([] (deps-git-urls (wm/current-workspace)))
  ([wsp]
   (when-let [dir (:workspace/directory wsp)]
     (->> (r.bb/deps dir)
          (filter (comp :git/url second))
          (map (fn [[lib coords]]
                 (let [url (:git/url coords)]
                   (assoc coords
                          :lib lib
                          :rofi/label (str "Open git url: " lib)
                          :rofi/on-select
                          (fn [_]
                            (notify/notify (str "Opening url: " url))
                            (browser/open url))))))))))

(comment
  (deps-git-urls))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clerk notebooks
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn notebook->rofi-actions [notebook]
  (concat
    [{:rofi/label     "Eval and broadcast update"
      :rofi/on-select (fn [_]
                        (notify/notify "Rerendering notebook" (:name notebook))
                        (doctor/rerender-notebook (:name notebook)))}
     {:rofi/label     "Open .clj file in emacs"
      :rofi/on-select (fn [_]
                        (notify/notify "Opening in emacs" (:name notebook))
                        (emacs/open-in-emacs {:emacs/file-path (:path notebook)}))}
     {:rofi/label     "Open .clj file in journal-emacs (focus journal first)"
      :rofi/on-select (fn [_]
                        (notify/notify "Opening in emacs" (:name notebook))
                        (wm/show-client "journal")
                        (emacs/open-in-emacs {:emacs/file-path  (:path notebook)
                                              :emacs/frame-name "journal"}))}
     {:rofi/label     "Open in dev browser"
      :rofi/on-select (fn [_]
                        (notify/notify "Opening in dev browser" (:name notebook))
                        (browser/open-dev
                          {:url (str "http://localhost:3334/notebooks/" (:name notebook))}))}]))

;; (defn notebook->rofi-opt [notebook]
;;   (let [label (str "notebook: " (:name notebook))]
;;     (assoc notebook :rofi/label label
;;            :rofi/on-select (fn [_] (rofi/rofi {:msg label}
;;                                               (notebook->rofi-actions notebook))))))

(defn notebook-rofi-opts []
  []
  #_(->> (notebooks/notebooks) (map notebook->rofi-opt)))

(comment
  (rofi/rofi (notebook-rofi-opts))

  (doctor/rerender-notebook (:name "clawe")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; client and workspace defs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn def->rofi-fields [def]
  (->> def
       (map (fn [[k val]]
              {:rofi/label (str "["  k " " val "]")}))))

(defn show-fields [def]
  (rofi/rofi
    {:msg (str def)}
    (def->rofi-fields def)))

;; clearly a bit of a multimethod/protocol here
;; i wonder all the ways i'm using rofi

(defn client-def->actions [d]
  [{:rofi/label     "Toggle Client"
    :rofi/on-select (fn [_] (toggle/toggle d))}
   {:rofi/label     "(re)Create Client"
    :rofi/on-select (fn [_] (client.create/create-client d))}
   {:rofi/label     "Hide Client"
    :rofi/on-select (fn [_] (wm/hide-client d))}
   {:rofi/label     "Show Client"
    :rofi/on-select (fn [_] (wm/show-client d))}
   {:rofi/label     "Focus Client"
    :rofi/on-select (fn [_] (wm/focus-client d))}
   {:rofi/label     "Show Fields"
    :rofi/on-select (fn [_] (show-fields d))}])

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
  [{:rofi/label     "Open Workspace and emacs"
    :rofi/on-select (fn [_]
                      (workspace.open/open-new-workspace wsp)
                      ;; TODO may need to handle a race-case, or pass in new wsp info to avoid it
                      (client.create/create-client "emacs"))}
   (when (-> wsp :workspace/directory)
     ;; TODO not relevant for every workspace
     {:rofi/label     "Open on Github"
      :rofi/on-select (fn [_]
                        (let [dir      (:workspace/directory wsp)
                              repo-url (string/replace dir "~" "https://github.com")]
                          (browser/open {:url repo-url})))})
   {:rofi/label     "Open Workspace and terminal"
    :rofi/on-select (fn [_]
                      (workspace.open/open-new-workspace wsp)
                      ;; TODO may need to handle a race-case, or pass in new wsp info to avoid it
                      (client.create/create-client "terminal"))}
   {:rofi/label     "Open Workspace"
    :rofi/on-select (fn [_] (workspace.open/open-new-workspace wsp))}
   {:rofi/label     "Close Workspace"
    :rofi/on-select (fn [_] (wm/delete-workspace wsp))}
   {:rofi/label     "Focus Workspace"
    :rofi/on-select (fn [_] (wm/focus-workspace wsp))}
   {:rofi/label     "Show Fields"
    :rofi/on-select (fn [_] (show-fields wsp))}])

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; neil
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-neil-dep [{:keys [repo-id]}]
  ;; TODO specify a dir with the current wsp?
  (let [wsp (wm/current-workspace)]
    (tmux/fire
      {:tmux.fire/cmd       (str "neil add dep --lib " repo-id " --latest-tag")
       :tmux.fire/session   (:workspace/title wsp)
       :tmux.fire/directory (:workspace/directory wsp)})))

(comment
  (add-neil-dep {:repo-id "teknql/wing"}))

(defn rofi-neil-suggestions []
  (concat
    (->> (clipboard/values)
         (map (fn [v]
                (when-let [repo-id (re/url->repo-id v)]
                  {:repo-id        repo-id
                   :rofi/label     (str "neil add dep " repo-id " (from clipboard)")
                   :rofi/on-select add-neil-dep})))
         (filter :repo-id))
    (->> (browser/tabs)
         (map (fn [t]
                (when-let [repo-id (re/url->repo-id (:tab/url t))]
                  {:repo-id        repo-id
                   :rofi/label     (str "neil add dep " repo-id " (from open tabs)")
                   :rofi/on-select add-neil-dep})))
         (filter :repo-id))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; mx fast
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn mx-commands-fast
  ([] (mx-commands-fast nil))
  ([_]
   (->>
     (concat
       (->>
         (git/rofi-clone-suggestions-fast)
         (map (fn [x]
                (-> x
                    (assoc :rofi/label (str "clone + create wsp: " (:rofi/label x)))
                    (update :rofi/on-select
                            (fn [f]
                              ;; return a function wrapping the existing on-select
                              (fn [arg]
                                (when-let [repo-id (:repo-id x)]
                                  (workspace.open/create-workspace-def-from-path repo-id))
                                (f arg))))))))

       (rofi-neil-suggestions)

       (client-defs)
       (workspace-defs)
       ;; all defcoms
       (->> (defcom/list-commands) (map r.core/defcom->rofi))
       (notebook-rofi-opts))
     (remove nil?))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; mx full
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mx-commands
  ([] (mx-commands nil))
  ([{:keys [wsp]}]
   (let [wsp (or wsp (wm/current-workspace))]
     (->>
       (concat
         ;; open deps github page
         (deps-git-urls wsp)

         (mx-commands-fast)

         ;; run bb tasks for the current workspace
         (bb-tasks-for-wsp wsp)

         ;; open a known workspace
         (workspace.open/open-workspace-rofi-options)

         ;; all bindings
         (->> (defkbd/list-bindings) (map defkbd/->rofi))

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
  (mx-commands)

  (->>
    (mx-commands)
    (filter :defcom/name)
    (filter (comp #(re-seq #"key" %) :defcom/name))
    (first)
    :rofi/on-select
    ((fn [f] (f)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mx
  "Run rofi with commands created in `mx-commands`."
  ([] (mx nil))
  ([_]
   (let [wsp (wm/current-workspace)]
     (->> (mx-commands {:wsp wsp})
          (rofi/rofi {:require-match? true
                      :msg            "Clawe commands"})))))

(defn mx-fast
  "Run rofi with commands created in `mx-commands-fast`."
  ([] (mx-fast nil))
  ([_]
   (->> (mx-commands-fast)
        (rofi/rofi {:require-match? true
                    :msg            "Clawe commands (fast)"}))))

(comment
  (mx)
  (mx-fast))
