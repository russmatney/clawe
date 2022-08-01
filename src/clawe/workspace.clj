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
   [:workspace/initial-file {:optional true} string?]])

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
  (current-workspaces
    [this] [this opts] "Return the focused workspaces as a vector of maps.")
  (all-workspaces [this] "Return all workspaces as a vector of maps.")
  (current-clients [this] [this opts] "Return all clients in the current active workspace.")
  (all-clients [this] [this opts] "Returns all running clients."))

(defrecord Yabai [] ClaweWM)
(defrecord AwesomeWM []
  ClaweWM
  (current-workspaces [this] (current-workspaces this nil))
  (current-workspaces [_this opts]
    (->>
      ;; TODO tags-only (no clients, maybe a bit faster?)
      ;; TODO filter on current tags (even less to serialize)
      (awm/fetch-tags
        (merge {:include-clients false :only-active true} opts))
      (filter :awesome.tag/selected)
      (map (fn [tag]
             (cond-> tag
               (and (:include-clients opts) (:awesome/clients tag))
               (-> tag
                   (assoc
                     :workspace/clients
                     (->>
                       (:awesome/clients tag)
                       (map (fn [client]
                              (-> client
                                  (assoc :client/window-name (:awesome.client/name client))
                                  (assoc :client/app-name (:awesome.client/class client))
                                  (assoc :client/focus (:awesome.client/focus client))))))))

               true
               (-> tag
                   (assoc :workspace/index (:awesome.tag/index tag))
                   (assoc :workspace/title (:awesome.tag/name tag)))))))))

(defsys *wm*
  :start
  (if (clawe.config/is-mac?) (Yabai.) (AwesomeWM.)))

(comment
  (sys/start! `*wm*)
  (sys/restart! `*wm*)
  (current-workspaces *wm*))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current
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
  ([] (current nil))
  ([opts]
   (sys/start! `*wm*)
   (let [default-title     "home"
         default-directory (zsh/expand "~")
         default-wsp       {:workspace/title     default-title
                            :workspace/directory default-directory}]
     ;; ask wm for current wsp(s)
     (->> (current-workspaces *wm* opts)
          ;; ensure title
          (map (fn [{:keys [workspace/title] :as wsp}]
                 (if-not title
                   (assoc wsp :workspace/title default-title)
                   wsp)))
          ;; merge config workspace def
          (map (fn [{:keys [workspace/title] :as wsp}]
                 (merge wsp (clawe.config/workspace-def title))))
          ;; ensure workspace directory
          (map (fn [{:keys [workspace/directory] :as wsp}]
                 (cond
                   (not directory)
                   (assoc wsp :workspace/directory default-directory)

                   (re-seq #"^~" directory)
                   (assoc wsp :workspace/directory (zsh/expand directory))

                   :else wsp)))
          ;; sort
          (sort-by (comp #{default-directory} :workspace/directory))
          ;; take 1
          first
          ;; if none, return a default
          ((fn [wsp] (if wsp wsp default-wsp)))))))

