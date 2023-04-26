(ns components.item
  (:require
   [doctor.ui.handlers :as handlers]
   [components.colors :as colors]
   [clojure.string :as string]))


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
                        (handlers/ensure-uuid (dissoc it :actions/inferred))))}
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
      :on-click #(handlers/delete-from-db (dissoc it :actions/inferred))}
     [:span {:class ["tooltip-text" "-mt-12" "-ml-12"]}
      "delete-from-db"]
     (->> it :db/id str (apply str))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; parent-names

(defn breadcrumbs [bcrumbs]
  [:span
   {:class ["flex" "flex-row" "flex-wrap"]}
   (->>
     bcrumbs
     reverse
     (map-indexed
       (fn [i nm]
         [:span {:class
                 (concat
                   (colors/color-wheel-classes {:type :line :i i})
                   ["flex-grow"])}
          " " nm]))
     (interpose [:span
                 {:class ["text-city-blue-dark-200" "px-4"]}
                 " > "])
     ;; kind of garbage, but :shrug:
     (map-indexed (fn [i sp] ^{:key i}
                    [:span sp])))])

(defn parent-names
  ([it] (parent-names nil it))
  ([{:keys [header? n]} it]
   (let [p-names      (-> it :org/parent-name (string/split #" > "))
         p-names      (cond->> p-names
                        n (take n))
         p-name       (-> p-names first)
         rest-p-names (-> p-names rest)]
     (if-not header?
       [breadcrumbs p-names]
       [:div
        {:class ["flex" "flex-row" "flex-wrap" "items-center"]}
        [breadcrumbs rest-p-names]
        [:span
         {:class ["text-city-blue-dark-200" "px-4"]}
         " > "]
        [:span
         {:class ["font-nes" "text-xl" "p-3"
                  "text-city-green-400"
                  "whitespace-nowrap"
                  ]}
         p-name]]))))
