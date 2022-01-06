(ns doctor.api.workspaces
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [clawe.workspaces :as clawe.workspaces]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Active workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active-workspaces []
  (->>
    (clawe.workspaces/all-workspaces)
    (filter :awesome.tag/name)
    (map clawe.workspaces/apply-git-status)
    (map #(dissoc % :rules/apply :rules/is-my-client?))))

(defsys *workspaces-stream*
  :start (s/stream)
  :stop (s/close! *workspaces-stream*))

(comment
  (sys/start! `*workspaces-stream*))

(defn update-workspaces []
  (println "pushing to workspaces stream (updating topbar)!")
  (s/put! *workspaces-stream* (active-workspaces)))

(comment
  (->>
    (active-workspaces)
    (sort-by :awesome.tag/index)
    first)

  (update-workspaces))
