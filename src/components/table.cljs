(ns components.table
  (:require
   [components.debug :as components.debug]
   [components.wallpaper :as components.wallpaper]
   [tick.core :as t]
   [components.floating :as floating]
   [components.garden :as components.garden]))


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
        [:div {:class ["table-row" "py-2" "border" "border-b-2" "text-sm"
                       ]}
         (for [[j cell] (->> cells (map-indexed vector))]
           ^{:key j}
           [:div {:class ["table-cell"
                          "border-b" "border-slate-700"
                          "p-2 pl-8"
                          "text-slate-300"
                          "align-middle"]}
            cell])])]]]])

(defn garden-by-tag-table-def [entities]
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
                       (first group)]])))} )

(defn table-for-doctor-type [doctor-type entities]
  [components.table/table
   (cond
     (#{:type/garden} doctor-type)
     {:headers ["File" "Name" "Parent" "Words" "Raw"]
      :rows    (->>
                 entities
                 (sort-by :file/last-modified)
                 reverse
                 (take 10) ;; only take the top 10 tags
                 (map (fn [note]
                        [(:org/short-path note)
                         (:org/name note)
                         (:org/parent-name note)
                         [floating/popover
                          {:hover true :click true
                           :anchor-comp
                           [:span
                            {:class [(when (seq (:org/body-string note))
                                       "text-city-pink-400")]}
                            (:org/word-count note)]
                           :popover-comp
                           [:div
                            {:class
                             ["text-city-blue-400"
                              "flex" "flex-col" "p-2"
                              "bg-yo-blue-500"]}
                            (:org/name note)
                            [components.garden/org-body note]]}]
                         [components.debug/raw-metadata
                          {:label "raw"}
                          note]])))}


     (#{:type/wallpaper} doctor-type)
     {:headers ["Img" "Wallpaper" "Used count" "Last Time Set" "Raw"]
      :rows    (->> entities
                    (sort-by :wallpaper/last-time-set)
                    (reverse)
                    (take 10) ;; only take the last 10
                    (map (fn [wp]
                           [[floating/popover
                             {:hover true :click true
                              :anchor-comp
                              [:img {:src   (-> wp :file/web-asset-path)
                                     :class ["max-h-24"]}]
                              :popover-comp
                              [components.wallpaper/wallpaper-comp wp]}]
                            (-> wp :wallpaper/short-path)
                            (:wallpaper/used-count wp)
                            (-> wp :wallpaper/last-time-set (t/new-duration :millis) t/instant)

                            [components.debug/raw-metadata
                             {:label "raw"}
                             wp]])))}

     :else
     {:headers ["No headers"]
      :rows    [["No rows"]]})])
