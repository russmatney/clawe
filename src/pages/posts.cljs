(ns pages.posts
  (:require
   [wing.core :as w]
   [wing.uix.router :as router]
   [uix.core.alpha :as uix]
   [clojure.string :as string]
   [tick.core :as t]

   [components.garden :as components.garden]
   [components.floating :as components.floating]
   [components.format :as components.format]
   [doctor.ui.db :as ui.db]))

(defn post-link
  "A link to a blog-post-y rendering of a garden note"
  [{:keys [on-select is-selected?]} item]
  (let [{:org/keys      [tags]
         :org.prop/keys [title]
         :time/keys     [last-modified]} item
        hovering?                        (uix/state false)]
    [:div
     {:class (->> ["flex" "flex-row" "flex-wrap"
                   "px-2"
                   "align-items"
                   (when is-selected? "border border-city-blue-600")
                   "bg-yo-blue-700"
                   "text-white"
                   (when @hovering? "cursor-pointer")
                   (when @hovering? "hover:text-city-blue-400")])

      :on-click       #(on-select)
      :on-mouse-enter #(reset! hovering? true)
      :on-mouse-leave #(reset! hovering? false)}

     [:span
      {:class ["px-2" "font-mono"]}
      (components.format/s-shortener {:length 18} title)]

     ;; filename
     #_[:div
        {:class ["font-mono"]}
        (components.format/s-shortener source-file)]

     [components.garden/tags-comp item]

     [:span.ml-auto
      (when last-modified
        [:div
         {:class ["font-mono"
                  "text-gray-500"]}
         (let [time-ago  (t/duration {:tick/beginning (t/instant last-modified)
                                      :tick/end       (t/now)})
               mins-ago  (t/minutes time-ago)
               hours-ago (t/hours time-ago)
               days-ago  (t/days time-ago)]
           (cond
             (< mins-ago 60)  (str mins-ago " min(s) ago")
             (< hours-ago 24) (str hours-ago " hour(s) ago")
             :else            (str days-ago " day(s) ago")))])]]))

(defn page [{:keys [conn]}]
  (let [selected-item-name (router/use-route-parameters [:query :item-name])
        items              (->> (ui.db/garden-notes conn {:n 500})
                                (filter (comp #{:level/root} :org/level)))
        default-selection  (cond->> items
                             @selected-item-name
                             (filter (comp #{@selected-item-name} :org/name))

                             ;; TODO read from slugs in query params
                             (not @selected-item-name)
                             (filter (comp #(string/includes? % "journal.org")
                                           :org/source-file))
                             true
                             first)
        ;; TODO preserve selection (query params?)
        last-selected (uix/state default-selection)
        open-posts    (uix/state #{default-selection})]
    (println "item-name" @selected-item-name)
    ;; Posts
    ;; List of post names grouped by tag
    ;; Maybe groups of linked nodes
    ;; use d3? use godot?

    [:div
     {:class ["bg-yo-blue-500"
              "flex" "flex-col" "flex-wrap"
              "min-h-screen"
              "overflow-y-auto"]}

     ;; list/selected
     [:div
      {:class ["flex" "flex-row"]}

      [:div
       {:class ["flex"
                "flex-grow-0"
                "flex-col" "flex-wrap"
                "p-2"
                "justify-center"
                "bg-yo-blue-700"
                "max-w-100"]}
       (for [[i it] (->> items
                         ;; TODO some fancy grouping/sorting/filtering feats
                         (sort-by :file/last-modified >)
                         (map-indexed vector))]

         ^{:key i}
         [post-link
          {:on-select    (fn [_]
                           ;; TODO set slugs in query params
                           (swap! open-posts (fn [op] (w/toggle op it)))
                           (reset! last-selected it)
                           (reset! selected-item-name (:org/name it)))
           :is-selected? (@open-posts it)}
          (assoc it :index i)])]

      (when (seq @open-posts)
        [:div
         {:class ["flex"
                  "flex-grow-1"
                  "p-2"
                  "bg-yo-blue-700"]}
         (for [[i p] (->> @open-posts
                          seq
                          ;; TODO some fancy sorting/filtering feats
                          ;; (sort-by :file/last-modified)

                          (map-indexed vector))]
           ^{:key (or (:org/source-file p) i)}
           [:div
            (components.garden/org-file p)])])]]))
