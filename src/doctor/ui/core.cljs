(ns doctor.ui.core
  (:require
   [plasma.client]
   [doctor.time-literals-transit :as tlt]
   [taoensso.timbre :as log]
   [time-literals.data-readers]
   [time-literals.read-write]
   [tick.timezone]
   [tick.locale-en-us]
   [wing.uix.router :as router]
   [uix.core.alpha :as uix]
   [uix.dom.alpha :as uix.dom]

   [doctor.ui.views.todos :as views.todos]
   [doctor.ui.views.topbar :as views.topbar]
   [doctor.ui.views.popover :as views.popover]
   [doctor.ui.views.screenshots :as views.screenshots]
   [doctor.ui.views.wallpapers :as views.wallpapers]
   [doctor.ui.views.workspaces :as views.workspaces]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; routes, home
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes
  [["/" {:name :page/home}]
   ["/todos" {:name :page/todos}]
   ["/popup" {:name :page/popup}]
   ["/topbar" {:name :page/topbar}]
   ["/topbar-bg" {:name :page/topbar-bg}]
   ["/counter" {:name :page/counter}]
   ["/screenshots" {:name :page/screenshots}]
   ["/workspaces" {:name :page/workspaces}]
   ["/wallpapers" {:name :page/wallpapers}]])

(defn default-main []
  [:div
   [:p.text-city-pink-100.p-4
    "No app selected, defaulting..."]
   [views.screenshots/widget]
   ])


(defn home [main]
  (let [params            (router/use-route-parameters)
        main              (or main default-main)
        ;; fallback main disabled for now, seems to some issue mounting/unmounting defhandlers
        current-page-name (-> router/*match* uix/context :data :name)]
    [:div
     {:class ["bg-city-blue-900"
              "min-h-screen"
              "min-w-100"]}
     [:div
      {:class ["flex" "flex-row"
               "space-between"
               "min-w-100"]}
      [:div {:class ["flex" "flex-col" "p-6"
                     "text-city-pink-100"
                     "text-xxl"
                     "font-nes"]}
       (for [[page-name label] [[:page/home "Home"]
                                [:page/todos "Todos"]
                                [:page/popup "Pop Up"]
                                [:page/topbar "Top Bar"]
                                [:page/topbar-bg "Top Bar BG"]
                                [:page/counter "Counter"]
                                [:page/wallpapers "Wallpapers"]
                                [:page/workspaces "Workspaces"]
                                [:page/screenshots "Screenshots"]]]
         ^{:key page-name}
         [:a {:class (when (#{current-page-name} page-name)
                       ["text-city-pink-400"
                        "text-bold"])
              :href  (router/href page-name)} label])]
      [:div {:class ["flex" "flex-col" "p-6"
                     "text-city-pink-100" "text-xl"]}
       [:p (str "Router:" (clj->js (uix/context router/*router*)))]
       [:p (str "Match: "  @params)]]]
     (when main [main])]))

(defn counter []
  (let [page-name (-> router/*match* uix/context :data :name)
        count     (router/use-route-parameters [:query :count])]
    [:div
     (when (= :page/counter page-name)
       [:button {:on-click #(swap! count inc)} @count])]))

(defn view
  []
  (let [page-name (-> router/*match* uix/context :data :name)]
    (case page-name
      :page/home        [home views.popover/tabs]
      :page/todos       [home views.todos/widget]
      :page/popup       [views.popover/popup]
      :page/topbar      [views.topbar/widget]
      :page/topbar-bg   [home views.topbar/widget]
      :page/counter     [home counter]
      :page/screenshots [home views.screenshots/widget]
      :page/wallpapers  [home views.wallpapers/widget]
      :page/workspaces  [home views.workspaces/widget]
      [home nil])))

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
     view]
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
       :transit-write-handlers tlt/write-handlers
       :transit-read-handlers  tlt/read-handlers}))
  (mount-root))
