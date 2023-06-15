(ns doctor.api
  (:require
   ;; [taoensso.timbre :as log]
   [api.workspaces :as workspaces]
   [api.topbar :as topbar]
   [api.todos :as todos]
   [api.blog :as blog]
   [screenshots.core :as screenshots]
   [clips.core :as clips]
   [wallpapers.core :as wallpapers]
   #_[notebooks.clerk :as notebooks.clerk]
   [clojure.string :as string]
   [babashka.fs :as fs]

   [clawe.mx :as clawe.mx]))


(defn route
  "Routes an api request."
  [{:keys [uri] :as _req}]
  ;; (log/debug "Routing API req" uri (System/currentTimeMillis))

  (cond
    (= uri "/reload")
    (do
      (wallpapers/reload)
      {:status 200 :body "reloaded doctor"})

    (= uri "/topbar/update")
    (do
      (workspaces/push-updated-workspaces)
      (topbar/push-topbar-metadata)
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

    (= uri "/clips/update")
    (do
      (clips/ingest-clips)
      {:status 200 :body "updated clips"})

    (= uri "/todos/update")
    (do
      (todos/reingest-todos)
      {:status 200 :body "updated todos"})

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

    (= uri "/clawe-mx")
    (do
      (clawe.mx/mx)
      {:status 200})

    (= uri "/clawe-mx-fast")
    (do
      (clawe.mx/mx-fast)
      {:status 200})

    :else
    (do
      (println "uri" uri)
      {:status 404})))

(comment
  (route {:uri "/reload"}))
