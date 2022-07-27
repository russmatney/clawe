(ns expo.pages.home
  (:require
   [datascript.core :as d]
   [components.debug :as components.debug]
   [components.garden :as components.garden]))

(defn page [{:db/keys [_conn db]}]
  (let [posts (->> (d/q '[:find (pull ?e [*])
                          :where  ;; TODO create and use an expo type
                          [?e :doctor/type :type/garden]]
                        db)
                   (map first))]
    [:div
     "Home"

     (for [[i p] (->> posts (map-indexed vector))]
       [:div
        {:key i}
        [:div
         {:class ["text-white" "text-xl"]}
         (:org/name p)

         [components.debug/raw-metadata p]]

        [components.garden/garden-node p]])


     ;; entity example
     #_(when db
         (println "ent 1" (d/entity db 1))
         (println "ent attr" (:other (d/entity db 1)))
         [:div "entity example" (d/entity db 1)])

     ;; pull example
     #_(when db
         (let [res (d/pull db '[*] 1)]
           (println "res" res)
           [:div "pull example" (str res)]))

     ;; query example
     #_(when db
         (let [res
               (d/q '{:find  [?e]
                      :where [[?e ?attr ?value]]
                      :in    [$ ?attr ?value]}
                    db :some/new :piece/of-data)]
           (println "res" res)
           [:div "query example" (str res)]))]))

(comment
  5
  )
