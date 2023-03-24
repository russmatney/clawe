(ns doctor.ui.core
  (:require
   [dates.transit-time-literals :as ttl]
   [plasma.client]
   [taoensso.timbre :as log]
   [time-literals.read-write]
   [tick.timezone]
   [tick.locale-en-us]
   [uix.core.alpha :as uix]
   [uix.dom.alpha :as uix.dom]
   [wing.core :as w]
   [wing.uix.router :as router]
   [datascript.transit :as dt]

   [pages.core :as pages]
   [pages.todos :as pages.todos]
   [pages.db :as pages.db]
   [pages.commits :as pages.commits]
   [pages.events :as pages.events]
   [pages.screenshots :as pages.screenshots]
   [pages.wallpapers :as pages.wallpapers]
   [pages.workspaces :as pages.workspaces]
   [pages.counter :as pages.counter]
   [pages.counts :as pages.counts]
   [pages.garden :as pages.garden]
   [pages.posts :as pages.posts]
   [pages.journal :as pages.journal]
   [hooks.db :as hooks.db]

   [doctor.ui.views.focus :as views.focus]
   [doctor.ui.views.topbar :as views.topbar]
   [doctor.ui.views.dashboard :as views.dashboard]))

(comment
  :hello)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; routes, home
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def route-defs
  [{:route "/" :page-name :page/home :label "Dashboard" :comp views.dashboard/widget}
   {:route "/db" :page-name :page/db :label "DB" :comp pages.db/page}
   {:route "/todo" :page-name :page/todos :label "Todos" :comp pages.todos/page}
   {:route "/commits" :page-name :page/commits :label "Commits" :comp pages.commits/page}
   {:route "/events" :page-name :page/events :label "Events" :comp pages.events/page}
   {:route "/topbar" :page-name :page/topbar :label "Top Bar" :comp views.topbar/widget :comp-only true}
   ;; {:route "/topbarbg":page-name :page/topbar-bg :label "Top Bar BG" :comp views.topbar/widget}
   {:route "/counter" :page-name :page/counter :label "Counter" :comp pages.counter/page}
   {:route "/counts" :page-name :page/counts :label "Counts" :comp pages.counts/page}
   {:route "/screenshots" :page-name :page/screenshots :label "Screenshots" :comp pages.screenshots/page}
   {:route "/workspaces" :page-name :page/workspaces :label "Workspaces" :comp pages.workspaces/page}
   {:route "/wallpapers" :page-name :page/wallpapers :label "Wallpapers" :comp pages.wallpapers/page}
   {:route "/garden" :page-name :page/garden :label "Garden" :comp pages.garden/page}
   {:route "/posts" :page-name :page/posts :label "Posts" :comp pages.posts/page}
   {:route "/journal" :page-name :page/journal :label "Journal" :comp pages.journal/page}
   {:route "/focus" :page-name :page/focus :label "Focus Widget" :comp views.focus/widget :comp-only true}])

(def routes
  (->> route-defs
       (map (fn [{:keys [route page-name]}] [route {:name page-name}]))
       (into [])))

(defn view [opts]
  (let [page-name          (->
                             #_{:clj-kondo/ignore [:unresolved-var]}
                             router/*match* uix/context :data :name)
        by-page-name       (w/index-by :page-name route-defs)
        {:keys [comp comp-only]
         :as   _route-def} (by-page-name page-name)

        ;; create fe db and pass it to every page
        {:keys [conn]} (hooks.db/use-db)
        opts           (assoc opts :conn conn)
        ]
    (if comp
      (if comp-only
        [comp opts]
        [pages/page route-defs comp opts])
      [:div "no page"])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Websocket events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn on-close []
  (log/info "Connection with server closed")
  (log/info "TODO impl reconnect logic"))
(defn on-error [] (log/info "Connection with server error"))
(defn on-open []
  (log/info "Connection with server open"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Bootstrap
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^:dev/after-load mount-root []
  (time-literals.read-write/print-time-literals-cljs!)
  (uix.dom/render
    [wing.uix.router/router-provider
     {:routes routes}
     view
     {:x (js/Date.now)}]
    (.getElementById js/document "app")))

(defn dev-setup []
  (enable-console-print!))

(goog-define SERVER_HOST "localhost")
(goog-define SERVER_PORT 3334)

(def plasma-ws-url (str "ws://" SERVER_HOST ":" SERVER_PORT "/plasma-ws"))

(defn ^:export init
  []
  (dev-setup)
  ;; TODO support websockets reconnecting
  (plasma.client/use-transport!
    (plasma.client/websocket-transport
      plasma-ws-url
      {:on-open  on-open
       :on-close on-close
       :on-error on-error
       :transit-write-handlers
       (merge
         ttl/write-handlers
         dt/write-handlers)
       :transit-read-handlers
       (merge
         ttl/read-handlers
         dt/read-handlers)}))
  (mount-root))
