(ns expo.pages.home
  (:require
   [datascript.core :as d]))

(defn page [{:db/keys [conn db]}]
  [:div
   "Home"

   [:div
    (pr-str conn)]


   (when db
     (println "ent 1" (d/entity db 1))
     (println "ent attr" (:other (d/entity db 1)))
     [:div (d/entity db 1)])


   (when db
     [:div
      (str
        (d/q '[:find [(pull ?a [*])]
               :in $ ?eid
               :where [?eid :db/id ?a]]
             db 1))])])
