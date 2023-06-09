(println "Clawe mx ns load")

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
   [clawe.workspace.open :as workspace.open]))


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
       (blog-rofi-opts)

       ;; all bindings
       (->> (defkbd/list-bindings) (map defkbd/->rofi)))
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
   (println "Clawe mx start")
   (let [wsp (wm/current-workspace)]
     (println "Clawe mx wsp pulled")
     (->> (mx-commands {:wsp wsp})
          (rofi/rofi {:require-match? true
                      :msg            "Clawe commands"
                      :cache-id       "clawe-mx"}))
     (println "Clawe mx stop"))))

(defn mx-fast
  "Run rofi with commands created in `mx-commands-fast`."
  ([] (mx-fast nil))
  ([_]
   (println "Clawe mx-fast start")
   (->> (mx-commands-fast)
        (rofi/rofi {:require-match? true
                    :msg            "Clawe commands (fast)"
                    :cache-id       "clawe-mx-fast"}))
   (println "Clawe mx-fast stop")))

(comment
  (mx-commands {:wsp (wm/current-workspace)})

  (mx)
  (mx-fast))
