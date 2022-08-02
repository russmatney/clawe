(ns clawe.workspace
  (:require
   [malli.core :as m]
   [malli.transform :as mt]

   [clawe.config :as clawe.config]
   [clawe.wm :as wm]
   [ralphie.zsh :as zsh]
   [ralphie.tmux :as tmux]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; schema
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def schema
  [:map
   [:workspace/title string?]
   [:workspace/directory string?]
   [:workspace/index int?]
   [:workspace/initial-file {:optional true} string?]
   ;; extra app names used to match on clients when applying clawe.rules
   [:workspace/app-names {:optional true} [:vector string?]]])

(comment
  (m/decode
    schema
    {:workspace/title     "journal"
     :workspace/directory "~/todo"
     :workspace/app-names "hi"
     :gibber              :jabber}
    (mt/strip-extra-keys-transformer))

  (m/validate
    schema
    {:workspace/title     "journal"
     :workspace/directory "~/todo"
     :workspace/index     0
     :workspace/app-names ["hi"]
     :gibber              :jabber}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspace helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-directory (zsh/expand "~"))
(defn- ensure-directory
  "Makes sure the passed workspace has a `:workspace/directory` set.

  If missing, sets it to `default-directory`.

  If it starts with `~`, `zsh/expands` it."
  [{:keys [workspace/directory] :as wsp}]
  (cond
    (not directory)
    (assoc wsp :workspace/directory default-directory)

    (re-seq #"^~" directory)
    (assoc wsp :workspace/directory (zsh/expand directory))

    :else wsp))

(defn- merge-with-def
  "Merges the passed workspace with a workspace-def from resouces/clawe.edn.

  Matches using the `:workspace/title`."
  [{:keys [workspace/title] :as wsp}]
  (merge wsp (clawe.config/workspace-def title)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current
  "Returns the current workspace according to the window manager.

  At minimum, this is a :workspace/title and :workspace/directory, but it includes
  other keys set by the window manager implementation.

  The directory defaults to `(zsh/expand \"~\")`, after checking against any
  workspace defs in `clawe.config` - these are ultimately stored/read from
  `resources/clawe.edn`. There are client and workspace install helpers
  to crud that data.

  Some WMs can have multiple current-workspaces at once (awesomeWM's tags).
  In that case, the first workspace with a :workspace/directory other than the default is taken,
  because it is presumably the 'repo-based' workspace (rather than the app or scratchpad based wsp).
  "
  ([] (current nil))
  ([opts]
   (let [default-title "home"
         default-wsp   {:workspace/title     default-title
                        :workspace/directory default-directory}]
     ;; ask wm for current wsp(s)
     (->> (wm/current-workspaces opts)
          ;; ensure title
          (map (fn [{:keys [workspace/title] :as wsp}]
                 (if-not title
                   (assoc wsp :workspace/title default-title)
                   wsp)))
          ;; merge config workspace def
          (map merge-with-def)
          (map ensure-directory)
          ;; sort
          (sort-by (comp #{default-directory} :workspace/directory))
          ;; take 1
          first
          ;; if none, return a default
          ((fn [wsp] (if wsp wsp default-wsp)))))))

(comment
  (current {:include-clients true}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; all defs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn all-defs
  "Returns all possible workspaces, as supported by `clawe.config/workspace-defs`,
  (backed by `resources/clawe.edn`).

  Supports opening a new workspace."
  ([] (all-defs nil))
  ([_opts]
   (->> (clawe.config/workspace-defs)
        (map (fn [[title def]] (assoc def :workspace/title title)))
        (map ensure-directory))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; all active
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn all-active
  "Returns all active workspaces.
  An active workspace is 'open', but not necessarily in focus.

  Merges workspace definitions with `clawe/config/workspace-defs`,
  which provide `:workspace/directory` and other workspace metadata.
  "
  ([] (all-active nil))
  ([opts]
   (->> (wm/active-workspaces opts)
        (map merge-with-def)
        (map ensure-directory))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; tmux session merging
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn merge-tmux-sessions
  "assumes the workspace title and tmux session are the same"
  ([wsps] (merge-tmux-sessions {} wsps))
  ([_opts wsps]
   (when-let [sessions-by-name (try (tmux/list-sessions)
                                    (catch Exception _e
                                      (println "Tmux probably not running!")
                                      nil))]
     (->> wsps
          (map (fn [{:workspace/keys [title] :as wsp}]
                 (if-let [sesh (sessions-by-name title)]
                   (assoc wsp :tmux/session sesh)
                   wsp)))))))
