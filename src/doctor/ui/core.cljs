(ns doctor.ui.core
  (:require
   [dates.transit-time-literals :as ttl]
   [plasma.client]
   [taoensso.telemere :as log]
   [taoensso.telemere.utils :as log.utils]
   [time-literals.read-write]
   [tick.timezone]
   [tick.locale-en-us]
   [tick.core :as t]

   [uix.core :as uix :refer [defui $]]
   [uix.dom :as uix.dom]

   [reagent.core :as r]
   [reitit.frontend :as rf]
   [reitit.frontend.easy :as rfe]

   [datascript.core :as d]
   [datascript.transit :as dt]

   [pages.core :as pages]
   [pages.db :as pages.db]
   [pages.events :as pages.events]
   [pages.screenshots :as pages.screenshots]
   [pages.wallpapers :as pages.wallpapers]
   [pages.garden :as pages.garden]
   [pages.posts :as pages.posts]
   [pages.journal :as pages.journal]

   [doctor.ui.hooks.use-db :as hooks.use-db]
   [doctor.ui.hooks.use-reaction :refer [use-reaction]]

   [doctor.ui.views.blog :as views.blog]
   [doctor.ui.views.commits :as views.commits]
   [doctor.ui.views.focus :as views.focus]
   [doctor.ui.views.todos :as views.todos]
   [doctor.ui.views.today :as views.today]
   [doctor.ui.views.pomodoro :as views.pomodoro]
   [doctor.ui.views.git-status :as views.git-status]
   [doctor.ui.views.topbar :as views.topbar]
   [doctor.ui.views.chess-games :as views.chess-games]
   [doctor.ui.views.dashboard :as views.dashboard]
   [doctor.ui.views.workspaces :as views.workspaces]
   ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; routes, home
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def route-defs
  [
   {:route "/" :page-name :page/home :label "Home" :comp views.dashboard/widget}
   {:route "/dashboard" :page-name :page/dashboard :label "Dashboard" :comp views.dashboard/widget}
   {:route "/focus" :page-name :page/focus :label "Focus" :comp views.focus/widget :hide-header true}
   {:route "/todos" :page-name :page/todos :label "Todos" :comp views.todos/widget}
   {:route "/blog" :page-name :page/blog :label "Blog" :comp views.blog/widget :hide-header true}
   {:route "/today" :page-name :page/today :label "Today" :comp views.today/widget}
   {:route "/wallpapers" :page-name :page/wallpapers :label "Wallpapers" :comp pages.wallpapers/page}
   {:route "/events" :page-name :page/events :label "Events" :comp pages.events/page}
   {:route "/pomodoros" :page-name :page/pomodoros :label "Pomodoros" :comp views.pomodoro/widget}
   {:route "/git-status" :page-name :page/git-status :label "Git-Status" :comp views.git-status/widget}
   {:route "/commits" :page-name :page/commits :label "Commits" :comp views.commits/widget}
   {:route "/db" :page-name :page/db :label "DB" :comp pages.db/page}
   {:route "/screenshots" :page-name :page/screenshots :label "Screenshots" :comp pages.screenshots/page}
   {:route "/garden" :page-name :page/garden :label "Garden" :comp pages.garden/page}
   {:route "/posts" :page-name :page/posts :label "Posts" :comp pages.posts/page}
   {:route "/workspaces" :page-name :page/workspaces :label "Workspaces" :comp views.workspaces/widget}
   {:route "/journal" :page-name :page/journal :label "Journal" :comp pages.journal/page}
   {:route "/chess-games" :page-name :page/chess :label "Chess Games" :comp views.chess-games/widget}
   {:route "/topbar" :page-name :page/topbar :label "Top Bar" :comp views.topbar/widget :comp-only true}
   {:route "/topbar-bg" :page-name :page/topbar-bg :label "Top Bar BG" :comp views.topbar/widget}
   ]
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; routes

(def routes
  (->> route-defs
       (map (fn [{:keys [route page-name]}] [route {:name page-name}]))
       (into [])))

(def router (rf/router routes {}))

(defonce current-route (r/atom nil))

(defn start-router []
  (rfe/start!
    router
    (fn [match _history] (reset! current-route match))
    {:use-fragment true}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; app-wrapping error boundary

(def error-boundary
  "Useful, but also an example of creating a react component directly."
  (uix.core/create-class
    {:displayName              "error-boundary"
     :getInitialState          (fn [] #js {:error nil})
     :getDerivedStateFromError (fn [error] #js {:error error})
     :componentDidCatch        (fn [error _error-info]
                                 (this-as this
                                   (let [props (.. this -props -argv)]
                                     (when-let [on-error (:on-error props)]
                                       (on-error error)))))
     :render                   (fn []
                                 (this-as this
                                   (if (.. this -state -error)
                                     ($ :div "error")
                                     (.. this -props -children))))}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; plasma details
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(goog-define SERVER_HOST "localhost")
(goog-define SERVER_PORT 3334)

(def plasma-ws-url (str "ws://" SERVER_HOST ":" SERVER_PORT "/plasma-ws"))

(defn on-close [] (log/log! :info "Connection with server closed"))
(defn on-error [] (log/log! :info "Connection with server error"))
(defn on-open [] (log/log! :info "Connection with server open"))
(defn on-reconnect [] (log/log! :info "Reconnected to server"))

(defn start-plasma []
  (plasma.client/use-transport!
    (plasma.client/websocket-transport
      plasma-ws-url
      {:on-open      on-open
       :on-close     on-close
       :on-reconnect on-reconnect
       :on-error     on-error
       :transit-write-handlers
       (merge ttl/write-handlers dt/write-handlers)
       :transit-read-handlers
       (merge ttl/read-handlers dt/read-handlers)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; root view

(defui view [{:keys [conn] :as opts}]
  (let [route-data     (some->> route-defs
                                (filter (comp #{(-> opts :route :data :name)} :page-name)) first)
        {:keys [comp]} route-data

        opts (assoc opts
                    :conn conn
                    :route-defs route-defs
                    :route-data route-data)]
    (if comp
      (if (:comp-only route-data)
        ($ comp opts)
        ($ pages/page (assoc opts :main comp)))
      ($ :div "no page"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Bootstrap
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; app

(defui app []
  ($ error-boundary {:on-error js/console.error}
     (let [route          (use-reaction current-route)
           {:keys [conn]} (hooks.use-db/use-db)]
       ($ view {:route route :conn conn}))))

(defonce root-el
  (uix.dom/create-root
    (js/document.getElementById "app")))

(defn mount-root []
  (start-router)
  (uix.dom/render-root ($ app) root-el))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn init
  []
  (enable-console-print!)
  (log/set-min-level! :debug)
  (log/remove-handler! :default/console)
  (log/add-handler!
    :custom-console
    (log/handler:console
      {:output-fn
       (log/format-signal-fn
         {:preamble-fn
          (log.utils/signal-preamble-fn
            {:format-inst-fn
             (fn [inst]
               (->> inst
                    (t/zoned-date-time)
                    (t/format "h:MM:ss")))})})}))
  (time-literals.read-write/print-time-literals-cljs!)
  (start-plasma)
  (mount-root))

(comment
  (log/get-handlers)
  (log/remove-handler! :custom-console)
  (log/log! {:level :info
             :data  {:hello :world}}
            "hello world")
  (t/format "h:mm:ss" (t/zoned-date-time
                        #inst "2024-12-13T17:29:44.179-00:00")))
