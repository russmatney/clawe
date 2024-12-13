(ns doctor.server
  (:require
   [cognitect.transit :as transit]
   [clojure.string :as string]
   [datascript.transit :as dt]
   [hiccup.page :as hiccup]
   [muuntaja.core :as muu]
   [muuntaja.format.form :as muu.form]
   [muuntaja.middleware :as muu.middleware]
   [plasma.server :as plasma.server]
   [plasma.server.interceptors :as plasma.interceptors]
   [ring.adapter.undertow :as undertow]
   [ring.adapter.undertow.websocket :as undertow.ws]
   [systemic.core :as sys :refer [defsys]]
   [taoensso.telemere :as log]
   [taoensso.telemere.utils :as log.utils]

   [api.db :as api.db]
   [api.topbar :as api.topbar]
   [api.workspaces :as api.workspaces]
   [dates.transit-time-literals :as ttl]
   [db.core :as db]
   [db.listeners :as db.listeners]
   [doctor.config :as doctor.config]
   [doctor.api :as doctor.api]
   [garden.watcher :as garden.watcher]
   [ralphie.notify :as notify]
   [wallpapers.core :as wallpapers]))

(log/set-min-level! :debug)
(log/remove-handler! :default/console)

;; shorter prefix console handler
(log/add-handler!
  :console
  (log/handler:console
    {:output-fn
     (log/format-signal-fn
       {:preamble-fn
        (fn [signal]
          (->
            ((log.utils/signal-preamble-fn
               {:format-inst-fn
                (log.utils/format-inst-fn
                  {:formatter (java.time.format.DateTimeFormatter/ofPattern "dd HH:mm:ss")})})
             signal)
            (string/replace ".nyc.rr.com" "")))})})
  {:needs-stopping? true})

;; file log handler
(log/add-handler!
  :durable
  (log/handler:file
    {:output-fn
     (log/format-signal-fn
       {:preamble-fn
        (log.utils/signal-preamble-fn
          {:format-inst-fn
           (log.utils/format-inst-fn
             {:formatter (java.time.format.DateTimeFormatter/ofPattern "dd HH:mm:ss")})})})
     :max-file-size (* 1024 1024 4)})
  {:needs-stopping? true})

(comment
  (keys (log/get-handlers))
  (log/remove-handler! :console)
  (log/log! :info "hi"))


(def m (muu/create
         (-> muu/default-options
             (assoc-in [:formats "application/x-www-form-urlencoded"]
                       muu.form/format))))

(comment
  (-> m (.options) :formats keys))

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

(defn server-status-notif [opts]
  (notify/notify
    (merge {:notify/subject "Server status notif"
            :notify/body    (str "active sessions: " (count @*sessions*))
            :notify/id      :doctor/server-status}
           opts)))

(defsys ^:dynamic *plasma-server*
  "Our plasma server bundle"
  (plasma.server/make-server
    {:session-atom           *sessions*
     :send-fn                #(undertow.ws/send %2 %1)
     :on-error               #(log/log! :warn [(:error %) "Error in plasma handler"
                                               (-> % :ctx :request (select-keys
                                                                     #{:event-name :fn-var :args}))])
     :transit-read-handlers  transit-read-handlers
     :transit-write-handlers transit-write-handlers
     :interceptors           [(plasma.interceptors/auto-require (fn [_] (sys/start!)))
                              (plasma.interceptors/load-metadata)]}))

(defn ->plasma-undertow-ws-handler [_req]
  {:undertow/websocket
   {:on-open
    #(do
       (plasma.server/on-connect! *plasma-server* (:channel %))
       (log/log! :info ["client connected" (str "(" (count @*sessions*) " current)")])
       (server-status-notif {:notify/subject "Websocket connected"}))
    :on-close-message
    #(do
       (plasma.server/on-disconnect! *plasma-server* (:channel %))
       (log/log! :info ["client disconnected" (str "(" (count @*sessions*) " current)")])
       (server-status-notif {:notify/subject "Websocket connected"}))
    :on-message #(plasma.server/on-message! *plasma-server*
                                            (:channel %)
                                            (:data %))
    :on-error   #(do
                   (log/log! :debug ["Error in plasma-ws" (:error %)])
                   (log/log! :debug ["on channel" (:channel %)]))}})


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

(defn router
  [{:keys [uri] :as req}]
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

(def app
  "Middlewares run bottom to top!"
  (-> router
      (muu.middleware/wrap-format m)))

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
   db/*conn*
   *plasma-server*]
  :start
  (wallpapers/ensure-wallpaper)
  (let [port (:server/port doctor.config/*config*)]
    (log/log! :info ["Starting *server* on port" port])
    (let [server (undertow/run-undertow
                   app {:port             port
                        :session-manager? false
                        :websocket?       true})]
      (server-status-notif {:notify/subject (str "Server started on port: " port)
                            :notify/id      "server-up"})
      ;; be sure to return the server as the system
      server))
  :stop
  (println "stopping *server*")
  (log/log! :info "*server* stopping")
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
