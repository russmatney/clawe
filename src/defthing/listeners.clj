(ns defthing.listeners
  (:require
   [taoensso.timbre :as log]
   [systemic.core :as sys :refer [defsys]]
   [datascript.core :as d]

   [defthing.db :as db]
   [expo.core :as expo]))

(defsys *garden->expo*
  :start (do
           (log/info "Adding :garden->expo db listener")
           (sys/start! `db/*conn*)
           (d/listen!
             db/*conn* :garden->expo
             (fn [tx]
               (def tx tx)
               ;; NOTE don't you dare try to get a :datoms-transacted off of this tx!
               (try
                 (log/info "garden note transacted!")
                 (expo/update-posts)
                 (catch Exception e
                   (log/warn "Error in garden->expo db listener" e)
                   tx)))))
  :stop
  (try
    (log/debug "Removing :garden->expo db listener")
    (d/unlisten! db/*conn* :garden->expo)
    (catch Exception e
      (log/debug "err removing listener" e)
      nil)))

(defn start-garden->expo-listener []
  (sys/start! `*garden->expo*))

(defn stop-garden->expo-listener []
  (sys/stop! `*garden->expo*))

(comment
  (sys/start! `*garden->expo*)
  (sys/stop! `*garden->expo*)

  (d/db db/*conn*)

  )
