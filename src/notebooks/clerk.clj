^{:nextjournal.clerk/visibility {:code :hide}}
(ns notebooks.clerk
  (:require
   [nextjournal.clerk :as clerk]
   [clawe.wm :as wm]
   [garden.db :as garden.db]

   [db.core :as db]
   ))

;; # Clerk Scratchpad

^{::clerk/visibility {:code :hide}}
(clerk/md
  (->>
    (range 1 7)
    (map (fn [x]
           (str
             (->> (range x) (map (constantly "#")) (apply str))
             " [ " x " ] wut up \n")))
    (apply str)))

^{::clerk/visibility {:code   :hide
                      :result :hide}
  ::clerk/no-cache   true}
(def wsp (wm/current-workspace))

^{::clerk/visibility {:code :hide}}
(clerk/md
  (str "## current workspace: " (:workspace/title wsp)))

;; # Recent org

^{::clerk/visibility {:code :hide}}
(->>
  (db/query '[:find (pull ?e [*])
              :where
              [?e :doctor/type :type/garden]
              [?e :org/source-file ?src]])
  (map first)
  )



;; # Mind Maps
