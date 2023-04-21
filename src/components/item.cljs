(ns components.item
  (:require
   [doctor.ui.handlers :as handlers]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; item-id-hash

(defn id-hash [it]
  [:div
   {:class
    (concat
      ["flex" "text-sm" "font-nes"
       "text-city-red-300" "ml-2"
       "hover:opacity-100"]
      (when-not (:org/id it)
        ["opacity-50" "cursor-pointer" "tooltip"]))
    :on-click (fn [_] (when-not (:org/id it)
                        (handlers/ensure-uuid it)))}
   (if (:org/id it)
     (->> it :org/id str (take 4) (apply str))
     [:span
      [:span {:class ["tooltip-text" "-mt-12" "-ml-12"]}
       "ensure-uuid"]
      "####"])])

(defn db-id [it]
  (when (:db/id it)
    [:div
     {:class
      ["flex" "text-sm" "font-nes" "ml-2"
       "text-city-green-300"
       "hover:text-slate-400"
       "hover:line-through"
       "cursor-pointer" "tooltip"]
      :on-click #(handlers/delete-from-db it)}
     [:span {:class ["tooltip-text" "-mt-12" "-ml-12"]}
      "delete-from-db"]
     (->> it :db/id str (apply str))]))
