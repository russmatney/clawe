(ns components.note
  (:require
   [clojure.set :as set]
   [taoensso.telemere :as log]
   [tick.core :as t]
   [uix.core :as uix :refer [$ defui]]

   [dates.tick :as dates]
   ))

(defn item-has-any-tags
  "Returns truthy if the item has at least one matching tag."
  [item]
  (-> item :org/tags seq))

(defn item-has-tags
  "Returns truthy if the item has at least one matching tag."
  [item tags]
  (-> item :org/tags (set/intersection tags) seq))

(defn backlink-notes [_note]
  ;; TODO how to cal on the frontend - could cache the total, or use datascript
  []
  #_(->> note :org/id
         blog.db/id->root-notes-linked-from
         (filter (comp blog.db/id->link-uri :org/id))))

(defui tags-list
  [{:keys [tags] :as note}]
  (let [tags (or tags (:org/tags note))]
    (when (seq tags)
      (->>
        tags
        (map #(str "#" %))
        (map-indexed
          (fn [i tag]
            ($ :a {:key   i
                   :href  (str "/tags.html" tag)
                   :class ["font-mono"]} tag)))
        (into ($ :div
                 {:class ["space-x-1"
                          "flex flex-row flex-wrap"]}))))))

(defn note->flattened-items [note]
  (tree-seq (comp seq :org/items) :org/items note))

(defn ->all-tags [item]
  ;; TODO daily notes should filter untagged items (subitems)
  (->> item note->flattened-items
       (mapcat :org/tags) (into #{})))

(defn ->all-links [item]
  (->> item note->flattened-items
       (mapcat :org/links-to) (into #{})))

(defn ->items-with-tags [note]
  (some->> note :org/items (filter item-has-any-tags)))

(defn ->all-images [item]
  (->> item note->flattened-items (mapcat :org/images)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; item metadata
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn date-published [note]
  (when (:blog/published-at note)
    (t/format "MMM dd, YYYY"
              (-> note :blog/published-at))))

(defn last-modified [note]
  (let [parsed
        (-> note :file/last-modified dates/parse-time-string)]
    (when-not parsed
      (log/log! :debug ["could not parse :file/last-modified"
                        (-> note :file/last-modified)
                        note]))
    (when parsed
      (t/format "MMM dd, YYYY" parsed))))

(defn created-at [note]
  (when (:org.prop/created-at note)
    (t/format "MMM dd, YYYY"
              (-> note :org.prop/created-at dates/parse-time-string))))

(defn word-count [note]
  ;; TODO daily notes should filter untagged items in word count
  (let [items (note->flattened-items note)
        #_    (org-crud/nested-item->flattened-items note)]
    (reduce + 0 (map :org/word-count items))))

(defui metadata [{:keys [item]}]
  ($ :div
     {:class ["flex flex-col"]}

     ;; TODO show more metadata
     (let [c (created-at item)]
       (when c
         ($ :span
            {:class ["font-mono"]}
            (str "Created: " c))))
     (let [dp (date-published item)]
       (when dp
         ($ :span
            {:class ["font-mono"]}
            (str "Published: " dp))))
     ($ :span
        {:class ["font-mono"]}
        (str "Last modified: " (last-modified item)))
     ($ :div
        {:class []}
        (if (seq (->all-tags item))
          (tags-list (assoc item
                            :tags (->> (->all-tags item) sort)))
          ($ :span {:class ["font-mono"]} "No tags")))
     ($ :span
        {:class ["font-mono"]}
        (str "Word count: " (word-count item)))
     (let [backlinks (backlink-notes item)]
       (when (seq backlinks)
         ($ :span
            {:class ["font-mono"]}
            (str "Backlinks: " (count backlinks)))))
     ($ :hr)))
