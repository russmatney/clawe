(ns doctor.api
  (:require
   [taoensso.timbre :as log]
   [api.workspaces :as workspaces]
   [api.topbar :as topbar]
   [api.todos :as todos]
   [screenshots.core :as screenshots]
   [wallpapers.core :as wallpapers]))


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
      (screenshots/ingest-screenshots)
      {:status 200 :body "updated screenshots"})

    (= uri "/todos/update")
    (do
      (todos/update-todos)
      {:status 200 :body "updated todos"})))

(comment
  (route {:uri "/reload"}))
