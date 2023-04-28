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
   [hiccup-icons.octicons :as octicons]
   [clojure.string :as string]
   [taoensso.encore :as enc]

   [pages.core :as pages]
   [pages.db :as pages.db]
   [pages.events :as pages.events]
   [pages.screenshots :as pages.screenshots]
   [pages.wallpapers :as pages.wallpapers]
   [pages.garden :as pages.garden]
   [pages.posts :as pages.posts]
   [pages.journal :as pages.journal]
   [hooks.db :as hooks.db]

   [doctor.ui.views.blog :as views.blog]
   [doctor.ui.views.commits :as views.commits]
   [doctor.ui.views.focus :as views.focus]
   [doctor.ui.views.todos :as views.todos]
   [doctor.ui.views.today :as views.today]
   [doctor.ui.views.pomodoro :as views.pomodoro]
   [doctor.ui.views.topbar :as views.topbar]
   [doctor.ui.views.chess-games :as views.chess-games]
   [doctor.ui.views.dashboard :as views.dashboard]
   [doctor.ui.views.workspaces :as views.workspaces]))

(defn output-fn
  [data]
  (let [{:keys [level ?err #_vargs _msg_ ?ns-str ?file _hostname_
                _timestamp_ ?line output-opts]}
        data]

    (str
      #_(when-let [ts (force timestamp_)]
          (str ts " "))
      #_ (force hostname_)
      #_ " "
      (string/upper-case (name level))  " "
      "[" (or ?ns-str ?file "?") ":" (or ?line "?") "]: "

      (when-let [msg-fn (get output-opts :msg-fn log/default-output-msg-fn)]
        (msg-fn data))

      (when-let [_err ?err]
        (when-let [ef (get output-opts :error-fn log/default-output-error-fn)]
          (when-not   (get output-opts :no-stacktrace?) ; Back compatibility
            (str enc/system-newline
                 (ef data))))))))

(log/merge-config! {:output-fn output-fn})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; routes, home
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def route-defs
  [{:route "/" :page-name :page/home :label "Dashboard" :comp views.dashboard/widget
    :icon  octicons/beaker16}
   {:route "/focus" :page-name :page/focus :label "Focus" :comp views.focus/widget :hide-header true
    :icon  octicons/light-bulb16}
   {:route "/todos" :page-name :page/todos :label "Todos" :comp views.todos/widget
    :icon  octicons/checklist16}
   {:route "/blog" :page-name :page/blog :label "Blog" :comp views.blog/widget :hide-header true
    :icon  views.blog/icon}
   {:route "/today" :page-name :page/today :label "Today" :comp views.today/widget
    :icon  views.today/icon}
   {:route "/wallpapers" :page-name :page/wallpapers :label "Wallpapers" :comp pages.wallpapers/page
    :icon  octicons/image16}
   {:route "/events" :page-name :page/events :label "Events" :comp pages.events/page
    :icon  octicons/calendar16}
   {:route "/pomodoros" :page-name :page/pomodoros :label "Pomodoros" :comp views.pomodoro/widget
    :icon  octicons/clock16}
   {:route "/commits" :page-name :page/commits :label "Commits" :comp views.commits/widget
    :icon  views.commits/icon}
   {:route "/db" :page-name :page/db :label "DB" :comp pages.db/page
    :icon  octicons/archive16}
   {:route "/screenshots" :page-name :page/screenshots :label "Screenshots" :comp pages.screenshots/page
    :icon  octicons/screen-full16}
   ;; {:route "/counter" :page-name :page/counter :label "Counter" :comp pages.counter/page}
   {:route "/garden" :page-name :page/garden :label "Garden" :comp pages.garden/page
    :icon  octicons/workflow16}
   {:route "/posts" :page-name :page/posts :label "Posts" :comp pages.posts/page
    :icon  octicons/comment16}
   {:route "/workspaces" :page-name :page/workspaces :label "Workspaces" :comp views.workspaces/widget
    :icon  octicons/clippy16}
   {:route "/journal" :page-name :page/journal :label "Journal" :comp pages.journal/page
    :icon  octicons/book16}
   {:route "/chess-games" :page-name :page/chess :label "Chess Games" :comp views.chess-games/widget
    :icon  octicons/moon16}
   {:route "/topbar" :page-name :page/topbar :label "Top Bar" :comp views.topbar/widget :comp-only true}
   {:route "/topbar-bg" :page-name :page/topbar-bg :label "Top Bar BG" :comp views.topbar/widget}])

(def routes
  (->> route-defs
       (map (fn [{:keys [route page-name]}] [route {:name page-name}]))
       (into [])))

(defn view [opts]
  (let [page-name         (-> #_{:clj-kondo/ignore [:unresolved-var]}
                              router/*match* uix/context :data :name)
        by-page-name      (w/index-by :page-name route-defs)
        {:keys [comp comp-only]
         :as   page-opts} (by-page-name page-name)

        ;; create fe db and pass it to every page
        {:keys [conn]} (hooks.db/use-db)
        opts           (-> opts (assoc :conn conn
                                       :route-defs route-defs)
                           (merge page-opts))]
    (if comp
      (if comp-only [comp opts] [pages/page comp opts])
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
       (merge ttl/write-handlers dt/write-handlers)
       :transit-read-handlers
       (merge ttl/read-handlers dt/read-handlers)}))
  (mount-root))
