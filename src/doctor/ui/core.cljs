(ns doctor.ui.core
  (:require
   [dates.transit-time-literals :as ttl]
   [plasma.client]
   [taoensso.timbre :as log]
   [time-literals.data-readers]
   [time-literals.read-write]
   [tick.timezone]
   [tick.locale-en-us]
   [uix.core.alpha :as uix]
   [uix.dom.alpha :as uix.dom]
   [wing.core :as w]
   [wing.uix.router :as router]

   [components.icons]
   [components.debug]
   [pages.core :as pages]
   [pages.todos]
   [pages.events]
   [pages.screenshots]
   [pages.wallpapers]
   [pages.workspaces]
   [pages.counter]
   [pages.counts]
   [pages.garden]
   [pages.posts]
   [pages.repos]
   [doctor.ui.views.topbar :as views.topbar]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; routes, home
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def route-defs
  [{:route "/" :page-name :page/home :label "Home" :comp pages.events/page}
   {:route "/todo" :page-name :page/todos :label "Todos" :comp pages.todos/page}
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
   {:route "/repos" :page-name :page/repos :label "Repos" :comp pages.repos/page}])

(def routes
  (->> route-defs
       (map (fn [{:keys [route page-name]}] [route {:name page-name}]))
       (into [])))

(defn view [opts]
  (let [page-name          (-> router/*match* uix/context :data :name)
        by-page-name       (w/index-by :page-name route-defs)
        {:keys [comp comp-only]
         :as   _route-def} (by-page-name page-name)]
    (if comp
      (if comp-only
        [comp opts]
        [pages/page route-defs comp opts])
      [pages/page])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Websocket events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn on-close [] (log/info "Connection with server closed"))
(defn on-error [] (log/info "Connection with server error"))
(defn on-open [] (log/info "Connection with server open"))

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

(def ws-url (str "ws://" SERVER_HOST ":" SERVER_PORT "/ws"))

(defn ^:export init
  []
  (dev-setup)
  (plasma.client/use-transport!
    (plasma.client/websocket-transport
      ws-url
      {:on-open                on-open
       :on-close               on-close
       :on-error               on-error
       :transit-write-handlers ttl/write-handlers
       :transit-read-handlers  ttl/read-handlers}))
  (mount-root))
