(ns doctor.api.core
  (:require
   [taoensso.timbre :as log]
   [doctor.api.core :as api]
   [doctor.api.workspaces :as d.workspaces]
   [doctor.api.topbar :as d.topbar]
   [doctor.api.todos :as d.todos]
   [doctor.ui.views.screenshots :as screenshots]
   [doctor.api.wallpapers :as d.wallpapers]))


(defn route
  "Routes an api request."
  [{:keys [uri] :as _req}]
  (log/info "Routing API req" uri (System/currentTimeMillis))

  (cond
    (= uri "/reload")
    (do
      (d.wallpapers/reload)
      {:status 200 :body "reloaded doctor"})

    (= uri "/topbar/update")
    (do
      (d.workspaces/update-workspaces)
      (d.topbar/update-topbar-metadata)
      {:status 200 :body "updated topbar"})

    (= uri "/dock/update")
    (do
      (d.workspaces/update-workspaces)
      (d.topbar/update-topbar-metadata)
      {:status 200 :body "updated topbar"})

    (= uri "/screenshots/update")
    (do
      (screenshots/update-screenshots)
      {:status 200 :body "updated screenshots"})

    (= uri "/todos/update")
    (do
      (d.todos/update-todos)
      {:status 200 :body "updated todos"})))

(comment
  (route {:uri "/reload"})

  (route {:uri "/dock/update"}))
