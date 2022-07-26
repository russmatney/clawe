(ns expo.pages.home
  (:require
   [datascript.core :as d]
   [datascript.db :as db]
   ))

(defn page [{:db/keys [conn db]}]
  [:div
   "Home"

   [:div
    (pr-str conn)]

   (when db
     (println "db" db)
     (println "ent 1" (d/entity db 1))
     [:div (d/entity db 1)])])
