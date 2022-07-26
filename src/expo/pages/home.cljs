(ns expo.pages.home
  (:require
   [datascript.core :as d]))

;; db when this worked:
;; #datascript/DB {:schema nil, :datoms [[1 :other :attrs/enum 536870913] [1 :some/new :piece/of-data 536870913] [1 :with/attrs 23 536870913]]}

(defn page [{:db/keys [conn db]}]
  [:div
   "Home"

   [:div
    (pr-str conn)]


   ;; entity example
   (when db
     (println "ent 1" (d/entity db 1))
     (println "ent attr" (:other (d/entity db 1)))
     [:div "entity example" (d/entity db 1)])

   ;; pull example
   (when db
     (let [res (d/pull db '[*] 1)]
       (println "res" res)
       [:div "pull example" (str res)]))

   ;; query example
   (when db
     (let [res
           (d/q '{:find  [?e]
                  :where [[?e ?attr ?value]]
                  :in    [$ ?attr ?value]}
                db :some/new :piece/of-data)]
       (println "res" res)
       [:div "query example" (str res)]))
   ])
