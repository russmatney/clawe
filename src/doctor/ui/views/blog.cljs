(ns doctor.ui.views.blog
  (:require
   [doctor.ui.hooks.use-blog :as use-blog]
   [uix.core.alpha :as uix]
   [components.filter :as components.filter]
   [components.debug :as components.debug]
   [components.filter-defs :as filter-defs]))

(defn bar []
  [:div
   {:class ["bg-city-blue-600"
            "w-full"
            "flex" "flex-col"]}
   [:span
    {:class ["font-mono"
             "text-city-blue-900"
             "p-2"]}
    "clawe/blog"]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; note-comp

(defn note-comp
  ([note] [note-comp nil note])
  ([{:keys [] :as opts}
    {:keys [] :as note}]
   [:div
    {:class ["flex flex-col" "p-4"]}
    ;; header
    [:div
     {:class ["flex flex-row"]}
     [:div
      (:org/name note)]

     ;; raw note on hover
     [components.debug/raw-metadata {:label "RAW"} note]]

    ;; body
    (when (:show-body? opts false)
      [:div
       {:class ["flex flex-col"]}
       (:org/body-string note)])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; presets

(defn presets []
  ;; these presets might be higher level modes, i.e. they might imply other ui changes
  {:published   {:filters     #{{:filter-key :filters/published :match "Published"}}
                 :group-by    :filters/last-modified-date
                 :sort-groups :filters/last-modified-date}
   :unpublished {:filters     #{{:filter-key :filters/published :match "Unpublished"}}
                 :group-by    :filters/last-modified-date
                 :sort-groups :filters/last-modified-date}

   :posts-by-last-modified {:filters     #{{:filter-key :filters/tags :match-fn #{"post" "posts"}}}
                            :group-by    :filters/last-modified-date
                            :sort-groups :filters/last-modified-date
                            :default     true}

   :posts-by-tags {:filters     #{{:filter-key :filters/tags :match-fn #{"post" "posts"}}}
                   :group-by    :filters/tags
                   :sort-groups :filters/tags}

   :tags
   {:filters     {}
    :group-by    :filters/tags
    :sort-groups :filters/tags
    #_#_:default true
    }

   :last-modified-date
   {:filters     {}
    :group-by    :filters/last-modified-date
    :sort-groups :filters/last-modified-date}

   :today
   {:filters
    #{{:filter-key :filters/short-path :match-str-includes-any #{(filter-defs/short-path-days-ago 0)}}}}

   :last-three-days
   {:filters
    #{{:filter-key :filters/short-path :match-str-includes-any
       (->> 3 range (map filter-defs/short-path-days-ago))}}
    :group-by :filters/short-path}

   :last-seven-days
   {:filters
    #{{:filter-key :filters/short-path :match-str-includes-any
       (->> 7 range (map filter-defs/short-path-days-ago))}}
    :group-by :filters/short-path}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; main widget

(defn widget [_opts]
  ;; TODO the 'current' usage in this widget could be a 'tag' based feature
  ;; i.e. based on arbitrary tags, e.g. if that's our 'mode' right now
  ;; i.e. 'current' is an execution mode - another mode might be pre or post execution
  (let [blog-data           (use-blog/use-blog-data)
        {:keys [all-notes]} @blog-data

        sample-pill-active (uix/state nil)

        pills       [{:on-click #(swap! sample-pill-active not)
                      :label    "Sample Pill"
                      :active   @sample-pill-active}]
        filter-data (components.filter/use-filter
                      (-> filter-defs/fg-config
                          (assoc :extra-preset-pills pills
                                 :items all-notes)
                          (update :presets merge (presets))))]
    (println "some filter data"
             :group-by-key (:group-by-key filter-data)
             :sort-groups-key (:sort-groups-key filter-data)
             :active-filters (:active-filters filter-data))
    [:div
     {:class ["bg-city-blue-800"
              "bg-opacity-90"
              "min-h-screen"
              "flex" "flex-col"]}

     [bar]

     [:hr {:class ["mb-6" "border-city-blue-900"]}]
     [:div
      {:class ["px-6"
               "text-city-blue-400"]}

      (:filter-grouper filter-data)]

     (when (seq (:filtered-items filter-data))
       [:div {:class ["pt-6"]}
        [components.filter/items-by-group
         (assoc filter-data :item->comp note-comp)]])

     (when (not (seq all-notes))
       [:div
        {:class ["text-bold" "text-city-pink-300" "p-4"]}
        [:h1
         {:class ["text-4xl" "font-nes"]}
         "no notes found!"]
        [:p
         {:class ["text-2xl" "pt-4"]}
         ""]])]))
