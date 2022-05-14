(ns pages.counts
  (:require
   ;; [uix.core.alpha :as uix]
   [hooks.count]
   [components.count]))

(defn page [_opts]
  (let [{:keys [items _count]} (hooks.count/use-counts)]
    ;; TODO think about a :count/break or hydra-ish api for splitting here
    ;; TODO pull tags into suggestions for hide/show filters
    [:div
     {:class ["flex" "flex-row" "flex-wrap"
              "justify-center"
              "overflow-hidden"]}

     (for [[i it] (->> items (map-indexed vector))]
       ^{:key i}
       [components.count/count-comp nil (assoc it :index i)])]))
