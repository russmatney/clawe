(ns doctor.server
  (:require
   [taoensso.timbre :as log]
   [systemic.core :as sys :refer [defsys]]
   [plasma.server :as plasma.server]
   [plasma.server.interceptors :as plasma.interceptors]
   [cognitect.transit :as transit]
   [ring.adapter.undertow :as undertow]
   [ring.adapter.undertow.websocket :as undertow.ws]

   [api.commits :as api.commits]
   [api.events :as api.events]
   [api.repos :as api.repos]
   [api.screenshots :as api.screenshots]
   [api.topbar :as api.topbar]
   [api.todos :as api.todos]
   [api.wallpapers :as api.wallpapers]
   [api.workspaces :as api.workspaces]
   [dates.transit-time-literals :as ttl]
   [defthing.listeners :as defthing.listeners]
   [doctor.config :as doctor.config]
   [doctor.api :as doctor.api]
   [garden.core :as garden]
   [garden.watcher :as garden.watcher]
   [ralphie.notify :as notify]))

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
     :on-error     #(log/warn (:error %) "Error in plasma handler"
                              (-> % :ctx :request (select-keys
                                                    #{:event-name :fn-var :args})))
     :transit-read-handlers
     (merge transit/default-read-handlers ttl/read-handlers)
     :transit-write-handlers
     (merge transit/default-write-handlers ttl/write-handlers)
     :interceptors [(plasma.interceptors/auto-require (fn [_] (sys/start!)))
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
   api.screenshots/*screenshots-stream*
   api.events/*events-stream*
   api.commits/*commits-stream*
   api.repos/*repos-stream*
   api.wallpapers/*wallpapers-stream*
   garden/*garden-stream*
   garden/*journals-stream*
   garden.watcher/*garden-watcher*]
  :start
  (let [port (:server/port doctor.config/*config*)]
    (log/info "Starting *server* on port" port)
    (defthing.listeners/start-garden->expo-listener)
    (let [server
          (undertow/run-undertow
            (fn [{:keys [uri] :as req}]
              (cond
                ;; handle plasma requests
                (= uri "/ws")
                {:undertow/websocket
                 {:on-open #(do #_(log/info "Client connected")
                                (plasma.server/on-connect! *plasma-server* %))

                  :on-message #(plasma.server/on-message!
                                 *plasma-server*
                                 (:channel %)
                                 (:data %))
                  :on-close   #(plasma.server/on-disconnect!
                                 *plasma-server*
                                 (:ws-channel %))}}

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
  (defthing.listeners/stop-garden->expo-listener)
  (.stop *server*))


(comment
  @sys/*registry*
  (sys/stop!)
  (sys/start! `*server*)
  (sys/restart! `*server*)
  *server*)
