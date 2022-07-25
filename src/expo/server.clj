(ns expo.server
  (:require
   [cognitect.transit :as transit]
   [dates.transit-time-literals :as ttl]
   [plasma.server :as plasma.server]
   [plasma.server.interceptors :as plasma.interceptors]
   [ring.adapter.undertow :as undertow]
   [ring.adapter.undertow.websocket :as undertow.ws]
   [systemic.core :as sys :refer [defsys]]
   [taoensso.timbre :as log]

   [expo.config :as config]
   [hooks.count :as count]
   [garden.core :as garden]))

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
     :transit-read-handlers  (merge transit/default-read-handlers ttl/read-handlers)
     :transit-write-handlers (merge transit/default-write-handlers ttl/write-handlers)
     :interceptors           [(plasma.interceptors/auto-require)
                              (plasma.interceptors/load-metadata)]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Server
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsys *server*
  "Expo webserver"
  :extra-deps [count/*counts-stream*
               garden/*garden-stream*]
  :start
  (let [port (:server/port config/*config*)]
    (log/info "Starting *server* on port" port)
    (undertow/run-undertow
      (fn [{:keys [uri] :as _req}]
        (log/info "request" uri (System/currentTimeMillis))
        ;; poor man's router
        (cond
          (= uri "/ws")
          {:undertow/websocket
           {:on-open    #(do (log/info "Client connected")
                             (plasma.server/on-connect! *plasma-server* %))
            :on-message #(plasma.server/on-message!
                           *plasma-server*
                           (:channel %)
                           (:data %))
            :on-close   #(plasma.server/on-disconnect!
                           *plasma-server*
                           (:ws-channel %))}}))
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
  (println "hi"))
