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
   [wing.core :as w]

   [dates.transit-time-literals :as ttl]
   [pages.core :as pages]
   [pages.counts]
   [pages.garden]
   [pages.posts]))

(def route-defs
  [{:route "/" :page-name :page/home :label "Home" :comp pages.counts/page}
   {:route "/garden" :page-name :page/garden :label "Garden" :comp pages.garden/page}
   {:route "/posts" :page-name :page/posts :label "Posts" :comp pages.posts/page}])

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
;;
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
