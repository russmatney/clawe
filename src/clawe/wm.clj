(ns clawe.wm
  (:require
   [systemic.core :as sys :refer [defsys]]
   [ralphie.zsh :as zsh]

   [clawe.awesome :as clawe.awesome]
   [clawe.config :as clawe.config]
   [clawe.wm.protocol :as wm.protocol]
   [clawe.yabai :as clawe.yabai])
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

(defn- merge-with-def
  "Merges the passed workspace with a workspace-def from resouces/clawe.edn.

  Matches using the `:workspace/title`."
  [{:keys [workspace/title] :as wsp}]
  (merge wsp (clawe.config/workspace-def title)))

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
     ;; TODO apply malli transform here instead of in the protocols?
     (map merge-with-def)
     (map ensure-directory))))

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
        (map merge-with-def)
        (map ensure-directory))))

(defn fetch-workspace
  ([workspace-title] (fetch-workspace nil workspace-title))
  ([opts workspace-title]
   (sys/start! `*wm*)
   (->
     (wm.protocol/-fetch-workspace *wm* opts workspace-title)
     merge-with-def
     ensure-directory)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; client fetching
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active-clients
  ([] (active-clients nil))
  ([opts]
   (sys/start! `*wm*)
   (wm.protocol/-active-clients *wm* opts)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; interactions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; crud

(defn ensure-workspace
  ([wsp-title] (ensure-workspace nil wsp-title))
  ([opts wsp-title]
   (sys/start! `*wm*)
   (wm.protocol/-ensure-workspace *wm* opts wsp-title)))
(def create-workspace ensure-workspace)
(comment create-workspace)

(defn delete-workspace
  [workspace]
  (sys/start! `*wm*)
  (wm.protocol/-delete-workspace *wm* workspace))

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

;; rearranging

(defn swap-workspaces-by-index
  [index-a index-b]
  (sys/start! `*wm*)
  (wm.protocol/-swap-workspaces-by-index *wm* index-a index-b))

(defn drag-workspace
  [dir]
  (sys/start! `*wm*)
  (wm.protocol/-drag-workspace *wm* dir))

(defn move-client-to-workspace
  ([c wsp] (move-client-to-workspace nil c wsp))
  ([opts c wsp]
   (sys/start! `*wm*)
   (wm.protocol/-move-client-to-workspace *wm* opts c wsp)))
