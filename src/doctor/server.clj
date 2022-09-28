(ns doctor.server
  (:require
   [taoensso.timbre :as log]
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
   [api.todos :as api.todos]
   [api.workspaces :as api.workspaces]
   [db.core :as db]
   [db.listeners :as db.listeners]
   [doctor.config :as doctor.config]
   [doctor.api :as doctor.api]
   [garden.watcher :as garden.watcher]
   [ralphie.notify :as notify]

   [notebooks.server :as notebooks.server]
   [nextjournal.clerk.viewer :as clerk-viewer]
   )
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream])
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clerk read/write handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn eval* [form]
  (println "\n\nCalling eval with form" form)
  (eval form))

(def clerk-read-handlers
  ;; wrapped in an extra vector to work around transit-cljs bug
  {"clerk/ViewerEval"
   (fn [[expr]] (eval* expr))
   "clerk/ViewerFn" (fn [[form]]
                      (clerk-viewer/->viewer-fn form))})

(def clerk-write-handlers
  ;; wrapping this in an extra vector to work around likely transit-cljs bug
  {nextjournal.clerk.viewer.ViewerEval (transit/write-handler "clerk/ViewerEval" #(vector (:form %)))
   nextjournal.clerk.viewer.ViewerFn   (transit/write-handler "clerk/ViewerFn" #(vector (:form %)))})

(def transit-read-handlers
  (merge transit/default-read-handlers
         ttl/read-handlers
         dt/read-handlers
         clerk-read-handlers))

(def transit-write-handlers
  (merge transit/default-write-handlers
         ttl/write-handlers
         dt/write-handlers
         clerk-write-handlers))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Plasma config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsys *sessions*
  "Plasma sessions"
  (atom {}))

(defsys *plasma-server*
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
                                     (log/debug "\nplasma interceptor" "fn-var" fn-var "event-name" event-name)
                                     (when (seq args) (log/debug "args" args)))
                                   ctx)}]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Server
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsys *server*
  "Doctor webserver"
  :extra-deps
  [api.workspaces/*workspaces-stream*
   api.topbar/*topbar-metadata-stream*
   api.todos/*todos-stream*
   api.db/*db-stream*
   ;; some of these should maybe be db/*conn* deps, not server deps
   ;; api.db/*tx->fe-db*
   ;; db.listeners/*garden->blog*
   garden.watcher/*garden-watcher*
   ;; this is also disabled in the impl
   ;; db.listeners/*garden->expo*
   db.listeners/*data-expander*
   db/*conn*
   notebooks.server/*clerk-server*
   notebooks.server/*clerk-workspace-server*
   ]
  :start
  (let [port (:server/port doctor.config/*config*)]
    (log/info "Starting *server* on port" port)
    (let [server
          (undertow/run-undertow
            (fn [{:keys [uri] :as req}]
              (cond
                ;; handle plasma requests
                (= uri "/plasma-ws")
                {:undertow/websocket
                 {:on-open #(do #_(log/info "Client connected")
                                (plasma.server/on-connect! *plasma-server* %))

                  :on-message       #(plasma.server/on-message!
                                       *plasma-server*
                                       (:channel %)
                                       (:data %))
                  :on-close-message #(plasma.server/on-disconnect!
                                       *plasma-server*
                                       (:ws-channel %))}}

                (= uri "/clerk-ws")
                {:undertow/websocket
                 {:on-open    #(log/info "Client connected")
                  :on-message (fn [msg]
                                (log/info "Message" msg)
                                (let [_ch  (:channel msg)
                                      data (:data msg)
                                      deserialized-msg
                                      (-> (transit/reader
                                            (ByteArrayInputStream. (.getBytes data))
                                            :json
                                            {:handlers transit-read-handlers})
                                          (transit/read))]
                                  (log/info "deser msg" deserialized-msg)

                                  ;; TODO send! some response to the client

                                  ))
                  :on-close-message #(log/info "Close message" %)}}

                ;; poor man's router
                :else (doctor.api/route req)

                ))
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
