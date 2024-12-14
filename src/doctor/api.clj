(ns doctor.api
  (:require
   [taoensso.telemere :as log]

   [api.workspaces :as workspaces]
   [api.topbar :as topbar]
   [api.todos :as todos]
   [api.blog :as blog]
   [api.pomodoros :as pomodoros]
   [api.repos :as repos]
   [api.screenshots :as screenshots]
   [api.clips :as clips]

   [clawe.restart :as clawe.restart]
   [clawe.mx :as clawe.mx]
   [clawe.toggle :as clawe.toggle]
   [clojure.string :as string]))


(defn route
  "Routes an api request."
  [{:keys [uri query-string] :as req}]
  (log/log! {:level :debug
             :data  {:uri uri :query-string query-string}}
            ["Routing API req" (System/currentTimeMillis)])

  (cond
    (string/starts-with? "/api/pomodoros" uri)
    (pomodoros/route req)

    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    ;; clawe

    (= uri "/reload")
    (do
      (clawe.restart/reload)
      {:status 200 :body "reloaded clawe"})

    (= uri "/topbar/update")
    (do
      (workspaces/push-updated-workspaces)
      (topbar/push-topbar-metadata)
      {:status 200 :body "updated topbar"})

    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    ;; screenshots/clips

    (= uri "/screenshots/update")
    (do
      (screenshots/ingest-screenshots)
      {:status 200 :body "updated screenshots"})

    (= uri "/clips/update")
    (do
      (clips/ingest-clips)
      {:status 200 :body "updated clips"})

    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    ;; git

    (= uri "/git/refresh")
    (do
      (repos/refresh-git-status)
      {:status 200 :body "refreshing git repo status"})

    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    ;; pomodoros/todos

    (= uri "/pomodoros/start")
    (do
      (pomodoros/start-pomodoro)
      {:status 200 :body "starting new pomodoro"})

    (= uri "/todos/update")
    (do
      (todos/reingest-todos)
      {:status 200 :body "updated todos"})

    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    ;; blog

    (= uri "/blog/rebuild")
    (do
      ;; TODO perhaps this should be non-blocking?
      (blog/rebuild-all)
      {:status 200 :body "updated blog"})

    (= uri "/blog/rebuild-indexes")
    (do
      (blog/rebuild-indexes)
      {:status 200 :body "updated blog indexes"})

    (= uri "/blog/rebuild-open-pages")
    (do
      (blog/rebuild-open-pages)
      {:status 200 :body "rebuilt open blog pages"})

    (= uri "/blog/restart-systems")
    (do
      (blog/restart-systems)
      {:status 200 :body "restarted blog systems"})

    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    ;; clawe rofi/mx

    (= uri "/clawe-mx")
    (do
      (clawe.mx/mx)
      {:status 200})

    (= uri "/clawe-mx-fast")
    (do
      (clawe.mx/mx-fast)
      {:status 200})

    (= uri "/clawe-mx-suggestions")
    (do
      (clawe.mx/mx-suggestions)
      {:status 200})

    (= uri "/clawe-mx-open")
    (do
      (clawe.mx/mx-open)
      {:status 200})

    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    ;; clawe toggle

    (= uri "/clawe-toggle")
    (do
      ;; TODO handle query params/body
      (clawe.toggle/toggle {:client/key query-string})
      {:status 200})

    :else
    (do
      (println "uri" uri)
      {:status 404})))

(comment
  (route {:uri "/reload"}))
