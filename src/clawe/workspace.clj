(ns clawe.workspace
  (:require
   [malli.core :as m]
   [malli.transform :as mt]
   [ralphie.zsh :as zsh]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; schema
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def schema
  [:map
   [:workspace/title string?]
   [:workspace/directory string?]])

(comment
  (m/decode
    schema
    {:workspace/title     "journal"
     :workspace/directory "~/todo"
     :gibber              :jabber}
    (mt/strip-extra-keys-transformer))

  (m/validate
    schema
    {:workspace/title     "journal"
     :workspace/directory "~/todo"
     :gibber              :jabber}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; window manager protocol
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol ClaweWM
  "A protocol for typical window management functions."
  (current-workspaces [this] "Return the focused workspaces as a vector of maps.")
  (current-workspaces-full [this] "Return focused workspaces, with client data attached.")
  (all-workspaces [this] "Return all workspaces as a vector of maps.")
  (all-workspaces-full [this] "Return all workspaces, with client data attached."))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current-workspace
  "Returns the current workspace, which is at minimum a :workspace/title
  and :workspace/directory.

  The directory defaults to `(zsh/expand \"~\")`, after checking against any
  workspace defs in `clawe.config` - these are ultimately stored/read from
  `resources/clawe.edn`. There are client and workspace install helpers
  to crud that data.

  Some WMs can have multiple current-workspaces at once (awesomeWM's tags).
  In that case, the first workspace with a :workspace/directory other than the default is taken,
  because it is presumably the 'repo-based' workspace (rather than the app or scratchpad based wsp).
  "
  []
  ;; ask wm for current wsp(s)
  ;; get title for each
  ;; get wsp def from config for title
  ;; sort non-home dir
  ;; take first

  {:workspace/title     "home"
   :workspace/directory (zsh/expand "~")})


(defn current-workspace-full
  "Slower than `current-workspace`, but includes more information/data.
  (like workspace/clients? git status? db merging?)"
  []
  (current-workspace))
