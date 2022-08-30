(ns components.filter
  (:require [components.floating :as floating]
            [uix.core.alpha :as uix]
            [components.debug :as components.debug]
            [util :as util]))

(defn- filter-grouper
  "A component for displaying and selecting filters/groups.

  Returned as part of `use-filter`."
  [items {:keys [all-filter-defs
                 set-group-by toggle-filter-by
                 items-group-by items-filter-by]}]
  [:div.flex.flex-row.flex-wrap
   {:class ["gap-x-3"]}

   ;; TODO break this up more
   (for [[i [filter-key filter-def]] (map-indexed vector all-filter-defs)]
     (let [grouped-by-counts
           (->> items
                (group-by (:group-by filter-def))
                util/expand-coll-group-bys
                (map (fn [[v xs]] [v (count xs)])))

           group-by-enabled? (= items-group-by filter-key)]
       [:div
        {:key i}

        [floating/popover
         {:hover true :click true
          :anchor-comp
          [:div.text-xl.font-nes
           {:class    ["cursor-pointer"
                       "hover:text-city-red-600"
                       (when group-by-enabled? "text-city-pink-400")]
            :on-click #(set-group-by filter-key)}
           (:label filter-def)]
          :popover-comp
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
                    :on-click #(toggle-filter-by (assoc filter-option :filter-key filter-key))
                    :class    ["flex" "flex-row" "font-mono"
                               "cursor-pointer"
                               "hover:text-city-red-600"
                               (when filter-enabled? "text-city-pink-400")]}
                   (:label filter-option)]))])

           (let [group-filters-by (:group-filters-by filter-def (fn [_] nil))]
             [:div
              {:class ["flex" "flex-row" "flex-wrap" "gap-x-4"]}
              (for [[i [group-label group]] (->> grouped-by-counts
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

                 (for [[i [k v]] (->> group (sort-by first >) (map-indexed vector))]
                   (let [filter-enabled? (items-filter-by {:filter-key filter-key :match k})]
                     [:div
                      {:key      i
                       :class    ["flex" "flex-row" "font-mono"
                                  "cursor-pointer"
                                  "hover:text-city-red-600"
                                  (when filter-enabled?
                                    "text-city-pink-400")]
                       :on-click #(toggle-filter-by {:filter-key filter-key :match k})}
                      (let [format-label (:format-label filter-def str)]
                        [:span.p-1.pl-2.text-xl.ml-auto (format-label k)])
                      [:span.p-1.text-xl.w-10.text-center v]]))])])]}]]))

   ;; TODO improve debug view for collections
   [components.debug/raw-metadata {:label "filters"} items-filter-by]

   ;; TODO create applied filters component for saving/revisiting saved filters
   (for [[i f] (->> items-filter-by
                    (map-indexed vector))]
     ^{:key i}
     [:div [:pre (str f)]])])

(defn- filter-match-fn
  "Builds a predicate for an item, for a grouped filter-key.

  Multiple filter-defs per key can be 'on' at a time
  (i.e. filtering by more than one tag or source-file),
  so here we build a predicate that returns true if any of the passed 'filter-defs' match.

  The matches can be exact or predicates themselves.

  The filter-key's group-by can return collections - in that case, a match on any elem
  is a match for the whole filter-key."
  [all-filter-defs [filter-key filter-defs]]
  (let [->value       (-> filter-key all-filter-defs :group-by)
        exact-matches (->> filter-defs (map :match) (into #{}))
        ;; TODO consider removing match-fns that can't be called
        pred-matches  (->> filter-defs (map :match-fn) (remove nil?))
        is-match      (->> [exact-matches]
                           (filter seq)
                           (concat pred-matches)
                           ((fn [fns]
                              (apply some-fn fns))))]
    (fn [raw]
      (-> raw ->value
          ((fn [val]
             (if (coll? val)
               (->> val (filter is-match) seq)
               (is-match val))))))))

;; TODO write a set of unit tests around this
(defn use-filter [{:keys [items default-filters default-group-by all-filter-defs]}]
  ;; TODO malli schema+validation for all-filter-defs and default-filters
  (let [items-group-by  (uix/state
                          (or default-group-by
                              (some->> all-filter-defs first first)))
        items-filter-by (uix/state default-filters)

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
     [filter-grouper items
      {:all-filter-defs all-filter-defs
       :set-group-by    #(reset! items-group-by %)
       :toggle-filter-by
       (fn [f-by]
         ;; TODO filters that use funcs won't match/exclude here
         (swap! items-filter-by #(if (% f-by) (disj % f-by) (conj % f-by))))
       :items-filter-by @items-filter-by
       :items-group-by  @items-group-by}]
     :filtered-items       filtered-items
     :filtered-item-groups filtered-item-groups}))
