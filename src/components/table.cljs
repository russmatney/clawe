(ns components.table
  (:require
   [uix.core :as uix :refer [$ defui]]
   [components.actions :as components.actions]))


(defui table
  "A basic table component.

  `:headers` is a list of header-comps (or strings).
  `:rows` is a list-of cells, which are lists of comps or strings.

  No sorting is done in this component - the vals/headers should be ordered properly
  before being passed in."
  [{:keys [headers rows n]}]

  (let [[page-size set-page-size] (uix/use-state (or n 10))
        step                      5]
    ($ :div
       {:class ["border border-slate-600 bg-slate-800/50 rounded-xl overflow-auto"
                "w-full" "my-4" "py-4"]}

       ($ :div
          {:class ["table" "table-auto" "w-full"]}

          ($ :div {:class ["table-header-group"]}
             ($ :div {:class ["table-row"]}
                (for [header-label headers]
                  ($ :div
                     {:key   header-label
                      :class ["table-cell" "text-left"
                              "border-b" "border-slate-600"
                              "p-4" "pl-8" "pt-0" "pb-3"
                              "font-medium"
                              "text-slate-200"]}
                     header-label))))

          ($ :div {:class ["table-row-group" "bg-slate-800"]}
             (for [[i cells] (->> rows
                                  (take page-size)
                                  (map-indexed vector))]

               ($ :div {:class ["table-row" "py-2" "border" "border-b-2" "text-sm"]
                        :key   i}
                  (for [[j cell] (->> cells (map-indexed vector))]
                    ($ :div {:class ["table-cell"
                                     "border-b" "border-slate-700"
                                     "p-2 pl-8"
                                     "text-slate-300"
                                     "align-middle"]
                             :key   j}
                       cell)))))

          ($ :div {:class ["table-footer-group"]}
             (let [axs
                   (->>
                     [(when (> page-size step)
                        {:action/label    "show less"
                         :action/on-click (fn [_] (set-page-size #(- % step)))})
                      (when (< page-size (count rows))
                        {:action/label    "show more"
                         :action/on-click (fn [_] (set-page-size #(+ % step)))})
                      (when (< page-size (count rows))
                        {:action/label    (str "show all (" (count rows) ")")
                         :action/on-click (fn [_] (set-page-size (count rows)))})]
                     (remove nil?))]
               (when (seq axs)
                 ($ :div {:class ["table-row" "w-full"]}
                    ($ components.actions/actions-list
                       {:actions axs})))))))))
