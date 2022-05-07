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

   [keybind.core :as key]

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

(comment
  (page-name->route :page/topbar)

  (for [[i [name label]] (map-indexed vector menu-opts)]
    (println i name label))

  "hi"

  (router/href :page/events)

  (js/location "https://google.com")
  (set! (.-location js/window) "/popup")

  ;; reload:
  (set! (.-location js/window) "somepath")

  ;; set hash?
  (set! (.-hash js/window.location) "somehash")
  )


(defn home [main]
  (let [params             (router/use-route-parameters)
        main               (or main default-main)
        ;; fallback main disabled for now, seems to some issue mounting/unmounting defhandlers
        current-page-name  (-> router/*match* uix/context :data :name)
        cursor-idx         (uix/state
                             (->> menu-opts (map first)
                                  ((fn [xs] (.indexOf xs current-page-name)))))
        indexed-menu-items (->> menu-opts (map-indexed vector))]
    (key/bind! "J" ::cursor-down
               (fn [ev]
                 (swap! cursor-idx (fn [v]
                                     (let [new-v (inc v)]
                                       (if (> new-v (dec (count indexed-menu-items)))
                                         (dec (count indexed-menu-items))
                                         new-v))))))
    (key/bind! "K" ::cursor-up
               (fn [ev]
                 (swap! cursor-idx
                        (fn [v]
                          (let [new-v (dec v)]
                            (if (< new-v 0)
                              0 new-v))))))
    ;; (key/bind! "enter"
    ;;            ::cursor-select
    ;;            (fn [e]
    ;;              (println "enter pressed")
    ;;              (println e)
    ;;              (when-let [[_i [page-name label]]
    ;;                         (nth indexed-menu-items @cursor-idx)]
    ;;                (set! (.-location js/window)
    ;;                      (page-name->route page-name)))))
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
       (for [[i [page-name label]]
             indexed-menu-items]
         ^{:key i}
         [:a {:class
              (cond
                (#{current-page-name} page-name)
                ["text-city-pink-400" "text-bold"]
                (#{@cursor-idx} i)
                ["text-city-red-400" "text-bold"])
              :href (router/href page-name)} label])]
      [:div {:class ["flex" "flex-col" "p-6"
                     "text-city-pink-100" "text-xl"]}
       [:p (str "Router:" (uix/context router/*router*))]
       [:p (str "Match: "  @params)]]
      (let [{:keys [tauri? open?] :as popup} (tauri/use-popup)]
        [:div {:class ["ml-auto"
                       "flex" "flex-col" "p-6"
                       "text-city-pink-100" "text-xl"]}
         (when (and tauri? @open?)
           [:button {:on-click (fn [_] ((:hide popup)))} "Hide Popup"])
         (when (and tauri? (not @open?))
           [:button {:on-click (fn [_] ((:show popup)))} "Show Popup"])])]

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
      :page/home        [home views.events/event-page]
      :page/todos       [home views.todos/widget]
      :page/popup       [home views.popup/popup]
      :page/events      [home views.events/event-page]
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
