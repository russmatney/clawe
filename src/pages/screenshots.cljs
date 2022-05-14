(ns pages.screenshots
  (:require
   [hooks.screenshots]
   [components.screenshot :as screenshot]))

(defn page []
  (let [{:keys [items]} (hooks.screenshots/use-screenshots)]
    [:div
     {:class ["flex" "flex-row" "flex-wrap" "flex-auto"
              "min-h-screen"
              "overflow-hidden"
              "bg-yo-blue-700"
              ]}
     (for [[i it] (->> items (map-indexed vector))]
       ^{:key i}
       [screenshot/screenshot-comp nil it])]))
