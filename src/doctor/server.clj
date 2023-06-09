(ns doctor.server
  (:require
   [clojure.string :as string]
   [taoensso.timbre :as log]
   [taoensso.encore :as enc]
   [systemic.core :as sys :refer [defsys]]
   [plasma.server :as plasma.server]
   [plasma.server.interceptors :as plasma.interceptors]
   [cognitect.transit :as transit]
   [ring.adapter.undertow :as undertow]
   [ring.adapter.undertow.websocket :as undertow.ws]
   [datascript.transit :as dt]

   [dates.transit-time-literals :as ttl]
   [api.db :as api.db]
   [api.topbar :as api.topbar]
   [api.workspaces :as api.workspaces]
   [db.core :as db]
   [db.listeners :as db.listeners]
   [doctor.config :as doctor.config]
   [doctor.api :as doctor.api]
   [garden.watcher :as garden.watcher]
   [ralphie.notify :as notify]
   [hiccup.page :as hiccup]))

(defn log-output-fn
  [data]
  (let [{:keys [level ?err #_vargs _msg_ ?ns-str ?file _hostname_
                _timestamp_ ?line output-opts]}
        data]

    (str
      #_(when-let [ts (force timestamp_)]
          (str ts " "))
      #_ (force hostname_)
      #_ " "
      (string/upper-case (name level))  " "
      "[" (or ?ns-str ?file "?") ":" (or ?line "?") "]: "

      (when-let [msg-fn (get output-opts :msg-fn log/default-output-msg-fn)]
        (msg-fn data))

      (when-let [_err ?err]
        (when-let [ef (get output-opts :error-fn log/default-output-error-fn)]
          (when-not   (get output-opts :no-stacktrace?) ; Back compatibility
            (str enc/system-newline
                 (ef data))))))))

(log/merge-config! {:output-fn log-output-fn})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; transit helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def transit-read-handlers
  (merge transit/default-read-handlers
         ttl/read-handlers
         dt/read-handlers))

(def transit-write-handlers
  (merge transit/default-write-handlers
         ttl/write-handlers
         dt/write-handlers))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Plasma config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsys ^:dynamic *sessions*
  "Plasma sessions"
  (atom {}))

(defsys ^:dynamic *plasma-server*
  "Our plasma server bundle"
  (plasma.server/make-server
    {:session-atom           *sessions*
     :send-fn                #(undertow.ws/send %2 %1)
     :on-error               #(log/warn (:error %) "Error in plasma handler"
                                        (-> % :ctx :request (select-keys
                                                              #{:event-name :fn-var :args})))
     :transit-read-handlers  transit-read-handlers
     :transit-write-handlers transit-write-handlers
     :interceptors           [(plasma.interceptors/auto-require (fn [_] (sys/start!)))
                              (plasma.interceptors/load-metadata)
                              #_{:name :doctor-logging
                                 :enter
                                 (fn [ctx]
                                   (let [{:keys [fn-var args event-name]} (:request ctx)]
                                     (log/debug "\nplasma interceptor"
                                                "fn-var" fn-var "event-name" event-name)
                                     (when (seq args) (log/debug "args" args)))
                                   ctx)}]}))

(defn ->plasma-undertow-ws-handler [_req]
  {:undertow/websocket
   {:on-open
    #(do
       (plasma.server/on-connect! *plasma-server* (:channel %))
       (log/info "client connected" (str "(" (count @*sessions*) " current)"))
       (notify/notify {:notify/subject "Websocket connected"
                       :notify/body    (str "active sessions: " (count @*sessions*))
                       :notify/id      :doctor/sessions}))

    :on-close-message
    #(do
       (plasma.server/on-disconnect! *plasma-server* (:channel %))
       (log/info "client disconnected" (str "(" (count @*sessions*) " current)"))
       (notify/notify {:notify/subject "Websocket disconnected"
                       :notify/body    (str "active sessions: " (count @*sessions*))
                       :notify/id      :doctor/sessions}))
    :on-message #(plasma.server/on-message! *plasma-server*
                                            (:channel %)
                                            (:data %))
    :on-error   #(do
                   (log/debug "Error in plasma-ws" (:error %))
                   (log/debug "on channel" (:channel %)))}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Doctor page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->doctor-page
  [{:keys [uri] :as req}]
  (hiccup/html5
    [:html
     [:head
      ;; TODO local tailwind styles
      ]
     [:body
      [:div
       (str "Welcome to " uri)]
      [:div
       (str "The Doctor is in!")]

      [:pre (str req)]

      [:div
       (str "Websocket connections: " (count @*sessions*))

       [:pre
        (str
          @*sessions*)]]

      [:div
       [:br]
       (str "TODO list some end points?")
       [:br]
       (str "TODO some doctor data?")
       [:br]
       (str "TODO status check?")]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Server
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsys ^:dynamic *server*
  "Doctor webserver"
  :extra-deps
  [api.workspaces/*workspaces-stream*
   api.topbar/*topbar-metadata-stream*
   api.db/*db-stream*
   ;; some of these should maybe be db/*conn* deps, not server deps
   ;; api.db/*tx->fe-db*
   ;; db.listeners/*garden->blog*
   garden.watcher/*garden-watcher*
   ;; this is also disabled in the impl
   db.listeners/*data-expander*
   db/*conn*]
  :start
  (let [port (:server/port doctor.config/*config*)]
    (log/info "Starting *server* on port" port)
    (let [server (undertow/run-undertow
                   (fn [{:keys [uri] :as req}]
                     (cond
                       ;; handle plasma requests
                       (= "/plasma-ws" uri)
                       (->plasma-undertow-ws-handler req)

                       (= "/doctor" uri)
                       {:status  200
                        :headers {"Content-Type" "text/html"}
                        :body    (->doctor-page req)}

                       ;; poor man's router
                       :else (doctor.api/route req)))
                   {:port             port
                    :session-manager? false
                    :websocket?       true})]
      (notify/notify {:notify/subject "Started doctor backend server!"
                      :notify/body    (str "On port: " port)
                      :notify/id      :doctor/server})
      ;; be sure to return the server as the system
      server))
  :stop
  (.stop *server*))


(defn restart []
  (if (sys/running? `*server*)
    (sys/restart! `*server*)
    (sys/start! `*server*)))

(comment
  (restart)
  *server*
  @sys/*registry*
  (sys/stop!)
  (sys/start! `*server*)
  (sys/restart! `*server*))
