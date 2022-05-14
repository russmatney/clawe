(ns chess.db
  (:require
   [chess.core :as chess]
   [tick.core :as t]
   [wing.core :as w]
   [defthing.db :as db]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; time helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn an-x-ago [duration]
  (-> (t/today)
      (t/at (t/midnight))
      (t/<< duration)
      t/inst
      inst-ms) )

(defn a-week-ago []
  (an-x-ago (t/new-duration 7 :days)))

(defn a-month-ago []
  (an-x-ago (t/new-duration 31 :days)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fetch games
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn games-since [{:keys [since]}]
  (chess/fetch-games
    {:opening true
     :evals   true
     :since   since}))

(defn games-since-last-week []
  (games-since {:since (a-week-ago)}))

(defn games-since-last-month []
  (games-since {:since (a-month-ago)}))

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
    (take 3)
    ;; (sync-games-to-db)
    )

  (count
    (fetch-db-games))

  (->>
    (fetch-db-games)
    (take 3)
    first)

  (chess/clear-cache))
