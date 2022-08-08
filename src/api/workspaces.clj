(ns api.workspaces
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [malli.transform :as mt]
   [malli.core :as m]
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
  )

(defsys *workspaces-stream*
  :start (s/stream)
  :stop (s/close! *workspaces-stream*))

(comment
  (sys/start! `*workspaces-stream*))

(defn update-workspaces []
  (println "pushing to workspaces stream (updating topbar)!")
  (s/put! *workspaces-stream* (active-workspaces)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(comment
  (def -w
    (->> (wm/active-workspaces)
         first
         ))

  (->>
    (active-workspaces)
    (sort-by :awesome.tag/index)
    first)

  (update-workspaces)

  )
