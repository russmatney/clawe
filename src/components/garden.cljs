(ns components.garden
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
  [{:org/keys      [short-path body]
    :org.prop/keys [title]
    :as            item}]

  [:div
   {:class ["flex" "flex-col" "p-2"]}
   [:span
    {:class ["font-nes" "text-xl" "text-city-green-200" "p-2"]}
    title]

   [:span
    {:class    ["font-mono" "text-xl" "text-city-green-200" "p-2"
                "hover:text-city-pink-400"
                "cursor-pointer"]
     :on-click (fn [_]
                 (let [res (hooks.garden/open-in-emacs item)]
                   (println "open-in-emacs res" res)
                   res))}
    short-path]

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Org file
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn org-file [{:keys     []
                 :org/keys [name]
                 :as       item}]
  [:div
   "name"
   name])
