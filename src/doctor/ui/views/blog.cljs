(ns doctor.ui.views.blog
  (:require
   [components.floating :as floating]
   [doctor.ui.hooks.use-blog :as use-blog]
   [uix.core.alpha :as uix]
   [components.filter :as components.filter]
   [components.debug :as components.debug]
   [components.filter-defs :as filter-defs]
   [components.actions :as components.actions]
   [components.garden :as components.garden]
   [doctor.ui.handlers :as handlers]
   [tick.core :as t]
   [dates.tick :as dates.tick]
   [components.colors :as colors]))

(defn bar []
  [:div
   {:class ["bg-yo-blue-800" "w-full"
            "flex" "flex-row"]}
   [:div
    {:class ["flex" "flex-col"
             "justify-center"
             "p-3"]}
    [:span {:class ["font-nes" "text-city-blue-600"]}
     ":clawe/blog"]]

   [:div
    {:class ["ml-auto" "p-2"]}
    [components.actions/actions-list
     {:actions
      [{:action/label    "Republish Blog"
        :action/on-click (fn [_]
                           (use-blog/republish-all))}]}]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; note-comp

(defn note-anchor [opts note]
  [:div
   {:class
    (concat
      ["border-8" "p-4" "m-2" "cursor-pointer"]
      (let [i (+ 3 (:i opts (int (rand 5))))]
        (concat
          (colors/color-wheel-classes
            {:i i :n (:page-size opts 5) :type :both})
          (colors/color-wheel-classes
            {:i (+ 2 i) :n (:page-size opts 5) :type :both :hover? true}))))}
   [:span
    {:class ["font-nes"]}
    (:org/name-string note)]])

(defn note-comp
  ([note] [note-comp nil note])
  ([{:keys [item-group label] :as opts}
    {:keys [] :as note}]
   [:div
    {:class ["flex flex-col" "p-4"]}
    ;; header
    [:div
     {:class ["flex flex-row"]}

     [floating/popover
      {:hover        true :click true
       :anchor-comp  [note-anchor opts note]
       :popover-comp [components.garden/full-note note]}]

     ;; raw note on hover
     [components.debug/raw-metadata {:label "raw"} note]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; notes table def

(defn notes-table-def []
  {:headers ["Published" "Name" "Tags (incl. nested)" "Actions" "Raw"]
   :->row   (fn [note]
              (let [note (assoc note :doctor/type :type/garden)]
                [[:span
                  (when (:blog/published note) "Published")]
                 [floating/popover
                  {:hover        true :click true
                   :anchor-comp  (:org/name note)
                   :popover-comp [components.garden/full-note note]}]
                 [components.garden/all-nested-tags-comp note]
                 [components.actions/actions-list
                  {:actions (handlers/->actions note)}]
                 [components.debug/raw-metadata {:label "raw"} note]]))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; presets

(defn filter-modified-in-last-n-days [n]
  {:filter-key :filters/last-modified-date
   :match-fn
   (fn [lm] (t/>= lm (t/date (t/<< (dates.tick/now) (t/new-duration n :days)))))})

(defn filter-has-tags [tags]
  (cond
    (string? tags) {:filter-key :filters/tags :match tags}
    (coll? tags)   {:filter-key :filters/tags :match-fn (->> tags (into #{}))}))

(defn presets []
  ;; these presets might be higher level modes, i.e. they might imply other ui changes
  {:published-by-last-modified
   {:filters     #{{:filter-key :filters/published :match "Published"}
                   (filter-modified-in-last-n-days 21)}
    :group-by    :filters/last-modified-date
    :sort-groups :filters/last-modified-date}

   :unpublished-by-last-modified
   {:filters     #{{:filter-key :filters/published :match "Unpublished"}
                   (filter-modified-in-last-n-days 21)}
    :group-by    :filters/last-modified-date
    :sort-groups :filters/last-modified-date}

   :published-by-tag
   {:filters     #{{:filter-key :filters/published :match "Published"}
                   (filter-modified-in-last-n-days 21)}
    :group-by    :filters/tags
    :sort-groups :filters/tags}

   :unpublished-by-tag
   {:filters     #{{:filter-key :filters/published :match "Unpublished"}
                   (filter-modified-in-last-n-days 21)}
    :group-by    :filters/tags
    :sort-groups :filters/tags}

   :posts-by-last-modified
   {:filters     #{(filter-has-tags #{"post" "posts"})}
    :group-by    :filters/last-modified-date
    :sort-groups :filters/last-modified-date}

   :posts-by-tags
   {:filters     #{(filter-has-tags #{"post" "posts"})}
    :group-by    :filters/tags
    :sort-groups :filters/tags}

   :tags
   {:filters     #{(filter-modified-in-last-n-days 21)}
    :group-by    :filters/tags
    :sort-groups :filters/tags
    :default     true}

   :by-last-modified-date
   {:filters     #{(filter-modified-in-last-n-days 21)}
    :group-by    :filters/last-modified-date
    :sort-groups :filters/last-modified-date}

   :modified-today
   {:filters
    #{{:filter-key :filters/last-modified-date
       :match      (t/today)}}
    :group-by    :filters/last-modified-date
    :sort-groups :filters/last-modified-date}

   :modified-yesterday
   {:filters
    #{{:filter-key :filters/last-modified-date :match (t/yesterday)}}
    :group-by    :filters/last-modified-date
    :sort-groups :filters/last-modified-date}

   :modified-last-7-days
   {:filters
    #{(filter-modified-in-last-n-days 21)}
    :group-by    :filters/last-modified-date
    :sort-groups :filters/last-modified-date}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; main widget

(defn is-daily? [note]
  (re-seq #"daily/" (:org/short-path note)))

(defn widget [_opts]
  (let [blog-data            (use-blog/use-blog-data)
        {:keys [root-notes]} @blog-data

        sort-published-first (uix/state nil)
        sort-published-last  (uix/state nil)
        hide-dailies         (uix/state nil)
        hide-all-tables      (uix/state nil)
        hide-all-groups      (uix/state nil)

        pills       [{:on-click (fn [_]
                                  (swap! sort-published-first not)
                                  (reset! sort-published-last nil))
                      :label    "sort-published-first"
                      :active   @sort-published-first}
                     {:on-click (fn [_]
                                  (swap! sort-published-last not)
                                  (reset! sort-published-first nil))
                      :label    "sort-published-last"
                      :active   @sort-published-last}
                     {:on-click #(swap! hide-dailies not)
                      :label    "hide-dailies"
                      :active   @hide-dailies}
                     {:on-click #(swap! hide-all-tables not)
                      :label    "hide-all-tables"
                      :active   @hide-all-tables}
                     {:on-click #(swap! hide-all-groups not)
                      :label    "hide-all-groups"
                      :active   @hide-all-groups}]
        filter-data (components.filter/use-filter
                      (-> filter-defs/fg-config
                          (assoc :extra-preset-pills pills
                                 :items root-notes
                                 :filter-items
                                 (fn [items]
                                   (cond->> items
                                     @hide-dailies (remove is-daily?))))
                          (update :presets merge (presets))))]

    [:div
     {:class ["bg-city-blue-800"
              "bg-opacity-90"
              "min-h-screen"
              "flex" "flex-col"
              "pb-16"]}

     [bar]

     [:hr {:class ["mb-6" "border-city-blue-900"]}]
     [:div
      {:class ["px-6"
               "text-city-blue-400"]}

      (:filter-grouper filter-data)]

     [:div
      {:class ["px-6"
               "text-city-blue-400"]}
      (str (count (:filtered-items filter-data)) " notes")]

     (when (seq (:filtered-items filter-data))
       [:div {:class ["pt-6"]}
        [components.filter/items-by-group
         (assoc filter-data
                :table-def (notes-table-def)
                :item->comp note-comp
                :hide-all-tables @hide-all-tables
                :hide-all-groups @hide-all-groups
                :sort-items (cond
                              @sort-published-first
                              (fn [items] (->> items (sort-by :blog/published >)))
                              @sort-published-last
                              (fn [items] (->> items (sort-by :blog/published <)))))]])

     (when (not (seq root-notes))
       [:div
        {:class ["text-bold" "text-city-pink-300" "p-4"]}
        [:h1
         {:class ["text-4xl" "font-nes"]}
         "no notes found!"]
        [:p
         {:class ["text-2xl" "pt-4"]}
         ""]])]))
