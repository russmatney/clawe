(ns doctor.ui.views.blog
  (:require
   [doctor.ui.hooks.use-blog :as use-blog]
   [uix.core.alpha :as uix]
   [components.filter :as components.filter]
   [components.garden :as components.garden]
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
;; note-group

(defn note-group [{:keys [item-group label
                          filter-data]}]
  (let [{:keys [items-group-by]} filter-data
        item-group-open?         (uix/state false)]
    ;; item group
    [:div
     {:class ["flex" "flex-col"]}
     [:div
      [:hr {:class ["mt-6" "border-city-blue-900"]}]
      [:div
       {:class ["p-6" "flex flex-row"]}
       ;; TODO filter-grouper group-by label rendering needs love
       (cond
         (#{:priority} items-group-by)
         (if label
           [components.garden/priority-label
            ;; mocking an org-item here
            {:org/priority label}]
           [:span
            {:class ["font-nes" "text-city-blue-400"]}
            "No Priority"])

         (#{:tags} items-group-by)
         (if label
           [:div
            ;; TODO style
            label]
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

        (let [items (->> item-group
                         (map-indexed vector))]
          (for [[i it] items]
            ^{:key i}
            [note-comp it]))])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; presets

(defn presets []
  ;; these presets might be higher level modes, i.e. they might imply other ui changes
  {:tags
   {:filters  {}
    :group-by :filters/tags}

   :last-modified-date
   {:filters  {}
    :group-by :filters/last-modified-date}
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
                      (assoc filter-defs/fg-config
                             :presets (presets)
                             :extra-preset-pills pills
                             :items all-notes))]
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

        (for [[i group-desc]
              (->> (:filtered-item-groups filter-data)
                   (map-indexed vector))]
          ^{:key i}
          [note-group (assoc group-desc
                             :filter-data filter-data)])])

     (when (not (seq all-notes))
       [:div
        {:class ["text-bold" "text-city-pink-300" "p-4"]}
        [:h1
         {:class ["text-4xl" "font-nes"]}
         "no todos found!"]
        [:p
         {:class ["text-2xl" "pt-4"]}
         ""]])]))
