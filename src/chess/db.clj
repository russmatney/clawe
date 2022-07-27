(ns chess.db
  (:require
   [chess.core :as chess]
   [dates.tick :as dates.tick]
   [defthing.db :as db]
   [clojure.edn :as edn]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fetch games
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn games-since [{:keys [since]}]
  (chess/fetch-games
    {:opening  true
     :evals    true
     :literate true
     :analysis true
     :since    since}))

(defn games-since-last-week []
  (games-since {:since (dates.tick/a-week-ago-ms)}))

(defn games-since-last-month []
  (games-since {:since (dates.tick/a-month-ago-ms)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; sync to db
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sync-games-to-db
  ([] (sync-games-to-db (games-since-last-week)))
  ([games] (db/transact games)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fetch games from db
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch-db-games []
  (->>
    (db/query
      '[:find (pull ?e [*])
        :where
        [?e :lichess.game/id ?id]])
    (map first)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; sandbox
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (sync-games-to-db (games-since-last-month))

  (->>
    (games-since-last-month)
    count
    ;; (sync-games-to-db)
    )

  (->>
    (games-since-last-month)
    (take 3)
    ;; (sync-games-to-db)
    )

  (count
    (fetch-db-games))

  (->>
    (games-since-last-month)
    (filter (comp seq :lichess.game/analysis))
    (map (fn [g] (update g :lichess.game/analysis edn/read-string)))
    (take 3))

  (->>
    (games-since-last-month)
    (take 3)
    ;; first
    )

  (chess/clear-cache))
