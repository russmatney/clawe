(ns pages.commits
  (:require
   [hooks.commits]
   [hooks.repos]
   [components.chess]
   [components.debug]
   [components.events]
   [components.git]
   [components.floating]
   [components.repo]
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
        [:div
         {:class    ["hover:text-city-blue-800"
                     "cursor-pointer"]
          :on-click (fn [_] (hooks.repos/fetch-commits repo))}
         [components.floating/popover
          {:hover true :click true
           :anchor-comp
           [:div
            {:class ["text-white"]}
            (:repo/path repo)]
           :popover-comp
           [components.repo/popover repo]}]])]

     [components.events/events-cluster nil (:items commits-resp)]]))
