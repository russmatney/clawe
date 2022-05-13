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

   [hiccup-icons.octicons :as octicons]

   [uix.dom.alpha :as uix.dom]

   [keybind.core :as key]

   [components.floating :as floating]
   [components.icons]

   [doctor.ui.tauri :as tauri]
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

(defn default-main []
  [:div
   [:p.text-city-pink-100.p-4
    "No app selected, defaulting..."]
   [views.events/event-page]])

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

(def page-name->route
  (->> routes
       (map (fn [[rt {:keys [name]}]]
              [name rt]))
       (into {})))

(defn menu []
  (let [params            (router/use-route-parameters)
        current-page-name (-> router/*match* uix/context :data :name)]
    [:div
     {:class
      ["flex" "flex-col" "p-6"
       "text-city-pink-100"
       "text-xxl"
       "font-nes"]}
     (for [[i [page-name label]]
           (->> menu-opts (map-indexed vector))]
       [:a {:key  i
            :class
            (concat
              ["hover:text-city-pink-500"]
              (cond
                (#{current-page-name} page-name)
                ["text-city-pink-400" "text-bold"]))
            :href (router/href page-name)} label])]))

(defn page
  "Accepts a main component and wraps it in page helpers with a menu."
  [main]
  (let [params             (router/use-route-parameters)
        main               (or main default-main)
        current-page-name  (-> router/*match* uix/context :data :name)
        indexed-menu-items (->> menu-opts (map-indexed vector))]
    [:div
     {:class ["bg-city-blue-900"
              "min-h-screen"
              "w-full"]}
     [:div {:class ["flex" "flex-row"
                    "bg-city-brown-600"
                    "shadow"
                    "shadow-city-brown-900"]}
      [:div
       {:class ["font-nes"
                "pt-3" "pl-3"
                "text-city-pink-200"
                "capitalize"]}
       current-page-name]

      [floating/popover
       {:click             true
        :offset            10
        :anchor-comp-props {:class ["ml-auto"]}
        :anchor-comp
        [:div
         {:class ["p-3" "text-city-pink-100"
                  "cursor-pointer"
                  "hover:text-city-pink-400"]}
         [components.icons/icon-comp
          {:text "Menu"
           :icon octicons/list-unordered}]]
        :popover-comp
        [:div
         {:class ["bg-city-blue-800"
                  "shadow-lg"
                  "shadow-city-pink-800"
                  ]}
         [menu]]}]]

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
      :page/home        [page views.events/event-page]
      :page/todos       [page views.todos/widget]
      :page/popup       [page views.popup/popup]
      :page/events      [page views.events/event-page]
      :page/topbar      [views.topbar/widget]
      :page/topbar-bg   [page views.topbar/widget]
      :page/counter     [page counter]
      :page/screenshots [page views.screenshots/widget]
      :page/wallpapers  [page views.wallpapers/widget]
      :page/workspaces  [page views.workspaces/widget]
      [page nil])))

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
