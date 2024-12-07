(ns api.workspaces
  (:require
   [taoensso.telemere :as t]
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [clawe.wm :as wm]
   [util :as util]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Active workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active-workspaces []
  (->>
    (wm/active-workspaces {:include-clients true})
    (map util/drop-complex-types)))

(comment
  (active-workspaces)
  (->> (active-workspaces) (filter :workspace/focused)))

(defsys ^:dynamic *workspaces-stream*
  :start (s/stream 1000)
  :stop (s/close! *workspaces-stream*))

(comment
  (sys/start! `*workspaces-stream*))

(defn push-updated-workspaces []
  (t/log! :info "pushing to workspaces stream (updating topbar)!")
  (s/put! *workspaces-stream* (active-workspaces)))

(comment
  (push-updated-workspaces)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
