(ns components.filter
  (:require
   [clojure.string :as string]
   [uix.core.alpha :as uix]

   [components.floating :as floating]
   [components.table :as components.table]
   [components.actions :as components.actions]
   [components.debug :as debug]
   [util :as util]
   [components.pill :as pill]
   [doctor.ui.localstorage :as localstorage]
   [hiccup-icons.octicons :as octicons]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; grouped filter items component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pagination-actions [{:keys [page-size step total]}]
  [(when (< @page-size total)
     {:action/label    (str "all (" total ")")
      :action/on-click (fn [_] (reset! page-size total))})
   (when (> @page-size 0)
     (let [new-size (max (- @page-size step) 0)]
       {:action/label    (str "less (" new-size ")")
        :action/on-click (fn [_] (reset! page-size new-size))}))
   (when (< @page-size total)
     (let [new-size (min (+ @page-size step) total)]
       {:action/label    (str "more (" new-size ")")
        :action/on-click (fn [_] (reset! page-size new-size))}))])

(defn group->comp
  [{:keys [item-group label item->comp group-by-key filter-items sort-items all-filter-defs
           default-page-size table-def
           hide-all-tables hide-all-groups
           active-filters-fn
           xs->xs]
    :as   opts}]
  (let [label->group-by-label (or (some-> group-by-key all-filter-defs :group-by-label)
                                  (fn [label] (or (str label) "None")))
        label-comp            (some-> group-by-key all-filter-defs :group-by-label-comp)
        label                 (label->group-by-label label)
        show-group            (uix/state true)
        group-comp-open?      (uix/state true)
        item-group-open?      (uix/state true)
        table-open?           (uix/state true)
        items                 (cond->> item-group
                                xs->xs       xs->xs
                                filter-items filter-items
                                sort-items   sort-items)
        page-size             (uix/state (or default-page-size 4))]
    [:div
     {:class ["flex" "flex-col"]}
     [:div
      [:hr {:class ["mt-6" "border-city-blue-900"]}]
      [:div
       {:class ["p-6" "flex flex-row" "items-center"]}
       [:span
        {:class ["font-nes" "text-city-blue-400"]}
        (if (and label label-comp)
          [label-comp label]
          (str (label->group-by-label label)))
        (str " (" (count items) ")")]

       [:span
        {:class ["pl-4" "font-mono" "text-city-blue-400"]}
        [debug/raw-metadata {:label "raw opts"}
         ;; limiting our selection here b/c opts is quite huge
         ;; (and has full components in it)
         (select-keys
           opts
           [:label :group-by-key :default-page-size :table-def
            :hide-all-tables :hide-all-groups])]]

       [:span
        {:class ["pl-4" "font-mono" "text-city-blue-400"]}
        [debug/raw-metadata {:label "raw items"}
         (->> items (take 10)
              (map (fn [x] (update x :org/items (fn [its] (take 2 its))))))]]

       [:div
        {:class ["ml-auto"]}

        [components.actions/actions-list
         {:n 5 ;; num actions to show before paginating
          :actions
          (concat
            [{:action/on-click (fn [_] (swap! show-group not))
              :action/icon     (if @show-group octicons/chevron-up16 octicons/chevron-down16)
              :action/label    (if @show-group "Hide" "Show")}]

            (when-not (and hide-all-tables hide-all-groups)
              (pagination-actions {:page-size page-size :step 4 :total (count items)}))

            [(when (not hide-all-tables)
               {:action/on-click (fn [_] (swap! table-open? not))
                :action/label    (str (if @table-open? "Hide table" "Show table"))})
             (when (not hide-all-groups)
               {:action/on-click (fn [_] (swap! item-group-open? not))
                :action/label    (str (if @item-group-open? "Hide items" "Show items"))})
             (when (not hide-all-groups)
               {:action/on-click (fn [_] (swap! group-comp-open? not))
                :action/label    (str (if @group-comp-open? "Hide group" "Show group"))})])}]]]]

     (when (and @show-group @item-group-open? (not hide-all-groups))
       [:div
        {:class ["flex" "flex-row" "flex-wrap" "justify-around"]}

        (when item->comp
          (for [[i it] (->> items (take @page-size) (map-indexed vector))]
            ^{:key (str (:org/name it) i)}
            [item->comp (assoc opts :i i :page-size @page-size
                               :filter-by active-filters-fn
                               :filter-fn filter-items) it]))])

     (when (and @show-group @table-open? (not hide-all-tables))
       [:div
        {:class ["flex" "flex-row" "w-full"]}
        (when (and table-def (:->row table-def))
          [components.table/table
           (-> table-def
               (assoc :n @page-size)
               (assoc :rows (->> items (map (:->row table-def)))))])])

     (when (and @show-group @group-comp-open? (:group->comp opts))
       ((:group->comp opts) (->> items (take @page-size))))]))

(defn items-by-group [filter-data]
  [:div
   (for [[i group-desc]
         (->> (:filtered-item-groups filter-data)
              (map-indexed vector))]
     ^{:key (str (:label group-desc) i)}
     [group->comp (merge group-desc filter-data)])])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; filter def anchor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-def-anchor
  [[filter-key filter-def]
   {:keys [set-group-by-key group-by-key set-current-preset-key set-sort-groups-key]}]
  (let [group-by-enabled? (= group-by-key filter-key)]
    [:div.text-xl.font-nes
     {:class    ["cursor-pointer"
                 "hover:text-city-red-600"
                 (when group-by-enabled? "text-city-pink-400")]
      :on-click (fn [_]
                  (set-current-preset-key nil)
                  (set-group-by-key filter-key)
                  (set-sort-groups-key filter-key))}
     (:label filter-def)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; filter def popover
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filter-def-popover
  [[filter-key filter-def]
   {:keys [items _filtered-items active-filters toggle-filter-by set-current-preset-key]}]
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
          (let [filter-enabled? (active-filters (assoc filter-option :filter-key filter-key))]
            [:div
             {:key      i
              :on-click (fn [_]
                          (set-current-preset-key nil)
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
                   filter-enabled? (active-filters filter-desc)]
               [:div
                {:key      i
                 :class    ["flex" "flex-row" "font-mono"
                            "cursor-pointer"
                            "hover:text-city-red-600"
                            (when filter-enabled?
                              "text-city-pink-400")]
                 :on-click (fn [_]
                             (set-current-preset-key nil)
                             (toggle-filter-by filter-desc))}
                (let [format-label (:format-label filter-def (fn [k]
                                                               (if (nil? k) "None" (str k))))]
                  [:span.p-1.pl-2.text-xl.ml-auto (format-label k)])
                [:span.p-1.text-xl.w-10.text-center (str "(" ct ")")]]))])])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; filter-grouper full component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- filter-grouper
  "A component for displaying and selecting filters/group-bys/sort-bys.

  Returned as part of `use-filter`."
  [{:keys [all-filter-defs
           active-filters
           group-by-key
           sort-groups-key
           current-preset-key
           show-filters-inline
           presets
           extra-preset-pills
           set-filters
           set-current-preset-key
           set-group-by-key
           set-sort-groups-key
           label]
    :as   config}]
  (let [filter-detail-open? (uix/state false)]
    [:div
     {:class ["flex flex-col"]}

     [:div
      {:class ["flex" "flex-col"]}
      [:div
       {:class ["pb-4" "flex" "flex-row" "items-center"]}
       (when label
         [:span {:class ["font-nes" "text-xl"]}
          label])

       [:div
        {:class ["pl-4"]}
        (let [meta {:group-by-key    group-by-key
                    :sort-groups-key sort-groups-key
                    :active-filters  active-filters}]
          [debug/raw-metadata {:label "Metadata"}
           meta])]

       [:div
        {:class ["pl-4"]}
        [:button {:on-click #(swap! filter-detail-open? not)
                  :class    ["whitespace-nowrap"
                             "text-sm"
                             "hover:text-city-pink-400"]}
         (str (if @filter-detail-open? "Hide" "Show") " filter detail")]]

       [:div
        {:class ["ml-auto"]}
        (when (seq extra-preset-pills)
          [pill/cluster extra-preset-pills])]]

      (when (and (seq presets) (> (count presets) 1))
        [pill/cluster
         (->> presets
              (sort-by first)
              (map
                (fn [[k {:keys [filters group-by sort-groups label]}]]
                  {:label  (or label k)
                   :active (#{current-preset-key} k)
                   :on-click
                   (fn [_]
                     (set-current-preset-key k)
                     (set-filters filters)
                     (set-group-by-key group-by)
                     (set-sort-groups-key sort-groups))})))])]

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
             (for [[i f] (->> active-filters
                              (filter (comp #{filter-key} :filter-key))
                              (map-indexed vector))]
               ^{:key i} [:div
                          {:class ["font-mono"]}
                          (str f)])

             ;; active group-by
             [:div
              [:pre ":group-by " group-by-key]]

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
  (let [->value              (-> filter-key all-filter-defs
                                 ;; fallback on identity here, but otherwise use the filter's :group-by impl
                                 (:group-by identity))
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
  "Pass some `id` for unique preset local-storage persistance."
  [{:keys [items all-filter-defs filter-items sort-items id] :as config}]
  (let [ls-key           (str "filter-preset-" id)
        param-preset-key (->> (localstorage/get-item ls-key) rest (apply str) keyword)
        ;; param-preset-key (router/use-route-parameters [:query :preset])
        [preset-key default]
        (or
          ;; preset matching key from local storage
          (and param-preset-key
               (some->> config :presets
                        (filter (comp #{param-preset-key} first)) first))
          ;; first preset with {:default true} in desc
          (some->> config :presets (filter (comp :default second)) first)
          ;; preset with :default as key
          (some->> config :presets (filter (comp #{:default} first)) first))

        initial-filters         (or (some-> default :filters) #{})
        initial-group-by-key    (or (some-> default :group-by)
                                    (some-> all-filter-defs first first))
        initial-sort-groups-key (some-> default :sort-groups)

        active-filters     (uix/state initial-filters)
        group-by-key       (uix/state initial-group-by-key)
        sort-groups-key    (uix/state initial-sort-groups-key)
        current-preset-key (uix/state preset-key)

        active-filters-fn
        (when (seq @active-filters)
          (->>
            @active-filters
            (group-by :filter-key)
            (map (partial filter-match-fn all-filter-defs))
            ;; every predicate must match
            (apply every-pred)))

        filtered-items (cond->> items
                         active-filters-fn (filter active-filters-fn)
                         filter-items      filter-items
                         sort-items        sort-items)

        group-by-f (or (some-> @group-by-key all-filter-defs :group-by)
                       (fn [_]
                         #_(println "WARN: no group-by-f could be determined")
                         :default))

        filtered-item-groups
        (->> filtered-items
             (group-by group-by-f)
             util/expand-coll-group-bys
             (map (fn [[label its]] {:item-group its :label label})))

        sort-groups-f        (some-> @sort-groups-key all-filter-defs :sort-groups-fn)
        filtered-item-groups (if sort-groups-f
                               (sort-groups-f filtered-item-groups)
                               (sort-by (comp util/label->comparable-int :label) < filtered-item-groups))]

    {:filter-grouper
     [filter-grouper
      (-> config
          (merge
            {:filtered-items         filtered-items
             :current-preset-key     @current-preset-key
             :active-filters         @active-filters
             :group-by-key           @group-by-key
             :sort-groups-key        @sort-groups-key
             :set-current-preset-key (fn [k]
                                       (localstorage/set-item! ls-key k)
                                       (reset! current-preset-key k))
             :set-group-by-key       #(reset! group-by-key %)
             :set-sort-groups-key    #(reset! sort-groups-key %)
             :set-filters            #(reset! active-filters %)
             :toggle-filter-by
             (fn [f-by]
               ;; TODO filters that use funcs won't match/exclude here
               (swap! active-filters #(if (% f-by) (disj % f-by) (conj % f-by))))}))]
     :all-filter-defs      all-filter-defs
     :filtered-items       filtered-items
     :filtered-item-groups filtered-item-groups
     :group-by-key         @group-by-key
     :active-filters       @active-filters
     :sort-groups-key      @sort-groups-key
     :current-preset-key   @current-preset-key
     ;; eventually passed into group by
     :active-filters-fn    active-filters-fn
     :filter-items         filter-items}))
