(ns pages.posts
  (:require
   [wing.uix.router :as router]
   [uix.core.alpha :as uix]
   [clojure.string :as string]

   [components.garden :as components.garden]
   [components.format :as components.format]
   [doctor.ui.db :as ui.db]))

(defn post-link
  "A clickable name, tags, and last-modified at"
  [{:keys [on-select is-selected?]} item]
  (let [{:org.prop/keys [title]
         :file/keys     [last-modified]} item]
    [:div
     {:class    ["grid" "grid-flow-col"
                 (when is-selected? "border border-city-blue-600")
                 "bg-yo-blue-700"
                 "text-white"
                 "cursor-pointer" "hover:text-city-blue-400"]
      :on-click #(on-select)}

     [:span
      {:class ["px-2" "font-mono"]}
      (components.format/s-shortener {:length 18} title)]

     [components.garden/tags-comp item]

     (when last-modified
       [:div
        {:class ["justify-self-end"
                 "font-mono" "text-gray-500"]}
        (components.format/last-modified-human last-modified)])]))

(defn toggle [s val]
  ((if (contains? s val)
     disj conj) s val))

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
    [:div
     {:class ["grid" "grid-flow-row"
              "bg-yo-blue-500"]}

     [:div
      {:class ["grid" "grid-flow-col"]}

      ;; list of note titles
      [:div
       {:class ["grid" "grid-flow-row" "p-2"
                "bg-yo-blue-700"
                "w-100"]}
       (for [[i it] (->> items
                         (sort-by :file/last-modified >)
                         (map-indexed vector))]
         ^{:key i}
         [post-link
          {:on-select    (fn [_]
                           ;; TODO set slugs in query params
                           (swap! open-posts (fn [op] (toggle op it)))
                           (reset! last-selected it)
                           (reset! selected-item-name (:org/name it)))
           :is-selected? (@open-posts it)}
          (assoc it :index i)])]

      ;; selected notes
      (when (seq @open-posts)
        [:div
         {:class ["grid" "p-2"
                  "bg-yo-blue-700"]}
         (for [[i p] (->> @open-posts
                          seq
                          ;; TODO some fancy sorting/filtering feats
                          ;; (sort-by :file/last-modified)

                          (map-indexed vector))]
           ^{:key (or (:org/source-file p) i)}
           [:div
            (components.garden/org-file p)])])]]))
