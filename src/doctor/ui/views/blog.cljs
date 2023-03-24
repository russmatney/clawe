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
   {:class ["bg-city-blue-600" "h-8"
            "w-full"
            "flex" "flex-col"]}])

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
                          filter-todos-results]}]
  (let [item-group-open? (uix/state false)]
    ;; item group
    [:div
     {:class ["flex" "flex-col"]}
     [:div
      [:hr {:class ["mt-6" "border-city-blue-900"]}]
      [:div
       {:class ["p-6" "flex flex-row"]}
       ;; TODO filter-grouper group-by label rendering needs love
       (cond
         (#{:priority} (:items-group-by filter-todos-results))
         (if label
           [components.garden/priority-label
            ;; mocking an org-item here
            {:org/priority label}]
           [:span
            {:class ["font-nes" "text-city-blue-400"]}
            "No Priority"])

         (#{:tags} (:items-group-by filter-todos-results))
         (if label
           [:div
            ;; TODO style
            label]
           [:span
            {:class ["font-nes" "text-city-blue-400"]}
            "No tags"])

         (#{:short-path} (:items-group-by filter-todos-results))
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
;; main widget

(defn widget [opts]
  ;; TODO the 'current' usage in this widget could be a 'tag' based feature
  ;; i.e. based on arbitrary tags, e.g. if that's our 'mode' right now
  ;; i.e. 'current' is an execution mode - another mode might be pre or post execution
  (let [blog-data           (use-blog/use-blog-data)
        {:keys [all-notes]} @blog-data
        notes               all-notes

        sample-pill-active (uix/state nil)
        pills
        [{:on-click #(swap! sample-pill-active not)
          :label    "Sample Pill"
          :active   @sample-pill-active}]

        filter-todos-results (components.filter/use-filter
                               (assoc filter-defs/fg-config
                                      :extra-preset-pills pills
                                      :items notes))]
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

      (:filter-grouper filter-todos-results)]

     (when (seq (:filtered-items filter-todos-results))
       [:div {:class ["pt-6"]}

        (for [[i group-desc]
              (->> (:filtered-item-groups filter-todos-results)
                   (map-indexed vector))]
          ^{:key i}
          [note-group (assoc group-desc
                             :filter-todos-results filter-todos-results)])])

     (when (not (seq notes))
       [:div
        {:class ["text-bold" "text-city-pink-300" "p-4"]}
        [:h1
         {:class ["text-4xl" "font-nes"]}
         "no todos found!"]
        [:p
         {:class ["text-2xl" "pt-4"]}
         ""]])]))
