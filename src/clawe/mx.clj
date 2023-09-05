(ns clawe.mx
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
   [org-crud.markdown :as org-crud.markdown]
   [org-crud.parse :as org-crud.parse]

   [defthing.defcom :as defcom]
   [defthing.defkbd :as defkbd]
   [ralphie.bb :as r.bb]
   [ralphie.browser :as browser]
   [ralphie.clipboard :as clipboard]
   [ralphie.core :as r.core]
   [ralphie.git :as git]
   [ralphie.notify :as notify]
   [ralphie.re :as re]
   [ralphie.rofi :as rofi]
   [ralphie.systemd :as systemd]
   [ralphie.tmux :as tmux]

   [clawe.client.create :as client.create]
   [clawe.config :as clawe.config]
   [clawe.doctor :as doctor]
   [clawe.toggle :as toggle]
   [clawe.wm :as wm]
   [clawe.workspace.open :as workspace.open]
   [clawe.workspace :as workspace]

   [timer :as timer]))

(timer/print-since "clawe.mx\tNamespace (and deps) Loaded")


(defn lines->paragraphs [s]
  (->> s
       string/split-lines
       (partition-by #{""})
       (map #(string/join " " %))
       (string/join "\n")))

(comment
  (lines->paragraphs
    "this is a line
with some more content
one new lines

and a new paragraph
with more content
below

hi there
"))

(defn clipboard-lines->paragraphs
  ([] (clipboard-lines->paragraphs nil))
  ([_] (->> (clipboard/get-clip "clipboard") lines->paragraphs clipboard/set-clip)))

(defcom/defcom convert-clipboard-lines-to-paragraphs clipboard-lines->paragraphs)

(defn clipboard-org->markdown
  ([] (clipboard-org->markdown nil))
  ([_]
   (->> (clipboard/get-clip "clipboard")
        string/split-lines
        org-crud.parse/parse-lines
        ;; TODO fix! this seems to drop everything
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

(comment
  (bb-tasks-for-wsp))

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
;; blog rofi
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn blog-rofi-opts []
  [{:rofi/label     "Rebuild Blog"
    :rofi/on-select doctor/rebuild-blog}
   {:rofi/label     "Rebuild Blog Indexes"
    :rofi/on-select doctor/rebuild-blog-indexes}
   {:rofi/label     "Rebuild Open Pages"
    :rofi/on-select doctor/rebuild-blog-open-pages}
   {:rofi/label     "Restart Blog Systems"
    :rofi/on-select doctor/restart-blog-systems}])

(comment
  (rofi/rofi (blog-rofi-opts)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; common urls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn common-url-opts []
  (concat
    (->>
      (clawe.config/local-dev-urls)
      (map (fn [{:keys [url label]}]
             {:rofi/label     (str "Dev Browser:\t" label "\t" url)
              :rofi/on-select (fn [_]
                                ;; TODO could have a pre-hook for ensuring the server for these urls
                                ;; e.g. tmux/fire bb serve in the right repo + tmux session
                                (browser/open-dev url))})))

    (->>
      (clawe.config/common-urls)
      (map (fn [{:keys [url label]}]
             {:rofi/label     (str "Browser:\t" label "\t" url)
              :rofi/on-select (fn [_] (browser/open url))})))))

(comment
  (rofi/rofi (common-url-opts)))

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

(declare mx)

(defn wsp-def->actions [wsp]
  [(when (-> wsp :workspace/directory)
     ;; TODO not relevant for every workspace
     {:rofi/label     "Open on Github"
      :rofi/on-select (fn [_]
                        (let [dir      (:workspace/directory wsp)
                              repo-url (string/replace dir "~" "https://github.com")]
                          (browser/open {:url repo-url})))})
   {:rofi/label     "Show Fields"
    :rofi/on-select (fn [_] (show-fields wsp))}
   {:rofi/label     "Open Workspace and terminal"
    :rofi/on-select (fn [_]
                      (workspace.open/open-new-workspace wsp)

                      ;; invoke clawe-mx to warm up the mx-cache for this workspace
                      ;; maybe want an option to NOT open rofi in this case
                      (mx {:wsp wsp})

                      ;; TODO may need to handle a race-case, or pass in new wsp info to avoid it
                      (client.create/create-client "terminal"))}
   {:rofi/label     "Open Workspace and emacs"
    :rofi/on-select (fn [_]
                      (workspace.open/open-new-workspace wsp)

                      ;; invoke clawe-mx to warm up the mx-cache for this workspace
                      ;; maybe want an option to NOT open rofi in this case
                      (mx {:wsp wsp})

                      ;; TODO may need to handle a race-case, or pass in new wsp info to avoid it
                      (client.create/create-client "emacs"))}
   {:rofi/label     "Open Workspace"
    :rofi/on-select (fn [_]
                      (workspace.open/open-new-workspace wsp)

                      ;; invoke clawe-mx to warm up the mx-cache for this workspace
                      ;; maybe want an option to NOT open rofi in this case
                      (mx {:wsp wsp}))}
   {:rofi/label     "Close Workspace"
    :rofi/on-select (fn [_] (wm/delete-workspace wsp))}
   {:rofi/label     "Focus Workspace"
    :rofi/on-select (fn [_] (wm/focus-workspace wsp))}])

(defn wsp-action-rofi [wsp]
  (rofi/rofi
    {:msg "Workspace def actions"}
    (wsp-def->actions wsp)))

(defn open-new-wsp-with-emacs [w]
  (workspace.open/open-new-workspace w)

  ;; future so this doesn't block
  (future
    ;; invoke clawe-mx to warm up the mx-cache for this workspace
    ;; maybe want an option to NOT open rofi in this case
    (mx {:wsp w}))

  ;; TODO may need to handle a race-case, or pass in new wsp info to avoid it
  (client.create/create-client "emacs"))

(defn test-fn [key]
  (if-let [wm (wm/key->workspace key)]
    (open-new-wsp-with-emacs wm)
    (println "couldn't find a wsp for def key" key)))

(comment
  (->> (wm/workspace-defs)
       (filter (comp #{"clove"} :workspace/title))
       first)
  (test-fn "clove")
  )

(defn workspace-defs []
  (->>
    (clawe.config/workspace-defs-with-titles)
    vals
    (mapcat
      (fn [w]
        [(-> w (assoc :rofi/label (str "wsp-open: " (:workspace/title w))
                      :rofi/description (:workspace/directory w)
                      :rofi/on-select open-new-wsp-with-emacs))
         (-> w (assoc :rofi/label (str "wsp-actions: " (:workspace/title w))
                      :rofi/description (:workspace/directory w)
                      :rofi/on-select
                      (fn [w] (wsp-action-rofi w))))]))))

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
;; mx-ctx-suggestions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn common-wsps []
  (->>
    (workspace-defs)
    (filter :workspace/directory)
    (filter (comp #(re-seq #"russmatney" %) :workspace/directory))))

(def common-wsps-mem (memoize common-wsps))

(defn mx-suggestion-commands
  "Commands that support dynamic context (e.g. open browser tabs).

  Commands that cannot be memoized,
  plus useful commands to have when the server is down (e.g. opening a workspace to fix it)."
  ([] (mx-suggestion-commands nil))
  ([{:keys [_wsp]}]
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
                                  (workspace.open/create-workspace-def-from-path repo-id)
                                  (notify/notify "Created wsp: " repo-id))
                                (f arg))))))))

       (rofi-neil-suggestions)

       ;; TODO show only common but unopen workspaces (clawe, dotfiles, dino)
       (common-wsps-mem))
     (remove nil?))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; mx fast
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mx-commands-fast
  ([] (mx-commands-fast nil))
  ([_]
   (->>
     (concat
       (->> (defcom/list-commands) (map r.core/defcom->rofi))
       (workspace-defs)
       (client-defs)
       (common-url-opts)
       (blog-rofi-opts)
       (->> (defkbd/list-bindings) (map defkbd/->rofi))

       [{:rofi/label "Drag workspace left"
         :rofi/on-select
         (fn [_]
           (wm/drag-workspace
             ;; up/down as the values here is crazy to me. who wrote this?!?
             :dir/down)
           (doctor/update-topbar))}
        {:rofi/label "Drag workspace right"
         :rofi/on-select
         (fn [_]
           (wm/drag-workspace :dir/up)
           (doctor/update-topbar))}])
     (remove nil?))))

;; TODO need to bust this, e.g. when new workspace defs are created
(def mx-commands-fast-memoized (memoize mx-commands-fast))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; mx full
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mx-commands
  ([] (mx-commands nil))
  ([{:keys [wsp]}]
   (let [wsp (or wsp (wm/current-workspace))]
     (->>
       (concat
         ;; common repo commands
         [(when-let [dir (:workspace/directory wsp)]
            {:rofi/label "Fetch latest"
             :rofi/on-select
             (fn [_] (tmux/fire
                       {:tmux.fire/cmd       "git fetch"
                        :tmux.fire/session   (:workspace/title wsp)
                        :tmux.fire/directory dir}))})
          (when-let [dir (:workspace/directory wsp)]
            {:rofi/label "Pull latest"
             :rofi/on-select
             (fn [_] (tmux/fire
                       {:tmux.fire/cmd       "git pull"
                        :tmux.fire/session   (:workspace/title wsp)
                        :tmux.fire/directory dir}))})]

         ;; open deps github pages
         (deps-git-urls wsp)

         ;; run bb tasks for the current workspace
         (bb-tasks-for-wsp wsp)

         (mx-commands-fast)

         ;; kill tmux/tags/clients
         (when-not (clawe.config/is-mac?)
           (kill-things wsp))

         ;; systemd stops/restart
         (when-not (clawe.config/is-mac?)
           (systemd/rofi-service-opts #(str "Restart " %) :systemd/restart))
         (when-not (clawe.config/is-mac?)
           (systemd/rofi-service-opts #(str "Stop " %) :systemd/stop)))
       (remove nil?)))))

;; TODO need to bust this cache from time to time, eg. when a new bb task is created
(def mx-commands-memoized (memoize mx-commands))

(comment
  (mx-commands))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mx
  "Run rofi with commands created in `mx-commands`."
  ([] (mx nil))
  ([{:keys [wsp]}]
   (timer/print-since "clawe.mx/mx\tstart")
   (let [wsp (or wsp (wm/current-workspace))]
     (timer/print-since "clawe.mx/mx\tfetched current workspace (or it's lazy?)")
     (->> (mx-commands-memoized {:wsp (workspace/strip wsp)})
          (#(do (timer/print-since "clawe.mx/mx\tcommands") %))
          (rofi/rofi {:require-match? true
                      :msg            "Clawe commands"
                      :cache-id       "clawe-mx"})))
   (timer/print-since "clawe.mx\tend")))

(defn mx-fast
  "Run rofi with commands created in `mx-commands-fast`."
  ([] (mx-fast nil))
  ([_]
   (timer/print-since "clawe.mx/mx-fast\tstart")
   (let [cmds (mx-commands-fast-memoized)]
     (->> cmds
          (#(do (timer/print-since "clawe.mx/mx-fast\tcommands fast") %))
          (rofi/rofi {:require-match? true
                      :msg            "Clawe commands (fast)"
                      :cache-id       "clawe-mx-fast"})))
   (timer/print-since "mx-fast\tend")))

(defn mx-suggestions
  "Run rofi with commands created in `mx-commands`."
  ([] (mx-suggestions nil))
  ([_]
   (timer/print-since "clawe.mx/mx-suggestions\tstart")
   (->> (mx-suggestion-commands)
        (#(do (timer/print-since "clawe.mx/mx-suggestions\tcommands") %))
        (rofi/rofi {:require-match? true
                    :msg            "Clawe suggestions"}))
   (timer/print-since "mx-suggestions\tend")))
