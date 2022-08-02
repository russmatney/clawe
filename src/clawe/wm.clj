(ns clawe.wm
  (:require
   [systemic.core :as sys :refer [defsys]]
   [clawe.awesome :as clawe.awesome]
   [clawe.config :as clawe.config]
   [clawe.wm.protocol :as wm.protocol]
   [clawe.yabai :as clawe.yabai])
  (:import
   [clawe.awesome AwesomeWM]
   [clawe.yabai Yabai]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; window manager sys
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsys *wm*
  :start
  (if (clawe.config/is-mac?) (Yabai.) (AwesomeWM.)))

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

(defn all-clients
  ([] (all-clients nil))
  ([opts]
   (sys/start! `*wm*)
   (wm.protocol/-all-clients *wm* opts)))
