(ns components.filter
  (:require
   [components.floating :as floating]
   [uix.core.alpha :as uix]
   [util :as util]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; filter def anchor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-def-anchor
  [[filter-key filter-def]
   {:keys [set-group-by items-group-by set-current-preset]}]
  (let [group-by-enabled? (= items-group-by filter-key)]
    [:div.text-xl.font-nes
     {:class    ["cursor-pointer"
                 "hover:text-city-red-600"
                 (when group-by-enabled? "text-city-pink-400")]
      :on-click (fn [_]
                  (set-current-preset nil)
                  (set-group-by filter-key))}
     (:label filter-def)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; filter def popover
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-def-popover
  [[filter-key filter-def]
   {:keys [items _filtered-items items-filter-by toggle-filter-by set-current-preset]}]
  (let [grouped-by-val-and-counts
        ;; TODO items vs filtered-items here needs to be toggleable
        ;; are we narrowing or widening?
        (->> items
             (group-by (:group-by filter-def))
             util/expand-coll-group-bys
             (map (fn [[v xs]] [v (count xs)])))]
    [:div
     {:class ["bg-city-blue-800"
              "text-city-pink-200"
              "py-2" "px-4"]}

     ;; custom filter-opt selection
     (when (seq (:filter-options filter-def))
       [:div
        (for [[i filter-option] (->> filter-def :filter-options (map-indexed vector))]
          (let [filter-enabled? (items-filter-by (assoc filter-option :filter-key filter-key))]
            [:div
             {:key      i
              :on-click (fn [_]
                          (set-current-preset nil)
                          (toggle-filter-by (assoc filter-option :filter-key filter-key)))
              :class    ["flex" "flex-row" "font-mono"
                         "cursor-pointer"
                         "hover:text-city-red-600"
                         (when filter-enabled? "text-city-pink-400")]}
             (:label filter-option)]))])

     (let [group-filters-by (:group-filters-by filter-def (fn [_] nil))]
       [:div
        {:class ["flex" "flex-row" "flex-wrap" "gap-x-4"]}
        (for [[i [group-label group]] (->> grouped-by-val-and-counts
                                           (group-by (comp group-filters-by first))
                                           (sort-by first)
                                           (map-indexed vector))]
          [:div
           {:key   i
            :class ["flex" "flex-col"]}

           (when group-label
             [:div
              {:class ["font-nes" "mx-auto"]}
              group-label])

           ;; these counts don't include applied filters
           (for [[i [k ct]] (->> group (sort-by first >) (map-indexed vector))]
             (let [filter-desc     (if (nil? k)
                                     {:filter-key filter-key :match-fn nil?}
                                     {:filter-key filter-key :match k})
                   ;; TODO extend to match on :match-str-includes-any (and other match extensions)
                   filter-enabled? (items-filter-by filter-desc)]
               [:div
                {:key      i
                 :class    ["flex" "flex-row" "font-mono"
                            "cursor-pointer"
                            "hover:text-city-red-600"
                            (when filter-enabled?
                              "text-city-pink-400")]
                 :on-click (fn [_]
                             (set-current-preset nil)
                             (toggle-filter-by filter-desc))}
                (let [format-label (:format-label filter-def (fn [k]
                                                               (if (nil? k) "None" (str k))))]
                  [:span.p-1.pl-2.text-xl.ml-auto (format-label k)])
                [:span.p-1.text-xl.w-10.text-center (str "(" ct ")")]]))])])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; preset-filters
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn preset-filters [{:keys [presets
                              current-preset
                              set-filters
                              set-current-preset
                              set-group-by]}]
  [:div
   {:class ["flex" "flex-col"]}
   [:span {:class ["font-nes" "text-xl"
                   "cursor-pointer"
                   "pb-2"]}
    "Presets"]

   [:div
    {:class ["flex" "flex-row" "ml-auto" "flex-wrap"
             "gap-1"]}
    (for [[k {:keys [filters group-by label]}]
          (->> presets (sort-by first))]
      ^{:key k}
      [:div
       {:class ["bg-yo-blue-800"
                "rounded-xl"
                "px-3" "py-2"
                "text-sm"
                "font-mono"
                "cursor-pointer"
                (if (#{current-preset} k)
                  "text-city-pink-400"
                  "text-city-green-600")
                "hover:text-city-red-600"]
        :on-click
        (fn [_]
          (set-current-preset k)
          (set-filters filters)
          (set-group-by group-by))}
       [:span (or label k)]])]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; filter-grouper full component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; let's not forget this wants to be a filter-grouper-sorter
(defn- filter-grouper
  "A component for displaying and selecting filters/groups.

  Returned as part of `use-filter`."
  [{:keys [all-filter-defs
           items-filter-by
           items-group-by
           show-filters-inline]
    :as   config}]
  (let [filter-detail-open? (uix/state false)]
    [:div
     {:class ["flex flex-col"]}

     [:div
      {:class ["pb-3" "flex" "flex-row"]}
      [preset-filters config]

      [:div
       [:button {:on-click #(swap! filter-detail-open? not)
                 :class    ["whitespace-nowrap"]
                 }
        (str (if @filter-detail-open? "Hide" "Show") " filter detail")]]]

     ;; active group-by
     [:div [:pre (str ":group-by " items-group-by)]]

     (when @filter-detail-open?
       ;; active filters
       (for [[i f] (->> items-filter-by (map-indexed vector))]
         ^{:key i} [:div
                    {:class ["font-mono"]}
                    (str f)]))

     (when @filter-detail-open?
       ;; edit filters
       [:div
        {:class ["flex flex-row"
                 "flex-wrap"
                 "pt-2"
                 "gap-x-4"]}

        (for [[i [filter-key filter-def]] (map-indexed vector all-filter-defs)]
          (if show-filters-inline
            ^{:key i}
            [:div
             {:class ["flex" "flex-col"
                      "items-center"
                      "grow"
                      "m-2"
                      "p-2"
                      "border-8"
                      "rounded-lg"
                      "border-city-blue-900"
                      "hover:border-city-orange-500"
                      "cursor-pointer"]}
             [:div
              [filter-def-anchor [filter-key filter-def] config]]

             ;; active filters
             (for [[i f] (->> items-filter-by
                              (filter (comp #{filter-key} :filter-key))
                              (map-indexed vector))]
               ^{:key i} [:div
                          {:class ["font-mono"]}
                          (str f)])

             ;; active group-by
             [:div
              [:pre ":group-by " items-group-by]]

             ;; fill to hold bottom in 'middle'
             [:div {:class ["grow"]}]
             [:div {:class ["grow"]}
              [filter-def-popover [filter-key filter-def] config]]]

            ^{:key i}
            [floating/popover
             {:hover        true :click true
              :anchor-comp  [filter-def-anchor [filter-key filter-def] config]
              :popover-comp [filter-def-popover [filter-key filter-def] config]}]))])]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; filtering logic
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- filter-match-fn
  "Builds a predicate for an item, for a grouped filter-key.

  Multiple filter-defs per key can be 'on' at a time
  (i.e. filtering by more than one tag or source-file),
  so here we build a predicate that returns true if any of the passed 'filter-defs' match.

  The matches can be exact or predicates themselves.

  The filter-key's group-by can return collections - in that case, a match on any elem
  is a match for the whole filter-key."
  [all-filter-defs [filter-key filter-defs]]
  (let [->value              (-> filter-key all-filter-defs :group-by)
        exact-matches        (->> filter-defs (map :match) (into #{}))
        ;; consider removing match-fns that can't be called
        pred-matches         (->> filter-defs (map :match-fn) (remove nil?))
        includes-any-matches (->> filter-defs (map :match-str-includes-any) (remove nil?)
                                  (map (fn [strs]
                                         ;; return a predicate for each set of strings
                                         (fn [val]
                                           (->> strs (filter #(string/includes? val %)) seq)))))
        is-match             (->> [exact-matches]
                                  (filter seq)
                                  (concat pred-matches includes-any-matches)
                                  (apply some-fn))]
    (fn [raw]
      (-> raw ->value
          ((fn [val]
             (if (coll? val)
               (->> val (filter is-match) seq)
               (is-match val))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; use-filter
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn use-filter
  [{:keys [items all-filter-defs] :as config}]
  ;; TODO cut off this default usage with local storage read/write for last-set preset
  (let [[d-key default]  (or (some->> config :presets (filter (comp :default second)) first)
                             (some->> config :presets (filter (comp #{:default} first)) first))
        default-filters  (or (some-> default :filters) #{})
        default-group-by (or (some-> default :group-by)
                             (some-> all-filter-defs first first))

        items-filter-by (uix/state default-filters)
        items-group-by  (uix/state default-group-by)
        current-preset  (uix/state d-key)

        filtered-items
        (if-not (seq @items-filter-by) items
                (->> items (filter
                             (apply every-pred
                                    ;; every predicate must match
                                    (->> @items-filter-by
                                         (group-by :filter-key)
                                         (map (partial
                                                filter-match-fn all-filter-defs)))))))

        filtered-item-groups
        (->> filtered-items
             (group-by (some-> @items-group-by all-filter-defs :group-by))
             util/expand-coll-group-bys
             (map (fn [[label its]] {:item-group its :label label})))]

    {:filter-grouper
     [filter-grouper
      (-> config
          (merge
            {:filtered-items     filtered-items
             :current-preset     @current-preset
             :items-filter-by    @items-filter-by
             :items-group-by     @items-group-by
             :set-current-preset #(reset! current-preset %)
             :set-group-by       #(reset! items-group-by %)
             :set-filters        #(reset! items-filter-by %)
             :toggle-filter-by
             (fn [f-by]
               ;; TODO filters that use funcs won't match/exclude here
               (swap! items-filter-by #(if (% f-by) (disj % f-by) (conj % f-by))))}))]
     :filtered-items       filtered-items
     :filtered-item-groups filtered-item-groups
     :items-group-by       @items-group-by
     :items-filter-by      @items-filter-by}))
