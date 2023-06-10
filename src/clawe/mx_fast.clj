(ns timer)

(def start-t (atom (System/currentTimeMillis)))
(def last-t (atom nil))

(defn print-since [line]
  (let [now      (System/currentTimeMillis)
        old-last @last-t]
    (reset! last-t now)
    (println "|" (when old-last (- now old-last)) "\t|" (- now @start-t) "\t|" line)))


(ns clawe.mx-fast
  (:require
   [clojure.string :as string]

   [defthing.defcom :as defcom]
   [defthing.defkbd :as defkbd]
   [ralphie.browser :as browser]
   ;; [ralphie.clipboard :as clipboard]
   [ralphie.core :as r.core]
   ;; [ralphie.git :as git]
   ;; [ralphie.re :as re]
   [ralphie.rofi :as rofi]
   ;; [ralphie.tmux :as tmux]

   [clawe.client.create :as client.create]
   [clawe.config :as clawe.config]
   [clawe.doctor :as doctor]
   [clawe.toggle :as toggle]
   [clawe.wm :as wm]
   [clawe.workspace.open :as workspace.open]))

(timer/print-since "clawe.mx-fast\tNamespace (and deps) Loaded")

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

;; (defn add-neil-dep [{:keys [repo-id]}]
;;   ;; TODO specify a dir with the current wsp?
;;   (let [wsp (wm/current-workspace)]
;;     (tmux/fire
;;       {:tmux.fire/cmd       (str "neil add dep --lib " repo-id " --latest-tag")
;;        :tmux.fire/session   (:workspace/title wsp)
;;        :tmux.fire/directory (:workspace/directory wsp)})))

;; (comment
;;   (add-neil-dep {:repo-id "teknql/wing"}))

;; (defn rofi-neil-suggestions []
;;   (concat
;;     (->> (clipboard/values)
;;          (map (fn [v]
;;                 (when-let [repo-id (re/url->repo-id v)]
;;                   {:repo-id        repo-id
;;                    :rofi/label     (str "neil add dep " repo-id " (from clipboard)")
;;                    :rofi/on-select add-neil-dep})))
;;          (filter :repo-id))
;;     (->> (browser/tabs)
;;          (map (fn [t]
;;                 (when-let [repo-id (re/url->repo-id (:tab/url t))]
;;                   {:repo-id        repo-id
;;                    :rofi/label     (str "neil add dep " repo-id " (from open tabs)")
;;                    :rofi/on-select add-neil-dep})))
;;          (filter :repo-id))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; mx fast
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mx-commands-fast
  ([] (mx-commands-fast nil))
  ([_]
   (timer/print-since "clawe.mx/mx-commands-fast\tstart")
   (->>
     (concat
       ;; (->>
       ;;   (git/rofi-clone-suggestions-fast)
       ;;   (map (fn [x]
       ;;          (-> x
       ;;              (assoc :rofi/label (str "clone + create wsp: " (:rofi/label x)))
       ;;              (update :rofi/on-select
       ;;                      (fn [f]
       ;;                        ;; return a function wrapping the existing on-select
       ;;                        (fn [arg]
       ;;                          (when-let [repo-id (:repo-id x)]
       ;;                            (workspace.open/create-workspace-def-from-path repo-id))
       ;;                          (f arg))))))))

       ;; (timer/print-since "clawe.mx/mx-commands-fast\trofi-clone-suggs")

       ;; (rofi-neil-suggestions)
       ;; (timer/print-since "clawe.mx/mx-commands-fast\trofi-neil-suggs")

       (client-defs)
       (timer/print-since "clawe.mx/mx-commands-fast\tclient-defs")
       (workspace-defs)
       (timer/print-since "clawe.mx/mx-commands-fast\tworkspace-defs")
       ;; all defcoms
       (->> (defcom/list-commands) (map r.core/defcom->rofi))
       (timer/print-since "clawe.mx/mx-commands-fast\tdefcoms")
       (blog-rofi-opts)
       (timer/print-since "clawe.mx/mx-commands-fast\tblog-opts")

       ;; all bindings
       (->> (defkbd/list-bindings) (map defkbd/->rofi))

       (timer/print-since "clawe.mx/mx-commands-fast\tbindings"))
     (remove nil?))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mx-fast
  "Run rofi with commands created in `mx-commands-fast`."
  ([] (mx-fast nil))
  ([_]
   (timer/print-since "clawe.mx/mx-fast\tstart")
   (->> (mx-commands-fast)
        (#(do (timer/print-since "clawe.mx/mx-fast\tcommands fast") %))
        (rofi/rofi {:require-match? true
                    :msg            "Clawe commands (fast)"
                    :cache-id       "clawe-mx-fast"}))
   (timer/print-since "clawe.mx-fast\tend")))

(comment

  (mx-fast)

  (time (mx-commands-fast))

  )
