(ns pages.posts
  (:require
   [wing.core :as w]
   [uix.core.alpha :as uix]
   [hooks.garden]
   [components.garden]
   [clojure.string :as string]
   [tick.core :as t]))

(defn s-shortener [s]
  (if (< (count s) 30)
    s
    (let [start (take 15 s)
          end   (->> s reverse (take 15) reverse)]
      (apply str (concat start "..." end)))))

(comment
  (s-shortener "some really long string with lots of thoughts that never end"))

(defn post-link
  "A link to a blog-post-y rendering of a garden note"
  [{:keys [on-select is-selected?]} item]
  (let [{:org/keys      [source-file]
         :org.prop/keys [title]
         :time/keys     [last-modified]}

        item
        hovering? (uix/state false)]
    [:div
     {:class (->> ["flex" "flex-row"
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

     [:div
      {:class    [
                  "font-mono"]
       :on-click (fn [_]
                   (let [res (hooks.garden/open-in-emacs item)]
                     (println res)
                     res))}
      (s-shortener source-file)]

     [:span.px-4]

     (s-shortener title)

     [:span.px-4]
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
            :else            (str days-ago " day(s) ago")))])]))

(defn page []
  (let [{:keys [items]}   (hooks.garden/use-garden)
        default-selection (->> items
                               ;; TODO read from slugs in query params
                               (filter (comp #(string/includes? % "journal.org") :org/source-file))
                               first)
        last-selected     (uix/state default-selection)
        open-posts        (uix/state #{default-selection})]
    ;; Posts
    ;; List of post names grouped by tag
    ;; Maybe groups of linked nodes
    ;; use d3? use godot?

    [:div
     {:class ["bg-yo-blue-500"
              "flex" "flex-col" "flex-wrap"
              "min-h-screen"
              "overflow-y-auto"
              ]}

     ;; top bar
     [:div
      {:class ["flex" "flex-col" "p-2"]}
      [:span
       {:class ["font-nes" "text-xl" "text-city-pink-200" "p-2"]}
       "Posts"]

      (when (#{0} (count items))
        [:div
         {:class ["p-6" "text-lg" "text-white"]}
         "Loading...................."
         ])]

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
                         (sort-by :time/last-modified >)
                         (map-indexed vector))]
         ^{:key i}
         [post-link
          {:on-select    (fn [_]
                           ;; TODO set slugs in query params
                           (swap! open-posts (fn [op] (w/toggle op it)))
                           (reset! last-selected it))
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
                          ;; (sort-by :time/last-modified)

                          (map-indexed vector))]
           ^{:key (or (:org/source-file p) i)}
           [:div
            (components.garden/selected-node p)])])]]))
