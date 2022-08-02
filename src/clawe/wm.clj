(ns clawe.wm
  (:require
   [systemic.core :as sys :refer [defsys]]
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
;; public api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current-workspaces
  ([] (current-workspaces nil))
  ([opts]
   (sys/start! `*wm*)
   (wm.protocol/-current-workspaces *wm* opts)))

(defn active-workspaces
  ([] (active-workspaces nil))
  ([opts]
   (sys/start! `*wm*)
   (wm.protocol/-active-workspaces *wm* opts)))

(defn ensure-workspace
  ([wsp-title] (ensure-workspace nil wsp-title))
  ([opts wsp-title]
   (sys/start! `*wm*)
   (wm.protocol/-ensure-workspace *wm* opts wsp-title)))

(defn focus-workspace
  ([workspace] (focus-workspace nil workspace))
  ([opts workspace]
   (sys/start! `*wm*)
   (wm.protocol/-focus-workspace *wm* opts workspace)))

(defn all-clients
  ([] (all-clients nil))
  ([opts]
   (sys/start! `*wm*)
   (wm.protocol/-all-clients *wm* opts)))

(defn focus-client
  ([client] (focus-client nil client))
  ([opts client]
   (sys/start! `*wm*)
   (wm.protocol/-focus-client *wm* opts client)))

(defn close-client
  ([client] (close-client nil client))
  ([opts client]
   (sys/start! `*wm*)
   (wm.protocol/-close-client *wm* opts client)))

(defn move-client-to-workspace
  ([c wsp] (move-client-to-workspace nil c wsp))
  ([opts c wsp]
   (sys/start! `*wm*)
   (wm.protocol/-move-client-to-workspace *wm* opts c wsp)))
