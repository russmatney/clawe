(ns pages.wallpapers
  (:require
   [doctor.ui.db :as ui.db]
   [pages.db.tables :as db.tables]
   [doctor.ui.handlers :as handlers]))

(defn page [{:keys [conn]}]
  (let [n     150
        items (ui.db/wallpapers conn {:n n})]
    [:div
     [:button {:class ["bg-slate-800"
                       "p-4"
                       "border"
                       "border-slate-600"
                       "rounded-xl"
                       "font-mono"
                       "text-white"]
               :on-click
               (fn [_] (handlers/ingest-wallpapers))}
      "Ingest wallpapers"]

     [:div
      {:class ["p-2"]}
      [db.tables/table-for-doctor-type {:n n} :type/wallpaper items]]]))
