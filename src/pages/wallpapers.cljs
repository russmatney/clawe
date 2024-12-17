(ns pages.wallpapers
  (:require
   [uix.core :as uix :refer [defui $]]

   [doctor.ui.db :as ui.db]
   [doctor.ui.hooks.use-db :as hooks.use-db]
   [doctor.ui.handlers :as handlers]
   [pages.db.tables :as db.tables]))

(defui page [_opts]
  (let [n     150
        items (:data (hooks.use-db/use-query
                       {:db->data (fn [db] (ui.db/wallpapers db {:n n}))}))]
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
          ($ db.tables/table-for-doctor-type {:n n :doctor-type :type/wallpaper :entities items})))))
