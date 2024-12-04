(ns components.item
  (:require
   [clojure.string :as string]
   [uix.core :as uix :refer [$ defui]]

   [components.colors :as colors]
   [doctor.ui.handlers :as handlers]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; item-id-hash

(defui id-hash
  [{:keys [item]}]
  ($ :div
     {:class
      (concat
        ["flex" "text-sm" "font-nes"
         "text-city-red-300" "ml-2"
         "hover:opacity-100"]
        (when-not (:org/id item)
          ["opacity-50" "cursor-pointer" "tooltip"]))
      :on-click (fn [_] (when-not (:org/id item)
                          (handlers/ensure-uuid (dissoc item :actions/inferred))))}
     (if (:org/id item)
       (->> item :org/id str (take 4) (apply str))
       ($ :span
          ($ :span {:class ["tooltip-text" "-mt-12" "-ml-12"]}
             "ensure-uuid")
          "####"))))

(defui db-id
  [{:keys [item]}]
  (when (:db/id item)
    ($ :div
       {:class
        ["flex" "text-sm" "font-nes" "ml-2"
         "text-city-green-300"
         "hover:text-slate-400"
         "hover:line-through"
         "cursor-pointer" "tooltip"]
        :on-click #(handlers/delete-from-db (dissoc item :actions/inferred))}
       ($ :span {:class ["tooltip-text" "-mt-12" "-ml-12"]}
          "delete-from-db")
       (->> item :db/id str (apply str)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; parent-names

(defui breadcrumbs [{:keys [breadcrumbs]}]
  ($ :span
     {:class ["flex" "flex-row" "flex-wrap"]}
     (->>
       breadcrumbs
       reverse
       (map-indexed
         (fn [i nm]
           ($ :span {:class
                     (concat
                       (colors/color-wheel-classes {:type :line :i i})
                       ["flex-grow"])
                     :key i}
              " " nm)))
       (interpose ($ :span
                     {:class ["text-city-blue-dark-200" "px-4"]}
                     " > "))
       ;; kind of garbage, but :shrug:
       (map-indexed (fn [i sp]
                      ($ :span {:key i} sp))))))

(defui parent-names
  [{:keys [header? n item]}]
  (let [p-names      (-> item :org/parent-name (string/split #" > "))
        p-names      (cond->> p-names
                       n (take n))
        p-name       (-> p-names first)
        rest-p-names (-> p-names rest)]
    (if-not header?
      ($ breadcrumbs {:breadcrumbs p-names})
      ($ :div
         {:class ["flex" "flex-row" "flex-wrap" "items-center"]}
         ($ breadcrumbs rest-p-names)
         ($ :span
            {:class ["text-city-blue-dark-200" "px-4"]}
            " > ")
         ($ :span
            {:class ["font-nes" "text-xl" "p-3"
                     "text-city-green-400"
                     "whitespace-nowrap"
                     ]}
            p-name)))))
