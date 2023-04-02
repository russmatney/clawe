(ns doctor.api
  (:require
   [taoensso.timbre :as log]
   [api.workspaces :as workspaces]
   [api.topbar :as topbar]
   [api.todos :as todos]
   [api.blog :as blog]
   [screenshots.core :as screenshots]
   [wallpapers.core :as wallpapers]
   #_[notebooks.clerk :as notebooks.clerk]
   [clojure.string :as string]
   [babashka.fs :as fs]))


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

    (= uri "/rerender/notebooks")
    #_ (notebooks.clerk/update-open-notebooks)
    {:status 200 :body "notebooks rerender disabled"}

    (string/starts-with? uri "/rerender/notebooks/")
    (let [nb-name (fs/file-name uri)]
      (if nb-name
        #_(notebooks.clerk/update-open-notebooks (symbol (str "notebooks." nb-name)))
        {:status 200 :body (str nb-name " notebook rerender disabled")}
        {:status 404 :body "notebook name not found"}))

    (= uri "/screenshots/update")
    (do
      (screenshots/ingest-screenshots)
      {:status 200 :body "updated screenshots"})

    (= uri "/todos/update")
    (do
      (todos/update-todos)
      {:status 200 :body "updated todos"})

    (= uri "/blog/rebuild")
    (do
      (blog/rebuild-all)
      {:status 200 :body "updated blog"})))

(comment
  (route {:uri "/reload"}))
