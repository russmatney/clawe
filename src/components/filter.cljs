(ns components.filter
  (:require [components.floating :as floating]
            [uix.core.alpha :as uix]
            [components.debug :as components.debug]
            [util :as util]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; filter def anchor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-def-anchor
  [[filter-key filter-def] {:keys [set-group-by items-group-by
                                   set-current-preset]}]
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
  [items [filter-key filter-def]
   {:keys [items-filter-by toggle-filter-by set-current-preset]}]
  (let [grouped-by-val-and-counts
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

           (for [[i [k v]] (->> group (sort-by first >) (map-indexed vector))]
             (let [filter-enabled? (items-filter-by {:filter-key filter-key :match k})]
               [:div
                {:key      i
                 :class    ["flex" "flex-row" "font-mono"
                            "cursor-pointer"
                            "hover:text-city-red-600"
                            (when filter-enabled?
                              "text-city-pink-400")]
                 :on-click (fn [_]
                             (set-current-preset nil)
                             (toggle-filter-by {:filter-key filter-key :match k}))}
                (let [format-label (:format-label filter-def str)]
                  [:span.p-1.pl-2.text-xl.ml-auto (format-label k)])
                [:span.p-1.text-xl.w-10.text-center v]]))])])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; preset-filters
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn preset-filters [{:keys [preset-filter-groups
                              current-preset
                              set-filters
                              set-current-preset
                              set-group-by]}]
  ;; TODO floating/popover
  [floating/popover
   {:hover true :click true
    :anchor-comp
    [:span {:class ["font-nes"]}
     "Presets"]

    :popover-comp
    [:div
     {:class ["flex" "flex-col"
              "bg-yo-blue-800"
              "p-4"]}
     (for [[k {:keys [filters group-by]}]
           preset-filter-groups]
       ^{:key k}
       [:div
        {:class ["font-mono"
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
        [:span k]])]}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; filter-grouper full component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; let's not forget this wants to be a filter-grouper-sorter
(defn- filter-grouper
  "A component for displaying and selecting filters/groups.

  Returned as part of `use-filter`."
  [items {:keys [all-filter-defs
                 items-filter-by
                 items-group-by]
          :as   filter-grouper-config}]
  [:div.flex.flex-col
   [preset-filters filter-grouper-config]

   [:div.flex.flex-row.flex-wrap
    {:class ["gap-x-3"]}

    (for [[i [filter-key filter-def]] (map-indexed vector all-filter-defs)]
      ^{:key i}
      [floating/popover
       {:hover        true :click true
        :anchor-comp  [filter-def-anchor [filter-key filter-def] filter-grouper-config]
        :popover-comp [filter-def-popover items [filter-key filter-def] filter-grouper-config]}])]

   ;; TODO improve debug view for collections
   [components.debug/raw-metadata {:label "[raw filters]"} items-filter-by]
   [components.debug/raw-metadata {:label "[raw group-by]"} {:val items-group-by}]

   ;; TODO create applied filters component for saving/revisiting saved filters
   (for [[i f] (->> items-filter-by (map-indexed vector))]
     ^{:key i} [:div [:pre (str f)]])
   [:div [:pre (str ":group-by " items-group-by)]]])


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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; use-filter
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO write a set of unit tests around this
(defn use-filter
  [{:keys [items
           default-filters default-group-by
           preset-filter-groups
           all-filter-defs]}]
  ;; TODO malli schema+validation for all-filter-defs and default-filters
  ;; TODO local storage read/write for each filter-grouper (are they serializable?)
  (let [default-filters  (or default-filters
                             (some-> preset-filter-groups :default :filters))
        default-group-by (or default-group-by
                             (some-> preset-filter-groups :default :group-by)
                             (some-> all-filter-defs first first))

        items-filter-by (uix/state default-filters)
        items-group-by  (uix/state default-group-by)
        current-preset  (uix/state :default)

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
      {:all-filter-defs      all-filter-defs
       :preset-filter-groups preset-filter-groups
       :set-current-preset   #(reset! current-preset %)
       :current-preset       @current-preset
       :set-group-by         #(reset! items-group-by %)
       :set-filters          #(reset! items-filter-by %)
       :toggle-filter-by
       (fn [f-by]
         ;; TODO filters that use funcs won't match/exclude here
         (swap! items-filter-by #(if (% f-by) (disj % f-by) (conj % f-by))))
       :items-filter-by      @items-filter-by
       :items-group-by       @items-group-by}]
     :filtered-items       filtered-items
     :filtered-item-groups filtered-item-groups
     :items-group-by       @items-group-by
     :items-filter-by      @items-filter-by}))
