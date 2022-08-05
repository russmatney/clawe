(ns clawe.wm
  (:require
   [systemic.core :as sys :refer [defsys]]
   [ralphie.zsh :as zsh]

   [clawe.awesome :as clawe.awesome]
   [clawe.config :as clawe.config]
   [clawe.wm.protocol :as wm.protocol]
   [clawe.yabai :as clawe.yabai]
   [clawe.client :as client])
  (:import
   [clawe.awesome Awesome]
   [clawe.yabai Yabai]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; window manager sys
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsys *wm*
  :start
  (if (clawe.config/is-mac?) (Yabai.) (Awesome.)))

(defn reload-wm []
  (sys/restart! `*wm*))

(comment
  (reload-wm)
  (sys/start! `*wm*))

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

    ;; TODO consider checking/converting mismatched /Users/<user> vs /home/<user> paths

    :else wsp))

(defn- merge-with-workspace-def
  "Merges the passed workspace with a workspace-def from resouces/clawe.edn.

  Matches using the `:workspace/title`."
  [{:keys [workspace/title] :as wsp}]
  (merge wsp (clawe.config/workspace-def title)))

(defn- merge-with-client-def
  "Attempts to merge the passed client with a matching client-def from resouces/clawe.edn.

  Matches using `client/match?`."
  [client]
  (let [defs (clawe.config/client-defs)
        matches
        (->> defs (filter
                    (fn [def]
                      ;; does this imply that clients should carry :match opts?
                      (client/match?
                        def ;; client-def can supply :match options
                        client def))))]
    (if (> (count matches) 1)
      (do
        (println "WARN: multiple matching defs found for client" (client/strip client) matches)
        (first matches))
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

(defn current-workspaces
  ([] (current-workspaces nil))
  ([opts]
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
   (sys/start! `*wm*)
   (some->> (current-workspaces opts) first)))

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
   (sys/start! `*wm*)
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
  ;; TODO refactor into protocol (perf)
  (some->>
    (active-clients)
    (filter (partial client/match? client))
    first))

(comment
  (focused-client)
  (active-clients))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; interactions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; crud

(defn create-workspace
  ([wsp-title] (create-workspace nil wsp-title))
  ([opts wsp-title]
   (sys/start! `*wm*)
   (wm.protocol/-create-workspace *wm* opts wsp-title)))

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

;; focus

(defn focus-workspace
  ([workspace] (focus-workspace nil workspace))
  ([opts workspace]
   (sys/start! `*wm*)
   (wm.protocol/-focus-workspace *wm* opts workspace)))

(defn focus-client
  ([client] (focus-client nil client))
  ([opts client]
   (sys/start! `*wm*)
   (wm.protocol/-focus-client *wm* opts client)))

;; maybe something more like toggle->away - something more specific to the toggle UX
(defn hide-client
  ([client] (hide-client nil client))
  ([opts client]
   (sys/start! `*wm*)
   ;; TODO document :hide/ types for clients
   (case (:hide/type client :hide/scratchpad)
     :hide/scratchpad
     (wm.protocol/-move-client-to-workspace
       *wm* {:ensure-workspace true} client
       (:client/workspace-title client (:client/window-title client)))

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
  ([c wsp] (move-client-to-workspace nil c wsp))
  ([opts c wsp]
   (sys/start! `*wm*)
   (wm.protocol/-move-client-to-workspace *wm* opts c wsp)))
