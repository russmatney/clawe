(ns components.table
  (:require [components.debug :as components.debug]))


(defn table
  "A basic table component.

  `:headers` is a list of header-comps (or strings).
  `:rows` is a list-of cells, which are lists of comps or strings.

  No sorting is done in this component - the vals/headers should be ordered properly
  before being passed in."
  [{:keys [headers rows]}]

  [:div
   {:class ["border border-slate-600 bg-slate-800/50 rounded-xl overflow-auto"]}
   [:div
    {:class ["my-8"]}

    [:div
     {:class ["table" "table-auto" "w-full"]}

     [:div {:class ["table-header-group"]}
      [:div {:class ["table-row"]}
       (for [header-label headers]
         [:div
          {:key   header-label
           :class ["table-cell" "text-left"
                   "border-b" "border-slate-600"
                   "p-4" "pl-8" "pt-0" "pb-3"
                   "font-medium"
                   "text-slate-200"]}
          header-label])]]

     [:div {:class ["table-row-group" "bg-slate-800"]}
      (for [[i cells] (->> rows (map-indexed vector))]
        ^{:key i}
        [:div {:class ["table-row" "py-2" "border" "border-b-2"]}
         (for [[j cell] (->> cells (map-indexed vector))]
           ^{:key j}
           [:div {:class ["table-cell"
                          "border-b" "border-slate-700"
                          "p4 pl-8"
                          "text-slate-300"
                          "text-sm"]}
            cell])])]]]])

(defn table-for-doctor-type [doctor-type entities]
  [components.table/table
   (cond
     (#{:type/garden} doctor-type)
     {:headers ["Tag" "Count" "Example"]
      :rows    (->>
                 entities
                 (group-by :org/tags)
                 (sort-by (comp count second))
                 reverse
                 (take 10) ;; only take the top 10 tags
                 (map (fn [[tag group]]

                        [[:span
                          (or tag "(no tag)")]
                         [:span
                          {:class ["font-nes"]}
                          (count group)]
                         [components.debug/raw-metadata
                          {:label (-> group first :org/name)}
                          (first group)]])))}

     (#{:type/wallpaper} doctor-type)
     {:headers ["Wallpaper" "Used count" "Last Time Set" "Raw"]
      :rows    (->> entities
                    (map (fn [wp]
                           [(:file/file-name wp)
                            (:wallpaper/used-count wp)
                            (:wallpaper/last-time-set wp)
                            [components.debug/raw-metadata
                             {:label "raw"}
                             wp]])))}

     :else
     {:headers ["No headers"]
      :rows    [["No rows"]]})])
