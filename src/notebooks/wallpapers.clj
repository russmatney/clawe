(ns notebooks.wallpapers
  {:nextjournal.clerk/visibility {:code :hide :result :hide}
   :nextjournal.clerk/toc true}
  (:require
   [wallpapers.core :as wallpapers]
   [nextjournal.clerk :as clerk]))

^{::clerk/no-cache   true}
(def wps
  (->>
    (wallpapers/all-wallpapers)
    (sort-by :wallpaper/used-count)
    reverse
    (take 4)
    (into [])))

(def wp-viewer
  {:transform-fn clerk/mark-presented
   :render-fn
   '(fn [wps]
      (v/html
        [:div
         [:h3 "Wallpaper Actions"]
         (for [wp wps]
           [:div
            [:span (str (:file/file-name wp))]

            [:button
             {:class    ["bg-blue-700" "hover:bg-blue-700"
                         "text-slate-300" "font-bold"
                         "py-2" "px-4" "m-1"
                         "rounded"]
              :on-click (fn [_]
                          (js/console.log "wp-clicked with " (clj->js wp))
                          (v/clerk-eval `(;; must be fully qualified ns for now
                                          wallpapers.core/set-wallpaper ~wp)))}

             "Set wallpaper"]])]))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
{:nextjournal.clerk/visibility {:result :show}}

;; # wallpapers

^{::clerk/no-cache true}
(clerk/table
  {::clerk/width :full}
  wps)

^{::clerk/viewer wp-viewer}
wps
