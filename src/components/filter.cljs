(ns components.filter
  (:require
   [components.floating :as floating]
   [uix.core.alpha :as uix]
   [util :as util]
   [clojure.string :as string]
   [components.pill :as pill]

   [components.filter-defs :as filter-defs]
   ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; grouped filter items component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn group->comp
  [{:keys [item-group label item->comp filter-data
           filter-items sort-items]}]
  (let [{:keys [items-group-by]} filter-data
        item-group-open?         (uix/state false)]
    ;; item group
    [:div
     {:class ["flex" "flex-col"]}
     [:div
      [:hr {:class ["mt-6" "border-city-blue-900"]}]
      [:div
       {:class ["p-6" "flex flex-row"]}
       ;; TODO support these label fallbacks via filter-defs
       (cond
         (#{:priority} items-group-by)
         (if label label
             [:span
              {:class ["font-nes" "text-city-blue-400"]}
              "No Priority"])

         (#{:tags} items-group-by)
         (if label label
             [:span
              {:class ["font-nes" "text-city-blue-400"]}
              "No tags"])

         (#{:short-path} items-group-by)
         [:span
          {:class ["font-nes" "text-city-blue-400"]}
          [filter-defs/path->basename label]]

         :else
         [:span
          {:class ["font-nes" "text-city-blue-400"]}
          (or
            ;; TODO parse this label to plain string with org-crud
            (str label) "None")])

       [:div
        {:class ["ml-auto"  "text-city-blue-400"]}
        [:button {:on-click #(swap! item-group-open? not)
                  :class    ["whitespace-nowrap"]}
         (str (if @item-group-open? "Hide" "Show")
              " " (count item-group) " item(s)")]]]]

     (when @item-group-open?
       [:div
        {:class ["flex" "flex-row" "flex-wrap" "justify-around"]}

        (let [items (cond->> item-group
                      filter-items filter-items
                      sort-items   sort-items
                      true         (map-indexed vector))]
          (for [[i it] items]
            ^{:key (str (:org/name it) i)}
            [item->comp it]))])]))

(defn items-by-group [{:keys [item->comp] :as filter-data}]
  [:div
   (for [[i group-desc]
         (->> (:filtered-item-groups filter-data)
              (map-indexed vector))]
     ^{:key (:label group-desc i)}
     [group->comp (assoc group-desc
                         :item->comp item->comp
                         :filter-data filter-data)])])

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
;; filter-grouper full component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; let's not forget this wants to be a filter-grouper-sorter
(defn- filter-grouper
  "A component for displaying and selecting filters/groups.

  Returned as part of `use-filter`."
  [{:keys [all-filter-defs
           items-filter-by
           items-group-by
           sort-groups-key
           show-filters-inline
           presets
           extra-preset-pills
           current-preset
           set-filters
           set-current-preset
           set-group-by]
    :as   config}]
  (let [filter-detail-open? (uix/state false)]
    [:div
     {:class ["flex flex-col"]}

     [:div
      {:class ["pb-3" "flex" "flex-row"]}
      [:div
       {:class ["flex" "flex-col"]}
       [:div
        {:class ["flex" "flex-row"]}
        [:span {:class ["font-nes" "text-xl"
                        "cursor-pointer"
                        "pb-2"]}
         "Presets"]

        [:div
         {:class ["ml-auto"]}
         [:button {:on-click #(swap! filter-detail-open? not)
                   :class    ["whitespace-nowrap"]}
          (str (if @filter-detail-open? "Hide" "Show") " filter detail")]]]

       [pill/cluster
        (->> presets
             (sort-by first)
             (map
               (fn [[k {:keys [filters group-by label]}]]
                 {:label  (or label k)
                  :active (#{current-preset} k)
                  :on-click
                  (fn [_]
                    (set-current-preset k)
                    (set-filters filters)
                    (set-group-by group-by))}))
             (concat (or extra-preset-pills [])))]]]

     ;; active group-by
     [:div [:pre (str ":group-by-key " items-group-by)]]

     ;; active sort-groups-key
     [:div
      [:pre ":sort-groups-key " sort-groups-key]]

     ;; active filters
     [:div
      [:pre ":active-filters "]
      (for [[i f] (->> items-filter-by (map-indexed vector))]
        ^{:key i} [:div
                   {:class ["font-mono"]}
                   (str f)])]

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

(defn label->comparable-int [p]
  (cond
    (and p (string? p)) (.charCodeAt p)
    (int? p)            p
    (keyword? p)        (label->comparable-int (name p))

    ;; TODO compare dates

    ;; some high val
    :else 1000))


(defn use-filter
  [{:keys [items all-filter-defs] :as config}]
  ;; TODO cut off this default usage with local storage read/write for last-set preset
  (let [[d-key default]
        (or
          ;; first preset with {:default true} in desc
          (some->> config :presets (filter (comp :default second)) first)
          ;; preset with :default as key
          (some->> config :presets (filter (comp #{:default} first)) first))
        initial-filters         (or (some-> default :filters) #{})
        initial-group-by-key    (or (some-> default :group-by)
                                    (some-> all-filter-defs first first))
        initial-sort-groups-key (some-> default :sort-groups)

        active-filters  (uix/state initial-filters)
        group-by-key    (uix/state initial-group-by-key)
        sort-groups-key (uix/state initial-sort-groups-key)
        current-preset  (uix/state d-key)

        filtered-items
        (if-not (seq @active-filters) items
                (->> items (filter
                             (apply every-pred
                                    ;; every predicate must match
                                    (->> @active-filters
                                         (group-by :filter-key)
                                         (map (partial
                                                filter-match-fn all-filter-defs)))))))

        ;; TODO support sorting items, both here and at the group level
        filtered-items filtered-items

        group-by-f (or (some-> @group-by-key all-filter-defs :group-by)
                       (fn [_]
                         (println "WARN: no group-by-f could be determined")
                         :default))

        filtered-item-groups
        (->> filtered-items
             (group-by group-by-f)
             util/expand-coll-group-bys
             (map (fn [[label its]] {:item-group its :label label})))

        sort-groups-f        (some-> @sort-groups-key all-filter-defs :sort-groups-fn)
        filtered-item-groups (if sort-groups-f
                               (sort-groups-f filtered-item-groups)
                               (sort-by (comp label->comparable-int :label) < filtered-item-groups))]

    {:filter-grouper
     [filter-grouper
      (-> config
          (merge
            {:filtered-items      filtered-items
             :current-preset      @current-preset
             :items-filter-by     @active-filters
             :items-group-by      @group-by-key
             :sort-groups-key     @sort-groups-key
             :set-current-preset  #(reset! current-preset %)
             :set-group-by        #(reset! group-by-key %)
             :set-sort-groups-key #(reset! sort-groups-key %)
             :set-filters         #(reset! active-filters %)
             :toggle-filter-by
             (fn [f-by]
               ;; TODO filters that use funcs won't match/exclude here
               (swap! active-filters #(if (% f-by) (disj % f-by) (conj % f-by))))}))]
     :filtered-items       filtered-items
     :filtered-item-groups filtered-item-groups
     :items-group-by       @group-by-key
     :items-filter-by      @active-filters
     :sort-groups-key      @sort-groups-key}))
