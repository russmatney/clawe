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
   #_[nextjournal.clerk.viewer :as clerk-viewer]

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
   [clojure.edn :as edn]
   [hiccup.page :as hiccup]
   #_[notebooks.clerk :as notebooks.clerk]
   #_[notebooks.core :as notebooks]))

(defn output-fn
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

(log/merge-config! {:output-fn output-fn})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; transit helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defn eval* [form]
;;   (println "\n\nCalling eval with form" form)
;;   (eval form))

;; (def clerk-read-handlers
;;   ;; wrapped in an extra vector to work around transit-cljs bug
;;   {"clerk/ViewerEval"
;;    (fn [[expr]] (eval* expr))
;;    "clerk/ViewerFn" (fn [[form]]
;;                       (clerk-viewer/->viewer-fn form))})

;; (def clerk-write-handlers
;;   ;; wrapping this in an extra vector to work around likely transit-cljs bug
;;   {nextjournal.clerk.viewer.ViewerEval (transit/write-handler "clerk/ViewerEval" #(vector (:form %)))
;;    nextjournal.clerk.viewer.ViewerFn   (transit/write-handler "clerk/ViewerFn" #(vector (:form %)))})

(def transit-read-handlers
  (merge transit/default-read-handlers
         ttl/read-handlers
         dt/read-handlers
         ;; clerk-read-handlers
         ))

(def transit-write-handlers
  (merge transit/default-write-handlers
         ttl/write-handlers
         dt/write-handlers
         ;; clerk-write-handlers
         ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clerk helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce !clients (atom #{}))
(comment (reset! !clients #{}))

(defn broadcast!
  "Sends an updated eval of the passed `notebook-sym` to _all_ clients."
  [notebook-sym]
  (println "broadcasting notebook-sym" notebook-sym)
  (when-let [_doc nil #_ (notebooks.clerk/ns-sym->viewer notebook-sym)]
    (doseq [_ch @!clients]
      #_(undertow.ws/send (clerk-viewer/->edn {:doc doc}) ch))))

(comment (broadcast! 'notebooks.core))

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
    (let [server
          (undertow/run-undertow
            (fn [{:keys [uri] :as req}]
              (cond
                ;; handle plasma requests
                (= uri "/plasma-ws")
                {:undertow/websocket
                 {:on-open          #(plasma.server/on-connect! *plasma-server* %)
                  :on-message       #(plasma.server/on-message! *plasma-server*
                                                                (:channel %)
                                                                (:data %))
                  :on-close-message #(plasma.server/on-disconnect! *plasma-server*
                                                                   (:ws-channel %))}}

                (:websocket? req)
                {:undertow/websocket
                 {:on-open
                  (fn [msg]
                    (swap! !clients conj (:channel msg))
                    #_(notebooks.clerk/channel-visiting-notebook msg))

                  :on-close-message
                  (fn [msg]
                    (swap! !clients disj (:channel msg))
                    #_(notebooks.clerk/channel-left-notebook msg))

                  :on-message
                  (fn [msg]
                    (let [data (:data msg)]
                      (cond
                        (string/starts-with? data "{:path ")
                        (let [_path (-> data edn/read-string :path)]
                          #_(notebooks.clerk/channel-visiting-notebook
                              (assoc msg :path path)))

                        :else
                        (do
                          (println "evaling: " data)
                          ;; TODO we'll want to make sure this ns is loaded
                          ;; note that the form from the FE can't be aliased
                          (eval (read-string data))))))}}

                (string/starts-with? uri "/notebooks/")
                (let [notebook-sym nil #_ (notebooks.clerk/path->notebook-sym uri)]
                  (when notebook-sym
                    (log/info "loading notebook" notebook-sym)
                    (let [{:keys [notebook error]}
                          (try
                            {:notebook nil
                             #_        (notebooks.clerk/ns-sym->html notebook-sym)}
                            (catch Exception e
                              (println "[CLERK] notebook build fail" notebook-sym)
                              (println e)
                              {:error e}))]
                      (if notebook
                        {:status  200
                         :headers {"Content-Type" "text/html"}
                         :body    notebook}
                        {:status  200
                         :headers {"Content-Type" "text/html"}
                         :body
                         (hiccup/html5
                           [:html
                            [:head]
                            [:body
                             [:div
                              (str "No notebook or failed to load nb at uri: " uri)

                              [:pre error]

                              #_(->>
                                  (notebooks/notebooks)
                                  (map #(assoc % :ns (notebooks.clerk/path->notebook-sym (:uri %))))
                                  (map (fn [{:keys [name uri]}]
                                         [:li
                                          [:a {:href uri}
                                           (str name)]]))
                                  (into [:ul]))]]])}))))

                (= "/" uri)
                (let [body
                      (hiccup/html5
                        [:html
                         [:head]
                         [:body
                          [:div
                           #_(->>
                               (notebooks/notebooks)
                               (map #(assoc % :ns (notebooks.clerk/path->notebook-sym (:uri %))))
                               (map (fn [{:keys [name uri]}]
                                      [:li
                                       [:a {:href uri}
                                        (str name)]]))
                               (into [:ul]))]]])]
                  {:status  200
                   :headers {"Content-Type" "text/html"}
                   :body    body})

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
  (-> "/notebooks/clawe" (string/replace-first "/" "") (string/replace-first "/" ".") symbol)

  (restart)
  *server*
  @sys/*registry*
  (sys/stop!)
  (sys/start! `*server*)
  (sys/restart! `*server*))
