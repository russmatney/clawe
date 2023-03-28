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
   [dates.tick :as dates.tick]))

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

(defn modified-in-last-n-days [n]
  (fn [lm] (t/>= lm (t/date (t/<< (dates.tick/now) (t/new-duration n :days))))))

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
                            :sort-groups :filters/last-modified-date}

   :posts-by-tags {:filters     #{{:filter-key :filters/tags :match-fn #{"post" "posts"}}}
                   :group-by    :filters/tags
                   :sort-groups :filters/tags}

   :tags
   {:filters     {}
    :group-by    :filters/tags
    :sort-groups :filters/tags
    :default     true}

   :last-modified-date
   {:filters     #{{:filter-key :filters/last-modified-date
                    :match-fn   (modified-in-last-n-days 21)}}
    :group-by    :filters/last-modified-date
    :sort-groups :filters/last-modified-date}

   :today
   {:filters
    #{{:filter-key :filters/last-modified-date
       :match      (t/today)}}
    :group-by    :filters/last-modified-date
    :sort-groups :filters/last-modified-date}

   :yesterday
   {:filters
    #{{:filter-key :filters/last-modified-date :match (t/yesterday)}}
    :group-by    :filters/last-modified-date
    :sort-groups :filters/last-modified-date}

   :last-seven-days
   {:filters
    #{{:filter-key :filters/last-modified-date
       :match-fn   (modified-in-last-n-days 8)}}
    :group-by    :filters/last-modified-date
    :sort-groups :filters/last-modified-date}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; main widget

(defn widget [_opts]
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
         (assoc filter-data
                :item->comp note-comp
                :table-def
                {:headers ["Published" "Name" "Actions" "Raw"]
                 :->row   (fn [note]
                            (let [note (assoc note :doctor/type :type/garden)]
                              [[:span
                                (when (:blog/published note) "Published")
                                ]
                               [floating/popover
                                {:hover        true :click true
                                 :anchor-comp  (:org/name note)
                                 :popover-comp [components.garden/full-note note]}]
                               [components.actions/actions-popup
                                {:actions (handlers/->actions note)}]
                               [components.debug/raw-metadata {:label "raw"} note]]))})]])

     (when (not (seq all-notes))
       [:div
        {:class ["text-bold" "text-city-pink-300" "p-4"]}
        [:h1
         {:class ["text-4xl" "font-nes"]}
         "no notes found!"]
        [:p
         {:class ["text-2xl" "pt-4"]}
         ""]])]))
