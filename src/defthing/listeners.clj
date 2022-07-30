(ns defthing.listeners
  (:require
   [taoensso.timbre :as log]
   [systemic.core :as sys :refer [defsys]]
   [datalevin.core :as datalevin]

   [defthing.db :as db]
   [expo.core :as expo]))
;; NOTE babashka (clawe, ralphie) cannot require this namespace,
;; because the datalevin pod does not support `listen!`
;; (which makes sense, bb runs are short-lived, so this is definitely server-only)

(defsys *garden->expo*
  :start (do
           (log/info "Starting :garden->expo db listener")
           (sys/start! `db/*db-conn*)
           (datalevin/listen!
             db/*db-conn* :garden->expo
             (fn [tx]
               ;; NOTE don't you dare try to get a :datoms-transacted off of this tx!
               (try
                 (log/info "garden note transacted!" tx)
                 (expo/update-posts)
                 (catch Exception e
                   (log/warn "Error in garden->expo db listener" e)
                   tx)))))
  :stop
  (try
    (log/info "Removing :garden->expo db listener")
    (datalevin/unlisten! db/*db-conn* :garden->expo)
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

  (datalevin/db db/*db-conn*)

  )
