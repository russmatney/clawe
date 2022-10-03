(ns notebooks.org-daily
  {:nextjournal.clerk/visibility {:code :hide :result :hide}
   :nextjournal.clerk/toc        true}
  (:require
   [garden.core :as garden]
   [org-crud.core :as org-crud]
   [org-crud.markdown :as org-crud.markdown]
   [nextjournal.clerk :as clerk]
   [notebooks.nav :as nav]
   [nextjournal.clerk.viewer :as clerk-viewer]

   [components.debug :as components.debug]
   [clojure.string :as string]))

^{::clerk/no-cache true}
(def todays-org-item
  (-> (garden/daily-path) org-crud/path->nested-item))

(def item-viewer
  {:transform-fn
   (fn [wrapped-value]
     (let [val (clerk-viewer/->value wrapped-value)]
       (clerk-viewer/html
         [:div
          {:class ["mt-auto" "p-4" "bg-slate-300"
                   "border"
                   "border-blue-800"]}
          #_(components.debug/raw-metadata val)
          (components.debug/colorized-metadata val)])))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
{::clerk/visibility {:result :show}}

^{::clerk/no-cache true}
(clerk/with-viewer
  item-viewer
  (->
    todays-org-item
    (dissoc
      ;; :org/body
      ;; :org/body-string
      :org/items
      )))

(clerk/md
  (->>
    (org-crud.markdown/item->md-body todays-org-item)
    (string/join "\n")))

(clerk/md
  (nav/notebook-links))
