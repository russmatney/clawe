(ns expo.ui.core
  (:require
   [plasma.client]
   [uix.dom.alpha :as uix.dom]
   [uix.core.alpha :as uix]
   [wing.uix.router :as router]
   [taoensso.timbre :as log]
   [time-literals.data-readers]
   [time-literals.read-write]
   [tick.timezone]
   [tick.locale-en-us]
   [expo.time-literals-transit :as tlt]
   [expo.ui.views.counts :as counts]
   [expo.ui.views.garden :as garden]
   [expo.ui.views.posts :as posts]))

(def routes
  [["/" {:name :page/root}]
   ["/garden" {:name :page/garden}]
   ["/posts" {:name :page/posts}]
   ])

(defn view
  []
  (let [page-name (-> router/*match* uix/context :data :name)]
    [:div
     {:class ["bg-yo-blue-500" "min-h-screen"]}
     [:div
      {:class ["font-nes" "text-lg" "p-4" "text-white"]}
      "Expo"]

     (case page-name
       :page/root   [counts/widget]
       :page/garden [garden/view]
       :page/posts  [posts/view]
       [:div "hi"])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
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
(goog-define SERVER_PORT 5777)

(def ws-url (str "ws://" SERVER_HOST ":" SERVER_PORT "/ws"))

(defn ^:export init
  []
  (plasma.client/use-transport!
    (plasma.client/websocket-transport
      ws-url
      {:on-open                #(log/info "on-open")
       :on-close               #(log/info "on-close")
       :on-error               #(log/info "on-error")
       :transit-write-handlers tlt/write-handlers
       :transit-read-handlers  tlt/read-handlers}))
  (dev-setup)
  (mount-root))
