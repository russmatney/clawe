(ns pages.commits
  (:require
   [hooks.commits]
   [hooks.repos]
   [components.events]
   [components.git]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page [_opts]
  (let [repo-resp    (hooks.repos/use-repos)
        repos        (:items repo-resp)
        commits-resp (hooks.commits/use-commits)]

    [:div
     {:class ["flex" "flex-col" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              "text-white"]}

     [:div
      {:class ["grid" "grid-cols-4 gap-4"]}
      (for [repo repos]
        ^{:key (:repo/path repo)}
        [:div
         {:class ["p-2"]}
         [components.git/repo-popover repo]])]

     (if (seq (:items commits-resp))
       [components.events/events-cluster nil (:items commits-resp)]
       [:div "No commits found!"])]))
