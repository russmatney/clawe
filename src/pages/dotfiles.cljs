(ns pages.dotfiles
  (:require
   [uix.core :as uix :refer [$ defui]]

   [doctor.ui.db :as ui.db]
   [doctor.ui.handlers :as handlers]
   [doctor.ui.hooks.use-db :as hooks.use-db]
   [doctor.ui.hooks.use-workspaces :as hooks.use-workspaces]
   [doctor.ui.views.workspaces :as views.workspaces]

   [pages.db.tables :as db.tables]
   ))

(defn wsps->wsp-named [wsps wsp-title]
  (some->> wsps
           (filter #(#{wsp-title} (:workspace/title %)))
           first))


(defui page [opts]
  (let [{:keys [active-workspaces all-clients]}
        (hooks.use-workspaces/use-workspaces)

        page-size 10
        n         150

        wallpapers
        (:data (hooks.use-db/use-query
                 {:db->data (fn [db] (ui.db/wallpapers db {:n n}))}))

        workspace (wsps->wsp-named
                    active-workspaces "dotfiles")]

    (println "wallpapers" (count wallpapers))

    ($ :div
       ($ :div
          ($ views.workspaces/workspace-card
             (assoc opts :workspace workspace)))

       ($ :hr)

       ($ :div
          ($ :button {:class ["bg-slate-800"
                              "p-4"
                              "border"
                              "border-slate-600"
                              "rounded-xl"
                              "font-mono"
                              "text-white"]
                      :on-click
                      (fn [_] (handlers/ingest-wallpapers))}
             "Ingest wallpapers")

          ($ :div
             {:class ["p-2"]}
             ($ db.tables/table-for-doctor-type
                {:n           page-size
                 :doctor-type :type/wallpaper
                 :entities    wallpapers}))))))
