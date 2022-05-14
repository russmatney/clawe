(ns pages.repos
  (:require
   [hooks.repos]))


(defn page [_opts]
  (let [{:keys [repos]} (hooks.repos/use-repos)]
    [:div
     "Repos"

     (for [[i repo] (map-indexed vector repos)]
       [:div
        {:key   i
         :class [""]
         }

        [:div (:git.repo/name repo)]]
       )
     ])


  )
