(ns notebooks.lichess
  {:nextjournal.clerk/toc        true
   :nextjournal.clerk/visibility {:code :hide :result :hide}}
  (:require
   [nextjournal.clerk :as clerk]
   [chess.db :as chess.db]
   [notebooks.viewers.my-notebooks :as my-notebooks]
   [tick.core :as t]))

(clerk/add-viewers! [my-notebooks/viewer])

(def games
  (chess.db/games-since-last-week))

(comment
  (count games)

  (->> games
       (take 2)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
{:nextjournal.clerk/visibility {:result :show}}

;; # Lichess Games

(clerk/table
  {::clerk/width :full}
  (->> games
       (sort-by :lichess.game/created-at t/>)
       #_(map #(select-keys
                 % #{:commit/author-date
                     :commit/directory
                     :commit/short-hash
                     :commit/subject
                     :commit/body}))
       (map (fn [game]
              game))))
