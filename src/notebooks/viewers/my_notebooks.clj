(ns notebooks.viewers.my-notebooks
  (:require [nextjournal.clerk.viewer :as v]
            [notebooks.core :as notebooks]
            [nextjournal.clerk :as clerk]))

(def links (notebooks/notebooks))

(def viewer
  {:name :clerk/notebook ;; overwrite default notebook viewer
   :transform-fn
   (fn [{:as wrapped-value :nextjournal/keys [viewers]}]
     (-> wrapped-value
         (update :nextjournal/value (partial v/process-blocks viewers))
         (update :nextjournal/value assoc :links links)
         clerk/mark-presented))

   :render-fn
   '(fn [{:as _doc :keys [blocks links]}]
      (js/console.log js/window.location.pathname)
      (.add (.-classList (js/document.querySelector "html")) "dark")
      (v/html
        [:div.flex
         [:div.fixed.top-2.left-2.z-10
          [:span.text-slate-400.px-4.mx-2 "Notebooks"]
          [:div.flex.flex-col
           (for [{:keys [uri name]} links]
             (let [current-page? (clojure.string/includes? js/window.location.pathname uri)]
               [:div
                {:class ["text-slate-400" "px-4 mx-2"
                         (when-not current-page? "border border-slate-400 rounded")]}
                (if current-page?
                  name
                  [:a {:href uri} name])]))]]
         [:div.flex-auto.h-screen.overflow-y-auto
          [:div.flex.flex-col.items-center.viewer-notebook.flex-auto
           (doall
             (map-indexed (fn [idx x]
                            ^{:key idx}
                            [:div {:class ["viewer" "w-full max-w-prose px-8"]}
                             [v/inspect-presented x]])
                          blocks))]]]))})
