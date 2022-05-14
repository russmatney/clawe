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

   [dates.transit-time-literals :as ttl]
   [pages.counts]
   [pages.garden]
   [pages.posts]))

(def routes
  [["/" {:name :page/root}]
   ["/garden" {:name :page/garden}]
   ["/posts" {:name :page/posts}]])

(defn view
  []
  (let [page-name (-> router/*match* uix/context :data :name)]
    [:div
     {:class ["bg-yo-blue-500" "min-h-screen"]}
     [:div
      {:class ["font-nes" "text-lg" "p-4" "text-white"]}
      "Expo"]

     (case page-name
       :page/root   [pages.counts/page]
       :page/garden [pages.garden/page]
       :page/posts  [pages.posts/page]
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
(goog-define SERVER_PORT 4443)

(def ws-url (str "ws://" SERVER_HOST ":" SERVER_PORT "/ws"))

(defn ^:export init
  []
  (plasma.client/use-transport!
    (plasma.client/websocket-transport
      ws-url
      {:on-open                #(log/info "on-open")
       :on-close               #(log/info "on-close")
       :on-error               #(log/info "on-error")
       :transit-write-handlers ttl/write-handlers
       :transit-read-handlers  ttl/read-handlers}))
  (dev-setup)
  (mount-root))
