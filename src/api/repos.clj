(ns api.repos
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]))

(defn active-repos []
  [])

(defsys *repos-stream*
  :start (s/stream)
  :stop (s/close! *repos-stream*))

(defn push-repo-update []
  (s/put! *repos-stream* (active-repos)))
