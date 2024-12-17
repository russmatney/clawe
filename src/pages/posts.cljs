(ns pages.posts
  (:require
   [clojure.string :as string]
   [taoensso.telemere :as log]
   [uix.core :as uix :refer [$ defui]]

   [components.garden :as components.garden]
   [components.format :as components.format]
   [doctor.ui.hooks.use-db :as hooks.use-db]
   [doctor.ui.db :as ui.db]))

(defui post-link
  "A clickable name, tags, and last-modified at"
  [{:keys [on-select is-selected? item]}]
  (let [{:org.prop/keys [title]
         :file/keys     [last-modified]} item]
    ($ :div
       {:class    ["grid" "grid-flow-col"
                   (when is-selected? "border border-city-blue-600")
                   "bg-yo-blue-700"
                   "text-white"
                   "cursor-pointer" "hover:text-city-blue-400"]
        :on-click #(on-select)}

       ($ :span
          {:class ["px-2" "font-mono"]}
          (components.format/s-shortener {:length 18} title))

       ($ components.garden/tags-comp {:item item})

       (when last-modified
         ($ :div
            {:class ["justify-self-end"
                     "font-mono" "text-gray-500"]}
            (components.format/last-modified-human last-modified))))))

(defn toggle [s val]
  ((if (contains? s val)
     disj conj) s val))

(defui page [_opts]
  (let [{:keys [data]}     (->> (hooks.use-db/use-query
                                  {:db->data (fn [db] (ui.db/garden-notes db {:n 500}))}))
        items              (->> data
                                (filter (comp #{:level/root} :org/level)))
        ;; TODO restore
        selected-item-name nil
        ;; (router/use-route-parameters [:query :item-name])
        default-selection  (cond->> items
                             selected-item-name
                             (filter (comp #{selected-item-name} :org/name))

                             ;; TODO read from slugs in query params
                             (not selected-item-name)
                             (filter (comp #(string/includes? % "journal.org")
                                           :org/source-file))
                             true
                             first)
        ;; TODO preserve selection (query params?)
        [last-selected set-last-selected] (uix/use-state default-selection)
        [open-posts set-open-posts]       (uix/use-state #{default-selection})]
    ($ :div
       {:class ["grid" "grid-flow-row"
                "bg-yo-blue-500"]}

       ($ :div
          {:class ["grid" "grid-flow-col"]}

          ;; list of note titles
          ($ :div
             {:class ["grid" "grid-flow-row" "p-2"
                      "bg-yo-blue-700"
                      "w-100"]}
             (for [[i it] (->> items
                               (sort-by :file/last-modified >)
                               (map-indexed vector))]
               ($ post-link
                  {:key          i
                   :on-select    (fn [_]
                                   ;; TODO set slugs in query params
                                   (set-open-posts (fn [op] (toggle op it)))
                                   (set-last-selected it)
                                   ;; (reset! selected-item-name (:org/name it))
                                   )
                   :is-selected? (open-posts it)
                   :item         (assoc it :index i)})))

          ;; selected notes
          (when (seq open-posts)
            ($ :div
               {:class ["grid" "p-2"
                        "bg-yo-blue-700"]}
               (for [[i p] (->> open-posts
                                seq
                                ;; TODO some fancy sorting/filtering feats
                                ;; (sort-by :file/last-modified)

                                (map-indexed vector))]
                 ($ :div
                    {:key (or (:org/source-file p) i)}
                    ($ components.garden/org-file {:item p})))))))))
