(ns clawe.wm
  (:require
   [babashka.fs :as fs]
   [clojure.string :as string]
   [systemic.core :as sys :refer [defsys]]

   [clawe.awesome :as clawe.awesome]
   [clawe.config :as clawe.config]
   [clawe.wm.protocol :as wm.protocol]
   [clawe.hyprland :as clawe.hyprland]
   [clawe.i3 :as clawe.i3]
   [clawe.sway :as clawe.sway]
   [clawe.yabai :as clawe.yabai]
   [clawe.client :as client]
   [ralphie.emacs :as r.emacs]
   [ralphie.config :as r.config]
   [ralphie.tmux :as r.tmux]
   [timer :as timer])
  (:import
   [clawe.awesome Awesome]
   [clawe.yabai Yabai]
   [clawe.i3 I3]
   [clawe.sway Sway]
   [clawe.hyprland Hyprland]
   ))


(timer/print-since "clawe.wm ns loading")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; window manager sys
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsys ^:dynamic *wm*
  :deps
  [clawe.config/*config*]
  :start
  (let [wm (clawe.config/get-wm)]
    (cond
      (= wm :wm/sway)     (Sway.)
      (= wm :wm/i3)       (I3.)
      (= wm :wm/yabai)    (Yabai.)
      (= wm :wm/awesome)  (Awesome.)
      (= wm :wm/hyprland) (Hyprland.)
      :else
      (do
        #_(notify/notify "unknown window manager!")
        (if (r.config/osx?) (Yabai.) (I3.))))))

(defn reload-wm []
  (when (sys/running? `*wm*)
    (sys/restart! `*wm*)))

(comment
  (reload-wm)
  (sys/start! `*wm*))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspace helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- ensure-directory
  "Makes sure the passed workspace has a `:workspace/directory` set.

  If missing, sets it to `default-directory`.

  If it starts with `~`, `zsh/expands` it."
  [{:keys [workspace/directory] :as wsp}]
  (cond
    (not directory)
    (assoc wsp :workspace/directory (str (fs/home)))

    (re-seq #"^~" directory)
    (update wsp :workspace/directory #(string/replace % "~" (str (fs/home))))

    :else wsp))

(defn- merge-with-workspace-def
  "Merges the passed workspace with a workspace-def from resouces/clawe.edn.

  Matches using the `:workspace/title`."
  [{:keys [workspace/title] :as wsp}]
  (merge wsp (clawe.config/workspace-def title)))

(defn- merge-with-client-def
  "Attempts to merge the passed client with a matching client-def from resouces/clawe.edn.

  Matches using `client/match?`."
  ;; TODO should we supply a workspace-title here for use-workspace-title matches/merges?
  [client]
  (let [defs (clawe.config/client-defs)
        matches
        (->> defs (filter
                    (fn [def]
                      ;; does this imply that clients should carry :match opts?
                      (client/match?
                        ;; client-def can supply specific :match options
                        ;; client def can 'merge' more freely (via :merge/skip-title)
                        (if (:merge/skip-title def)
                          (assoc def :match/skip-title true)
                          def)
                        client def))))]
    (cond
      (> (count matches) 1)
      (do
        (println "WARN: multiple matching defs found for client" (client/strip client) matches)
        (merge client (some->> matches first)))

      (= (count matches) 0)
      ;; TODO this works for most cases but breaks when skipping title on emacs matches
      ;; b/c the journal and wsp-emacs clients both match for skip-title emacs clients
      ^{:clj-kondo/ignore [:redundant-do]}
      (do
        #_(println "WARN: zero matching defs found for client" (client/strip client))
        client)

      :else
      (merge client (some->> matches first)))))

(defn- merge-client-defs [wsp]
  (update wsp :workspace/clients #(->> % (map merge-with-client-def))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspace fetching
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspace-defs
  "Returns all possible workspaces, as supported by `clawe.config/workspace-defs`,
  (backed by `resources/clawe.edn`).

  Supports opening a new workspace."
  ([] (workspace-defs nil))
  ([_opts]
   (->> (clawe.config/workspace-defs)
        (map (fn [[title def]] (assoc def :workspace/title title)))
        (map ensure-directory))))

(defn key->workspace [key]
  (some->> (workspace-defs)
           (filter (comp #{key} :workspace/title))
           first))

(defn current-workspaces
  ([] (current-workspaces nil))
  ([opts]
   (timer/print-since "current-workspaces start")
   (sys/start! `*wm*)
   (->>
     (wm.protocol/-current-workspaces *wm* opts)
     ;; TODO apply malli transform here instead of in the protocols
     (map merge-with-workspace-def)
     (map ensure-directory)
     (map merge-client-defs))))

(defn current-workspace
  ([] (current-workspace nil))
  ([opts]
   (timer/print-since "current-workspace start")
   (sys/start! `*wm*)
   (some->> (current-workspaces opts) first)))

(comment
  (current-workspace))

(defn active-workspaces
  ([] (active-workspaces nil))
  ([opts]
   (sys/start! `*wm*)
   (->> (wm.protocol/-active-workspaces *wm* opts)
        ;; TODO apply malli transform here instead of in the protocols?
        (map merge-with-workspace-def)
        (map ensure-directory)
        (map merge-client-defs))))

(defn fetch-workspace
  ([workspace-or-title] (fetch-workspace nil workspace-or-title))
  ([opts workspace-or-title]
   (sys/start! `*wm*)
   (let [title (if (string? workspace-or-title)
                 workspace-or-title (:workspace/title workspace-or-title))]
     (some->
       (wm.protocol/-fetch-workspace *wm* opts title)
       merge-with-workspace-def
       ensure-directory
       merge-client-defs))))

(comment
  clawe.config/*config*
  (clawe.config/reload-config)
  (current-workspace)
  (fetch-workspace "madeup")
  (fetch-workspace {:include-clients true}
                   {:workspace/title "slack"})
  (->>
    (active-workspaces)
    (remove :workspace/title))
  (wm.protocol/-fetch-workspace *wm* nil "invented"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; client fetching
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn client-defs
  "Returns client-defs, as defined in `resources/clawe.edn`.
  Supports opening new clients, finding matching clients, and toggling clients into view."
  ([] (client-defs nil))
  ([_opts]
   (clawe.config/client-defs)))

;; we could be locally caching calls like these, if we're willing to cache burst
(defn active-clients
  ([] (active-clients nil))
  ([opts]
   (timer/print-since "clawe.wm/active-clients started")

   (sys/start! `*wm*)

   (timer/print-since "*wm* started")
   (->>
     (wm.protocol/-active-clients *wm* opts)
     (map merge-with-client-def))))

(defn focused-client []
  ;; TODO refactor into the protocol (perf)
  (some->>
    (active-clients)
    (filter :client/focused)
    first))

(defn fetch-client [client]
  (when-let [client (cond (map? client)    client
                          (string? client) (clawe.config/client-def client))]
    ;; TODO refactor into protocol (perf)
    (some->>
      (active-clients)
      (filter (partial client/match? client client))
      first)))

(comment
  (fetch-client "twitch-chat")
  (clawe.config/client-def "twitch-chat")
  (clawe.config/reload-config)

  (clawe.config/client-def "web")

  (->>
    (active-clients)
    (map client/strip)
    )

  (some->>
    (active-clients)
    (take 3)
    (filter (partial client/match?
                     (clawe.config/client-def "web")))
    first
    )

  (fetch-client "web")
  (fetch-client "journal")
  (focused-client)
  (active-clients))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; interactions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; crud

(defn create-workspace
  ([workspace-or-title] (create-workspace nil workspace-or-title))
  ([opts workspace-or-title]
   (sys/start! `*wm*)
   (let [title (if (string? workspace-or-title)
                 workspace-or-title (:workspace/title workspace-or-title))]
     (wm.protocol/-create-workspace *wm* opts title)

     ;; ensure tmux and emacs sessions exist for this title
     ;; perhaps we want to opt-in/out of these?
     (r.tmux/ensure-session {:tmux/session-name title})
     (r.emacs/ensure-workspace {:emacs/workspace-name title}))))

(defn delete-workspace
  [workspace]
  (sys/start! `*wm*)
  (wm.protocol/-delete-workspace *wm* workspace))

(comment
  (->>
    (active-workspaces)
    (remove :workspace/title)
    #_(map delete-workspace)
    ))

(defn close-client
  ([client] (close-client nil client))
  ([opts client]
   (sys/start! `*wm*)
   (wm.protocol/-close-client *wm* opts client)))

(comment
  (close-client nil))

;; focus

(defn focus-workspace
  ([workspace] (focus-workspace nil workspace))
  ([opts workspace]
   (sys/start! `*wm*)
   (wm.protocol/-focus-workspace *wm* opts workspace)))

(defn open-new-workspace [wsp]
  ;; not all wms need the wsp to exist... some 'create' via 'focus'
  (when-not (fetch-workspace wsp)
    (create-workspace wsp))
  (focus-workspace wsp))

(defn bury-client
  ([client] (bury-client nil client))
  ([opts client] (wm.protocol/-bury-client *wm* opts client)))

(defn bury-clients
  ([] (bury-clients nil (active-clients)))
  ([clients] (bury-clients nil clients))
  ([opts clients]
   (->>
     clients
     (remove :bury/ignore)
     (wm.protocol/-bury-clients *wm* opts))))

(defn focus-client
  "Intended as a send-focus only - does not pull clients to the workspace.
  (unless you are on osx ...?)."
  ([client] (focus-client
              ;; client supplies opts in single arity case
              client client))
  ([opts client]
   (sys/start! `*wm*)
   (let [client (cond (map? client)    client
                      (string? client) (fetch-client client))]

     (try
       ;; maybe we want bury to be per-client configurable?
       ;; i think it'd default to true...
       (bury-clients (:workspace/clients
                      (or (:current-workspace opts)
                          (current-workspace
                            {:prefetched-clients (active-clients)}))))
       (catch Exception e
         (println e)
         (println "[WARN]: bury-clients not impled (or some other error)")))

     (wm.protocol/-focus-client *wm* opts client))))

(declare move-client-to-workspace)
(defn show-client
  "Opts `:current-workspace`, `:focus/float-and-center`, `focus-client` opts"
  ([client] (show-client nil client))
  ([opts client]

   ;; TODO support showing via i3 scratchpad mechanics (faster, less jank)

   (let [client (cond (map? client)    client
                      (string? client) (fetch-client client))]
     ;; TODO would like to focus+center first to avoid the jank on linux
     ;; but on osx, do it the other way to avoid the auto-space-switch.
     ;; suppose we need to float and center, then switch workspaces, then focus
     (move-client-to-workspace
       client (or (:current-workspace opts) (current-workspace)))

     (focus-client (merge {:float-and-center (:focus/float-and-center client true)} opts)
                   client))))

(comment
  (fetch-client "web")
  (show-client "web")

  (:workspace/clients
   (current-workspace
     {:prefetched-clients (active-clients)})))

(comment
  (clawe.config/reload-config)
  (->>
    (active-clients)
    (filter (comp #{"Emacs"} :client/app-name))
    (map client/strip)
    (map #(assoc % :wsp-title (client/client->workspace-title %)))))

;; maybe something more like toggle->away - something more specific to the toggle UX
(defn hide-client
  ([client] (hide-client nil client))
  ([opts client]
   (sys/start! `*wm*)
   ;; TODO document :hide/ types for clients
   (case (:hide/type client :hide/fallback)
     :hide/fallback
     (let [wsp-title (or (client/client->workspace-title client) "fallback")]
       (move-client-to-workspace nil client wsp-title))

     :hide/scratchpad
     ;; TODO impl in yabai and awesomewm
     (wm.protocol/-hide-scratchpad *wm* nil client)

     :hide/os-hide
     (println "to impl!")
     ;; (wm.protocol/-hide-client *wm* opts client)

     :hide/close
     (wm.protocol/-close-client *wm* opts client)

     ;; default to os level close.... pr
     (println "WARN: unsupported :hide/type" opts))))

;; rearranging

(defn swap-workspaces-by-index
  [index-a index-b]
  (sys/start! `*wm*)
  (wm.protocol/-swap-workspaces-by-index *wm* index-a index-b))

;; TODO consider impling a fallback if this isn't impled
(defn drag-workspace
  [dir]
  (sys/start! `*wm*)
  (wm.protocol/-drag-workspace *wm* dir))

(comment
  (drag-workspace :dir/up)
  (drag-workspace :dir/down))

(defn move-client-to-workspace
  ([c wsp-or-title] (move-client-to-workspace nil c wsp-or-title))
  ([opts c wsp-or-title]
   (when (and c wsp-or-title)
     (sys/start! `*wm*)
     (when-let [wsp (cond (map? wsp-or-title)    wsp-or-title
                          (string? wsp-or-title) (key->workspace wsp-or-title))]
       ;; TODO support moving to a workspace for only a string
       ;; (even if there's no workspace def for it)
       (wm.protocol/-move-client-to-workspace *wm* opts c wsp)))))
