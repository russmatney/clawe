(ns clawe.wm
  (:require
   [systemic.core :as sys :refer [defsys]]
   [ralphie.zsh :as zsh]

   [clawe.awesome :as clawe.awesome]
   [clawe.config :as clawe.config]
   [clawe.wm.protocol :as wm.protocol]
   [clawe.yabai :as clawe.yabai]
   [clawe.client :as client]
   [clojure.string :as string])
  (:import
   [clawe.awesome Awesome]
   [clawe.yabai Yabai]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; window manager sys
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsys *wm*
  :extra-deps
  [clawe.config/*config*]
  :start
  (if (clawe.config/is-mac?) (Yabai.) (Awesome.)))

(defn reload-wm []
  (when (sys/running? `*wm*)
    (sys/restart! `*wm*)))

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
  (when-let [client (cond (map? client)    client
                          (string? client) (clawe.config/client-def client))]
    ;; TODO refactor into protocol (perf)
    (some->>
      (active-clients)
      (filter (partial client/match? client))
      first)))

(comment
  (fetch-client "journal")
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

(comment
  (close-client nil))

;; focus

(defn focus-workspace
  ([workspace] (focus-workspace nil workspace))
  ([opts workspace]
   (sys/start! `*wm*)
   (wm.protocol/-focus-workspace *wm* opts workspace)))

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
     (wm.protocol/-focus-client *wm* opts client))))

(defn bury-client
  ([client] (bury-client nil client))
  ([opts client] (wm.protocol/-bury-client *wm* opts client)))

(defn bury-clients
  [clients]
  (doseq [cli clients] (bury-client cli)))

(defn bury-all-clients
  ([] (bury-all-clients nil))
  ([opts] (wm.protocol/-bury-all-clients *wm* opts)))

(comment bury-all-clients)

(declare move-client-to-workspace)
(defn show-client
  "Opts `:current-workspace`, `:focus/float-and-center`, `focus-client` opts"
  ([client] (show-client nil client))
  ([opts client]
   (let [client (cond (map? client)    client
                      (string? client) (fetch-client client))]
     ;; TODO would like to focus+center first to avoid the jank on linux
     ;; but on osx, do it the other way to avoid the auto-space-switch.
     ;; suppose we need to float and center, then switch workspaces, then focus
     (move-client-to-workspace
       client (or (:current-workspace opts) (current-workspace)))

     ;; TODO bring client to top instead
     (try
       (bury-clients (:clients (or (:current-workspace opts) (current-workspace))))
       (catch Exception e
         (println "[WARN]: bury-clients not impled (or some other error)")
         (println e)
         (println "[WARN]: bury-clients not impled (or some other error)")))

     (focus-client (merge {:float-and-center (:focus/float-and-center client true)} opts)
                   client))))

(defn client->workspace-title [client]
  (or (:client/workspace-title client)
      (let [wt (:client/window-title client)]
        (cond
          (re-seq #" " wt)
          ;; term before first space
          (->> (string/split wt #" ") first)

          :else wt))))

(comment
  (clawe.config/reload-config)
  (->>
    (active-clients)
    (filter (comp #{"Emacs"} :client/app-name))
    (map client/strip)
    (map #(assoc % :wsp-title (client->workspace-title %)))
    )
  )

;; maybe something more like toggle->away - something more specific to the toggle UX
(defn hide-client
  ([client] (hide-client nil client))
  ([opts client]
   (sys/start! `*wm*)
   ;; TODO document :hide/ types for clients
   (case (:hide/type client :hide/scratchpad)
     :hide/scratchpad
     (wm.protocol/-move-client-to-workspace
       *wm* nil client
       (client->workspace-title client))

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
