(ns doctor.ui.core
  (:require
   [plasma.client]
   [dates.transit-time-literals :as ttl]
   [taoensso.timbre :as log]
   [time-literals.data-readers]
   [time-literals.read-write]
   [tick.timezone]
   [tick.locale-en-us]
   [wing.uix.router :as router]
   [uix.core.alpha :as uix]

   [uix.dom.alpha :as uix.dom]
   [components.icons]
   [components.debug]

   [pages.core :as page]

   [doctor.ui.views.todos :as views.todos]
   [doctor.ui.views.topbar :as views.topbar]
   [doctor.ui.views.popup :as views.popup]
   [doctor.ui.views.events :as views.events]
   [doctor.ui.views.screenshots :as views.screenshots]
   [doctor.ui.views.wallpapers :as views.wallpapers]
   [doctor.ui.views.workspaces :as views.workspaces]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; routes, home
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes
  [["/" {:name :page/home}]
   ["/todo" {:name :page/todos}]
   ["/popup" {:name :page/popup}]
   ["/events" {:name :page/events}]
   ["/topbar" {:name :page/topbar}]
   ["/topbar-bg" {:name :page/topbar-bg}]
   ["/counter" {:name :page/counter}]
   ["/screenshots" {:name :page/screenshots}]
   ["/workspaces" {:name :page/workspaces}]
   ["/wallpapers" {:name :page/wallpapers}]])

(def menu-opts
  [[:page/home "Home"]
   [:page/todos "Todos"]
   [:page/popup "Pop Up"]
   [:page/events "Events"]
   [:page/topbar "Top Bar"]
   [:page/topbar-bg "Top Bar BG"]
   [:page/counter "Counter"]
   [:page/wallpapers "Wallpapers"]
   [:page/workspaces "Workspaces"]
   [:page/screenshots "Screenshots"]])

(defn counter []
  (let [page-name     (-> router/*match* uix/context :data :name)
        query-params  (router/use-route-parameters [:query])
        the-count     (router/use-route-parameters [:query :count])
        the-count-int (try (js/parseInt @the-count) (catch js/Error e
                                                      nil))]

    [:div
     {:class ["p-6" "bg-city-orange-light-800"
              "text-city-orange-light-200"
              "flex" "flex-col"
              "text-4xl"]}

     [components.debug/raw-metadata {:initial-show true
                                     :label        false}
      (assoc (js->clj @query-params)
             :page-name page-name
             :the-count-int the-count-int)]

     [:button {:class    ["bg-city-red-dark-700" "rounded" "shadow" "py-2" "px-4" "m-2"]
               :on-click #(reset! the-count (inc the-count-int))}
      (or @the-count "the-count!")]]))

(defn view
  []
  (let [page-name (-> router/*match* uix/context :data :name)]
    (case page-name
      :page/home        [page/page menu-opts views.events/event-page]
      :page/todos       [page/page menu-opts views.todos/widget]
      :page/popup       [page/page menu-opts views.popup/popup]
      :page/events      [page/page menu-opts views.events/event-page]
      :page/topbar      [views.topbar/widget]
      :page/topbar-bg   [page/page menu-opts views.topbar/widget]
      :page/counter     [page/page menu-opts counter]
      :page/screenshots [page/page menu-opts views.screenshots/widget]
      :page/wallpapers  [page/page menu-opts views.wallpapers/widget]
      :page/workspaces  [page/page menu-opts views.workspaces/widget]
      [page/page])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Websocket events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn on-close []
  (log/info "Connection with server closed")
  )

(defn on-error []
  (log/info "Connection with server error")
  )

(defn on-open []
  (log/info "Connection with server open")
  )

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
