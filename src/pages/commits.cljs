(ns pages.commits
  (:require
   [hooks.commits]
   [hooks.repos]
   [components.chess]
   [components.debug]
   [components.events]
   [components.git]
   [components.floating]
   [components.screenshot]
   [components.timeline]
   [components.todo]))

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
              "bg-yo-blue-700"]}

     [:div
      {:class ["flex" "flex-col" "text-white"]}
      (for [repo repos]
        ^{:key (:repo/path repo)}
        [components.git/repo-popover repo])]

     [components.events/events-cluster nil (:items commits-resp)]]))
