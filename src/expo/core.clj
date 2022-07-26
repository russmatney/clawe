(ns expo.core
  (:require
   [datascript.core :as d]
   [ralphie.zsh :as zsh]))

(comment
  (def conn
    (->
      (d/empty-db)
      (d/conn-from-db)))

  (d/transact! conn [{:some/new   :piece/of-data
                      :with/attrs 23
                      :and/other  :attrs/enum}])

  ;; pr-str the db to get it stringified
  (pr-str (d/db conn))

  (def expo-db-path (zsh/expand "~/russmatney/clawe/expo/public/expo-db.edn"))
  (def db-str (pr-str (d/db conn)))
  (spit expo-db-path db-str))
