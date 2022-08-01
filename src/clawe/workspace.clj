(ns clawe.workspace
  (:require
   [malli.core :as m]
   [malli.transform :as mt]
   [ralphie.zsh :as zsh]
   [clawe.config :as clawe.config]
   [systemic.core :as sys :refer [defsys]]
   [ralphie.awesome :as awm]))

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
;; window manager protocol
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol ClaweWM
  "A protocol for typical window management functions."
  ;; TODO move 'current' to 'selected'
  (current-workspaces
    [this] [this opts] "Return the focused workspaces as a vector of maps.")
  (active-workspaces [this] [this opts] "Return all workspaces as a vector of maps.")
  (current-clients [this] [this opts] "Return all clients in the current active workspace.")
  (all-clients [this] [this opts] "Returns all running clients."))


(defrecord Yabai [] ClaweWM)


(defn awesome-client->clawe-client [client]
  (-> client
      (assoc :client/window-name (:awesome.client/name client))
      (assoc :client/app-name (:awesome.client/class client))
      (assoc :client/focus (:awesome.client/focus client))))

(defn tag->wsp [tag]
  (-> tag
      (assoc :workspace/index (:awesome.tag/index tag))
      (assoc :workspace/title (:awesome.tag/name tag))) )

(defrecord AwesomeWM []
  ClaweWM
  (current-workspaces [this] (current-workspaces this nil))
  (current-workspaces [_this opts]
    (->>
      ;; TODO tags-only (no clients, maybe a bit faster?)
      ;; TODO filter on current tags (even less to serialize)
      (awm/fetch-tags (merge {:include-clients false :only-current true} opts))
      (filter :awesome.tag/selected)
      (map (fn [tag]
             (cond-> tag
               (and (:include-clients opts) (:awesome/clients tag))
               (-> (assoc :workspace/clients
                          (->> tag :awesome/clients
                               (map awesome-client->clawe-client ))))

               true tag->wsp)))))

  (active-workspaces [this] (active-workspaces this nil))
  (active-workspaces [_this opts]
    (->>
      (awm/fetch-tags opts)
      (map tag->wsp))))

;; TODO break into clawe/wm ns, put protocol in there
(defsys *wm*
  :start
  (if (clawe.config/is-mac?) (Yabai.) (AwesomeWM.)))

(comment
  (sys/start! `*wm*)
  (sys/restart! `*wm*)
  (current-workspaces *wm*))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspace helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-directory (zsh/expand "~"))
(defn -ensure-directory
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

(defn -merge-with-def
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
   (sys/start! `*wm*)
   (let [default-title "home"
         default-wsp   {:workspace/title     default-title
                        :workspace/directory default-directory}]
     ;; ask wm for current wsp(s)
     (->> (current-workspaces *wm* opts)
          ;; ensure title
          (map (fn [{:keys [workspace/title] :as wsp}]
                 (if-not title
                   (assoc wsp :workspace/title default-title)
                   wsp)))
          ;; merge config workspace def
          (map -merge-with-def)
          (map -ensure-directory)
          ;; sort
          (sort-by (comp #{default-directory} :workspace/directory))
          ;; take 1
          first
          ;; if none, return a default
          ((fn [wsp] (if wsp wsp default-wsp)))))))

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
        (map -ensure-directory))))

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
   (sys/start! `*wm*)
   (->> (active-workspaces *wm* opts)
        (map -merge-with-def)
        (map -ensure-directory))))
