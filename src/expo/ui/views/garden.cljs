(ns expo.ui.views.garden
  (:require
   [hooks.garden]
   [uix.core.alpha :as uix]))

(defn garden-node
  [{:keys [on-select]} item]
  (let [{:org/keys      [source-file]
         :org.prop/keys [title created-at]}

        item
        hovering? (uix/state false)]
    [:div
     {:class          ["m-1" "p-4"
                       "border" "border-city-blue-600"
                       "bg-yo-blue-700"
                       "text-white"
                       (when @hovering? "cursor-pointer")]
      :on-click       #(on-select)
      :on-mouse-enter #(reset! hovering? true)
      :on-mouse-leave #(reset! hovering? false)}

     title

     (when created-at
       [:div
        {:class ["font-mono"]}
        created-at])

     [:div
      {:class    ["font-mono"
                  "hover:text-city-blue-400"]
       :on-click (fn [_]
                   (let [res (hooks.garden/open-in-emacs item)]
                     (println res)
                     res))}
      source-file]]))

(defn selected-node
  [{:org/keys      [source-file body]
    :org.prop/keys [title]}]

  [:div
   {:class ["flex" "flex-col" "p-2"]}
   [:span
    {:class ["font-nes" "text-xl" "text-city-green-200" "p-2"]}
    title]

   [:span
    {:class ["font-mono" "text-xl" "text-city-green-200" "p-2"]}
    source-file]

   [:div
    {:class ["font-mono" "text-city-blue-400"
             "flex" "flex-col" "p-2"
             "bg-yo-blue-500"]}
    (for [[i line] (map-indexed vector body)]
      (let [{:keys [text]} line]
        (cond
          (= "" text)
          ^{:key i} [:span {:class ["py-1"]} " "]

          :else
          ^{:key i} [:span text])))]])

(defn view []
  (let [{:keys [items]} (hooks.garden/use-garden)
        selected        (uix/state (first items)) ]
    [:div
     {:class ["flex" "flex-col" "flex-wrap"
              "overflow-hidden"
              "min-h-screen"]}

     (when-not items
       [:div
        {:class ["p-6" "text-lg" "text-white"]}
        "Loading...................."])

     (when @selected
       (selected-node @selected))

     [:div
      {:class ["flex" "flex-row" "flex-wrap"
               "justify-center"]}
      (for [[i it] (->> items (map-indexed vector))]
        ^{:key i}
        [garden-node
         {:on-select    (fn [_] (reset! selected it))
          :is-selected? (= @selected it)}
         (assoc it :index i)])]]))
