(ns doctor.server
  (:require
   [taoensso.timbre :as log]
   [systemic.core :as sys :refer [defsys]]
   [plasma.server :as plasma.server]
   [plasma.server.interceptors :as plasma.interceptors]
   [cognitect.transit :as transit]
   [ring.adapter.undertow :as undertow]
   [ring.adapter.undertow.websocket :as undertow.ws]

   [doctor.config :as config]
   [doctor.time-literals-transit :as tlt]
   [doctor.api.core :as api]
   [doctor.api.workspaces :as d.workspaces]
   [doctor.api.topbar :as d.topbar]
   [doctor.api.todos :as d.todos]
   [doctor.ui.views.screenshots :as screenshots]
   [doctor.ui.views.wallpapers :as wallpapers]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Plasma config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsys *sessions*
  "Plasma sessions"
  (atom {}))

(defsys *plasma-server*
  "Our plasma server bundle"
  (plasma.server/make-server
    {:session-atom *sessions*
     :send-fn      #(undertow.ws/send %2 %1)
     :on-error     #(log/warn (:error %) "Error in plasma handler" {:request %})
     :transit-read-handlers
     (merge transit/default-read-handlers
            tlt/read-handlers)
     :interceptors [(plasma.interceptors/auto-require
                      #(do (log/info "Auto requiring namespace" {:namespace %})
                           (systemic.core/start!)))
                    (plasma.interceptors/load-metadata)]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Server
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsys *server*
  "Doctor webserver"
  :extra-deps
  [d.workspaces/*workspaces-stream*
   d.topbar/*topbar-metadata-stream*
   d.todos/*todos-stream*
   screenshots/*screenshots-stream*
   wallpapers/*wallpapers-stream*]
  :start
  (let [port (:server/port config/*config*)]
    (log/info "Starting *server* on port" port)
    (undertow/run-undertow
      (fn [{:keys [uri] :as req}]
        (log/info "request" uri (System/currentTimeMillis))
        (cond
          ;; handle plasma requests
          (= uri "/ws")
          {:undertow/websocket
           {:on-open #(do (log/info "Client connected")
                          (plasma.server/on-connect! *plasma-server* %))

            :on-message #(plasma.server/on-message!
                           *plasma-server*
                           (:channel %)
                           (:data %))
            :on-close   #(plasma.server/on-disconnect!
                           *plasma-server*
                           (:ws-channel %))}}

          ;; poor man's router
          :else (api/route req)))
      {:port             port
       :session-manager? false
       :websocket?       true}))
  :stop
  (.stop *server*))

(comment
  @sys/*registry*
  (sys/stop!)
  (sys/start! `*server*)
  (sys/restart! `*server*)
  *server*

  (slurp "http://localhost:3334/dock/update")
  (slurp "http://localhost:3334/dock/update")
  (println "hi"))
