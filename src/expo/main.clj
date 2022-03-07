(ns expo.main
  (:require
   [systemic.core :as sys :refer [defsys]]
   [taoensso.timbre :as log]
   [nrepl.server :as nrepl]
   [cider.nrepl :as cider]
   [refactor-nrepl.middleware :as refactor-mw]
   [expo.server :as server]
   [expo.config :as config])
  (:gen-class))

(defsys *nrepl*
  :closure
  (let [nrepl-val (atom nil)
        port      (:nrepl/port config/*config*)]
    (log/info config/*config*)
    (log/info "Starting Expo backend nREPL" {:port port})
    (try
      (reset! nrepl-val (nrepl/start-server
                          :port port
                          :handler
                          (refactor-mw/wrap-refactor
                            cider/cider-nrepl-handler)))
      (catch Exception e
        (log/error e "expo B.E. nrepl server error")))
    {:stop
     (fn []
       (nrepl/stop-server @nrepl-val))}))

(defn -main
  "Main entrypoint for the server"
  []
  (try
    (sys/start! `*nrepl*)
    (sys/start! `server/*server*)
    (catch Exception e
      (let [{:keys [cause system]} (ex-data e)]
        (if cause
          (log/error e "Error during system startup" {:system system})
          (log/error e "Error during startup"))
        ;; wait a little bit before dying so that the error gets flushed
        (Thread/sleep 10000)
        (throw (or cause e))))))