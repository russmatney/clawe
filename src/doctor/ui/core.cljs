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
   [wing.uix.router :as router]

   [components.icons]
   [components.debug]
   [pages.core :as page]
   [pages.todos]
   [pages.events]
   [pages.screenshots]
   [pages.wallpapers]
   [pages.workspaces]
   [pages.counter]
   [pages.counts]
   [pages.garden]
   [pages.posts]
   [doctor.ui.views.topbar :as views.topbar]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; routes, home
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes
  [["/" {:name :page/home}]
   ["/todo" {:name :page/todos}]
   ["/events" {:name :page/events}]
   ["/topbar" {:name :page/topbar}]
   ["/topbar-bg" {:name :page/topbar-bg}]
   ["/counter" {:name :page/counter}]
   ["/counts" {:name :page/counts}]
   ["/screenshots" {:name :page/screenshots}]
   ["/workspaces" {:name :page/workspaces}]
   ["/wallpapers" {:name :page/wallpapers}]
   ["/garden" {:name :page/garden}]
   ["/posts" {:name :page/posts}]])

(def menu-opts
  [[:page/home "Home"]
   [:page/todos "Todos"]
   [:page/events "Events"]
   [:page/topbar "Top Bar"]
   [:page/topbar-bg "Top Bar BG"]
   [:page/counter "Counter"]
   [:page/counts "Counts"]
   [:page/garden "Garden"]
   [:page/posts "Posts"]
   [:page/wallpapers "Wallpapers"]
   [:page/workspaces "Workspaces"]
   [:page/screenshots "Screenshots"]])

(defn view []
  (let [page-name (-> router/*match* uix/context :data :name)]
    (case page-name
      :page/topbar      [views.topbar/widget]
      :page/home        [page/page menu-opts pages.events/event-page]
      :page/todos       [page/page menu-opts pages.todos/page]
      :page/events      [page/page menu-opts pages.events/event-page]
      :page/topbar-bg   [page/page menu-opts views.topbar/widget]
      :page/counter     [page/page menu-opts pages.counter/page]
      :page/counts      [page/page menu-opts pages.counts/page]
      :page/garden      [page/page menu-opts pages.garden/page]
      :page/posts       [page/page menu-opts pages.posts/page]
      :page/screenshots [page/page menu-opts pages.screenshots/page]
      :page/wallpapers  [page/page menu-opts pages.wallpapers/widget]
      :page/workspaces  [page/page menu-opts pages.workspaces/widget]
      [page/page])))

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
