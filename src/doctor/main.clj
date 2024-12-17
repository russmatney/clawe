(ns doctor.main
  (:require
   [systemic.core :as sys :refer [defsys]]
   [taoensso.telemere :as log]
   [nrepl.server :as nrepl]
   ;; [cider.nrepl :as cider]
   [cider.nrepl.middleware :as cider.middleware]
   [refactor-nrepl.middleware]

   [doctor.server :as server]
   [doctor.config :as d.config])
  (:gen-class))

(defsys ^:dynamic *nrepl*
  :closure
  (let [nrepl-val (atom nil)
        port      (d.config/nrepl-port)]
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

(defsys ^:dynamic *exit-code-promise*
  "Systemic system to prevent shutdown until somebody delivers an exit code"
  :start (promise) :stop (deliver *exit-code-promise* 0))

(defn -main [& _args]
  (systemic.core/start! `*exit-code-promise*)
  (try
    (sys/start! `*exit-code-promise*
                `*nrepl*
                `server/*server*)
    (.addShutdownHook
      (Runtime/getRuntime)
      (Thread. (fn []
                 (log/log! {:id ::shutdown-hook} "Shutting down systems on system hook")
                 (sys/stop!))))
    (catch Exception e
      (let [{:keys [system]} (ex-data e)]
        (log/error!
          {:id   ::system-start-error
           :msg  "Error during system startup"
           :data {:system system}}
          e)
        (try
          (let [running-systems
                (->> systemic.core/*registry*
                     (deref)
                     (keys)
                     (filter sys/running?)
                     (remove #{`*exit-code-promise*}))]
            (log/log! {:id   ::partial-shutdown
                       :data {:systems running-systems}}
                      "Shutting down partially started systems")
            (apply sys/stop! running-systems))
          (catch Exception e
            (log/error! {:id  ::partial-shutdown-error
                         :msg "Error during partial shutdown"}
                        e))
          (finally
            (Thread/sleep 10000)
            (deliver *exit-code-promise* 1))))))
  (System/exit @*exit-code-promise*))
