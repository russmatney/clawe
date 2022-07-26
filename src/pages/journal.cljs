(ns pages.journal
  (:require
   [hooks.journals :as hooks.journals]
   [components.garden :as components.garden]))


(defn page [_opts]
  (let [{:keys [items]} (hooks.journals/use-journals)]
    [:div
     "Journals"

     (for [[i journal] (map-indexed vector items)]
       [:div
        {:key   i
         :class [""]}

        [components.garden/org-file journal]])]))
