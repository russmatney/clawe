(ns doctor.api
  (:require
   [taoensso.timbre :as log]
   [api.workspaces :as workspaces]
   [api.topbar :as topbar]
   [api.todos :as todos]
   [api.screenshots :as screenshots]
   [api.wallpapers :as wallpapers]
   [defthing.db :as defthing.db]))


(defn route
  "Routes an api request."
  [{:keys [uri] :as _req}]
  (log/info "Routing API req" uri (System/currentTimeMillis))

  (cond
    (= uri "/reload")
    (do
      (wallpapers/reload)
      {:status 200 :body "reloaded doctor"})

    (= uri "/topbar/update")
    (do
      (workspaces/update-workspaces)
      (topbar/update-topbar-metadata)
      {:status 200 :body "updated topbar"})

    (= uri "/screenshots/update")
    (do
      (screenshots/update-screenshots)
      {:status 200 :body "updated screenshots"})

    (= uri "/todos/update")
    (do
      (todos/update-todos)
      {:status 200 :body "updated todos"})

    (= uri "/db/restart-conn")
    (do
      (defthing.db/restart-conn)
      {:status 200 :body "Restarted defthing.db connection"})))

(comment
  (route {:uri "/reload"}))
