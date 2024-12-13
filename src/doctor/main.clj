(ns doctor.main
  (:require
   [systemic.core :as sys :refer [defsys]]
   [taoensso.telemere :as log]
   [nrepl.server :as nrepl]
   ;; [cider.nrepl :as cider]
   [cider.nrepl.middleware :as cider.middleware]
   [refactor-nrepl.middleware]

   [doctor.server :as server]
   [doctor.config :as config])
  (:gen-class))

(defsys ^:dynamic *nrepl*
  :closure
  (let [nrepl-val (atom nil)
        port      (:nrepl/port config/*config*)]
    (log/log! :info ["Starting Doctor backend *nrepl*" {:port port}])
    (try
      (reset! nrepl-val
              (nrepl/start-server
                :port port
                :handler
                (apply nrepl/default-handler
                       (conj cider.middleware/cider-middleware
                             'refactor-nrepl.middleware/wrap-refactor))))
      (catch Exception e
        (println "nrepl startup error" e)
        (log/log! :info [e "nrepl server error"])))
    {:stop
     (fn []
       (println "nrepl startup stopping")
       (log/log! :info "stopping *nrepl*")
       (nrepl/stop-server @nrepl-val))}))

(defn -main
  "Main entrypoint for the Doctor server"
  []
  (try
    (sys/start! `*nrepl*)
    (sys/start! `server/*server*)
    (catch Exception e
      (let [{:keys [cause system]} (ex-data e)]
        (println "server startup error" e)
        (if cause
          (log/log! :info [e "Error during system startup" {:system system}])
          (log/log! :info [e "Error during startup"]))
        ;; wait a little bit before dying so that the error gets flushed
        (Thread/sleep 10000)
        (throw (or cause e))))))
