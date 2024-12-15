(ns doctor.ui.views.blog
  (:require
   [tick.core :as t]
   [uix.core :as uix :refer [$ defui]]

   [components.actions :as components.actions]
   [components.colors :as colors]
   [components.debug :as components.debug]
   [components.floating :as floating]
   [components.filter :as components.filter]
   [components.filter-defs :as filter-defs]
   [components.garden :as components.garden]
   [components.note :as components.note]
   [dates.tick :as dates.tick]
   [doctor.ui.hooks.use-blog :as use-blog]
   [doctor.ui.handlers :as handlers]))

(def icon nil)

(defn is-daily? [note]
  (some->> note :org/short-path (re-seq #"^daily/")))

(defn is-garden-note? [note]
  (some->> note :org/short-path (re-seq #"^garden/")))

(defn blog-actions []
  [{:action/label    "Republish Blog"
    :action/on-click (fn [_] (use-blog/rebuild-all))}
   {:action/label    "Refresh Blog DB"
    :action/on-click (fn [_] (handlers/refresh-blog-db))}
   {:action/label    "Ingest Garden Recent"
    :action/on-click (fn [_] (handlers/ingest-garden-latest))}
   {:action/label    "Ingest Garden Full"
    :action/on-click (fn [_] (handlers/ingest-garden-full))}])

(defui bar []
  ($ :div
     {:class ["bg-yo-blue-800" "w-full"
              "flex" "flex-row"]}
     ($ :div
        {:class ["flex" "flex-col" "items-center" "justify-center"
                 "p-3"]}
        ($ :span {:class ["font-nes" "text-city-blue-600"]}
           ":clawe/blog"))

     ($ :div
        {:class ["ml-auto" "p-2"]}
        ($ components.actions/actions-list
           {:actions (blog-actions)}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; note-comp

(defui note-stats [{:keys [item] :as opts}]
  ($ :div
     {:class
      (concat
        ["border-8" "p-4" "m-2"
         "cursor-pointer"
         "flex flex-col"]
        (let [i (+ 3 (:i opts (int (rand 5))))]
          (concat
            (colors/color-wheel-classes
              {:i i :n (:page-size opts 5) :type :both})
            (colors/color-wheel-classes
              {:i (+ 2 i) :n (:page-size opts 5) :type :both :hover? true}))))}
     ($ :div
        {:class ["flex flex-row justify-between"]}

        ($ :span
           {:class ["font-nes"]}
           (:org/name-string item))

        ($ :span
           {:class ["font-nes" "ml-auto"]}
           (str "(" (inc (:i opts)) "/" (count (:item-group opts)) ")")))

     ($ components.note/metadata {:item item})

     ($ :div
        {:class ["font-nes"]}
        (str "Links: " (-> item components.note/->all-links count)))

     ;; TODO once we get the frontend db story set
     ;; ($ :div
     ;;  {:class ["font-nes"]}
     ;;  (str "Backlinks: " (-> item components.note/->all-links count)))

     (when (is-daily? item)
       ($ :div
          {:class ["font-nes"]}
          (str "Daily items: " (-> item components.note/->items-with-tags count)
               "/" (-> item :org/items count))))

     (when (seq (components.note/->all-images item))
       ($ :div
          {:class ["font-nes"]}
          (str "Images: " (count (components.note/->all-images item)))))

     ($ :hr)

     ($ :div
        {:class ["flex flex-col justify-between ml-auto"]}
        ($ floating/popover
           {:hover        true :click true
            :anchor-comp  ($ :span "content")
            :popover-comp ($ components.garden/full-note {:item item})})

        ;; raw note on hover
        ($ components.debug/raw-data {:label "raw" :data item}))))

(defui note-comp [opts]
  ($ :div
     {:class ["flex flex-col" "p-4" "grow"]}
     ($ note-stats opts)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; notes table def

(defn color-for-count [ct]
  (cond
    (#{0} ct) "text-city-blue-800"
    (#{1} ct) "text-city-blue-700"
    (>= ct 8) "text-city-pink-300"
    (>= ct 3) "text-city-red-300"
    (> ct 1)  "text-city-green-300"))

(defn notes-table-def []
  {:headers ["Published" "Tags (incl. nested)" "Name" "Words" "Tag/Items" "Tag/Todos" "pub/Links" "pub/BackLinks" "Actions"]
   :->row   (fn [note]
              (let [note            (assoc note :doctor/type :type/note)
                    items           (components.note/note->flattened-items note)
                    todos           (->> items (filter :org/status))
                    items           (->> items (remove :org/status))
                    items-with-tags (->> items (filter components.note/item-has-any-tags))
                    todos-with-tags (->> todos (filter components.note/item-has-any-tags))
                    total-wc        (components.note/word-count note)]
                [($ :span.font-mono
                    (if (:blog/published note) "Published"
                        ($ components.actions/actions-list
                           {:actions (handlers/->actions note) :n 1})))
                 ($ components.garden/all-nested-tags-comp {:item note})
                 ($ components.debug/raw-data
                    {:label (str (:org/name-string note))
                     :data  note})
                 ($ :span.font-nes
                    {:class [(color-for-count (/ total-wc 8))]}
                    total-wc)
                 (if (is-daily? note)
                   ($ :span.font-nes
                      {:class [(color-for-count (count items-with-tags))]}
                      (str (count items-with-tags) "/" (count items)))
                   ($ :span.font-nes
                      {:class [(color-for-count (count items))]}
                      (str (count items))))
                 (if (is-daily? note)
                   ($ :span.font-nes
                      {:class [(color-for-count (count todos-with-tags))]}
                      (str (count todos-with-tags) "/" (count todos)))
                   ($ :span.font-nes
                      {:class [(color-for-count (count todos))]}
                      (str (count todos))))
                 ($ :span.font-nes
                    {:class [(color-for-count (:links/published-count note))]}
                    (str (:links/published-count note) "/" (:links/count note)))
                 ($ :span.font-nes
                    {:class [(color-for-count (:backlinks/published-count note))]}
                    (str (:backlinks/published-count note) "/" (:backlinks/count note)))
                 ($ components.actions/actions-list
                    {:actions (handlers/->actions note) :n 5})]))})

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
    :sort-groups :filters/tags}

   :by-last-modified-date
   {:filters     #{#_(filter-modified-in-last-n-days 21)}
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
    :sort-groups :filters/last-modified-date}

   :unpublished-but-published-tag
   {:filters     #{{:filter-key :filters/published :match "Unpublished"}
                   {:filter-key :filters/tags :match "published"}}
    :group-by    :filters/last-modified-date
    :sort-groups :filters/last-modified-date}

   :unpublished-garden-notes
   {:filters     #{{:filter-key :filters/published :match "Unpublished"}
                   {:filter-key :filters/short-path
                    :match-fn   (fn [x]
                                  (not (filter-defs/is-daily-fname x)))}}
    :group-by    :filters/last-modified-date
    :sort-groups :filters/last-modified-date}

   :unpublished-dailies
   {:filters     #{{:filter-key :filters/published :match "Unpublished"}
                   {:filter-key :filters/short-path
                    :match-fn   filter-defs/is-daily-fname}}
    :group-by    :filters/last-modified-date
    :sort-groups :filters/last-modified-date
    :default     true}

   :dead-ends
   {:group-by    :filters/link-count
    :sort-groups :filters/link-count}

   :dead-ends-published
   {:filters     #{{:filter-key :filters/published :match "Published"}}
    :group-by    :filters/link-count
    :sort-groups :filters/link-count}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; main widget

(defui widget [opts]
  (let [blog-data            (use-blog/use-blog-data)
        {:keys [root-notes]} blog-data

        ;; TODO merge/use db notes here, drop use-blog-data
        ;; root-notes (ui.db/root-notes (:conn opts) {:join-children true})

        [sort-published-first set-sort-published-first] (uix/use-state nil)
        [sort-published-last set-sort-published-last]   (uix/use-state nil)
        [only-notes set-only-notes]                     (uix/use-state nil)
        [only-dailies set-only-dailies]                 (uix/use-state nil)
        [hide-dailies set-hide-dailies]                 (uix/use-state nil)
        [hide-all-tables set-hide-all-tables]           (uix/use-state nil)
        [hide-all-groups set-hide-all-groups]           (uix/use-state nil)

        pills [{:on-click (fn [_]
                            (set-sort-published-first not)
                            (set-sort-published-last nil))
                :label    "sort-published-first"
                :active   sort-published-first}
               {:on-click (fn [_]
                            (set-sort-published-last not)
                            (set-sort-published-first nil))
                :label    "sort-published-last"
                :active   sort-published-last}
               {:on-click #(set-only-notes not)
                :label    "only-garden-notes"
                :active   only-notes}
               {:on-click #(set-only-dailies not)
                :label    "only-dailies"
                :active   only-dailies}
               {:on-click #(set-hide-dailies not)
                :label    "hide-dailies"
                :active   hide-dailies}
               {:on-click #(set-hide-all-tables not)
                :label    "hide-all-tables"
                :active   hide-all-tables}
               {:on-click #(set-hide-all-groups not)
                :label    "hide-all-groups"
                :active   hide-all-groups}]

        filter-data
        (components.filter/use-filter
          (-> filter-defs/fg-config
              (assoc
                :id (:filter-id opts :views-blog)
                :label (str (count root-notes) " Notes")
                :extra-preset-pills pills
                :items root-notes
                :filter-items
                (fn [items]
                  (cond->> items
                    only-notes   (filter is-garden-note?)
                    hide-dailies (remove is-daily?)
                    only-dailies (filter is-daily?))))
              (update :presets merge (presets))))]

    ($ :div
       {:class ["bg-city-blue-800"
                "bg-opacity-90"
                "min-h-screen"
                "flex" "flex-col"
                "pb-16"]}

       ($ bar)

       ($ :hr {:class ["mb-6" "border-city-blue-900"]})
       ($ :div
          {:class ["px-6"
                   "text-city-blue-400"]}

          (:filter-grouper filter-data))

       ($ :div
          {:class ["px-6"
                   "text-city-blue-400"]}
          (str (count (:filtered-items filter-data)) " notes"))

       (when (seq (:filtered-items filter-data))
         ($ :div {:class ["pt-6"]}
            ($ components.filter/items-by-group
               (assoc filter-data
                      :table-def (notes-table-def)
                      :item->comp #($ note-comp %)
                      :hide-all-tables hide-all-tables
                      :hide-all-groups hide-all-groups
                      :sort-items (cond
                                    sort-published-first
                                    (fn [items] (->> items (sort-by :blog/published >)))
                                    sort-published-last
                                    (fn [items] (->> items (sort-by :blog/published <))))))))

       (when (not (seq root-notes))
         ($ :div
            {:class ["text-bold" "text-city-pink-300" "p-4"]}
            ($ :h1
               {:class ["text-4xl" "font-nes"]}
               "no notes found!")
            ($ :p
               {:class ["text-2xl" "pt-4"]}
               ""))))))
