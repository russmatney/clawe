(ns notebooks.blog-daily
  {:nextjournal.clerk/visibility {:code :hide :result :hide}
   :nextjournal.clerk/toc        true}
  (:require
   [garden.core :as garden]
   [org-crud.core :as org-crud]
   [org-crud.markdown :as org-crud.markdown]
   [nextjournal.clerk :as clerk]
   [clojure.string :as string]
   [clojure.set :as set]
   [clojure.walk :as walk]
   [notebooks.clerk :as notebooks.clerk]
   [tick.core :as t]))

(defonce ^:dynamic day (t/today))

(defn export-for-day [{:keys [day]}]
  (with-bindings {#'notebooks.blog-daily/day day}
    (notebooks.clerk/path+ns-sym->spit-static-html
      (str "daily/" day ".html") 'notebooks.blog-daily)))

(comment
  (export-for-day {:day (t/today)})
  (export-for-day {:day (str (t/<< (t/today) (t/new-period 1 :days)))})

  (with-bindings {#'notebooks.blog-daily/day (t/yesterday)}
    (->>
      (notebooks.clerk/eval-notebook 'notebooks.blog-daily)
      :blocks
      (take 12)
      last
      :result
      :nextjournal/value
      :nextjournal/value)))


^{::clerk/no-cache true}
(def todays-org-item
  (let [item (-> (garden/daily-path day) org-crud/path->nested-item)]
    ;; recursively remove all :private: tagged items

    ;; TODO pull this walk construct into a useful org-crud fn
    (walk/postwalk
      (fn [node]
        (cond
          (not (map? node)) node

          (and
            (map? node)
            (seq (:org/items node)))
          (update node :org/items
                  (fn [items]
                    (->>
                      items
                      (remove
                        (fn [{:keys [org/tags]}]
                          (seq (set/intersection
                                 ;; tags that signal item rejection
                                 #{"private"}
                                 tags)))))))

          :else node))
      item)))

(def daily-items
  (->> todays-org-item :org/items))

(defn items-with-tags [tags]
  (->> daily-items
       (filter (comp seq :org/tags))
       (filter (comp seq #(set/intersection tags %) :org/tags))))

(comment
  (items-with-tags #{"til"})
  (items-with-tags #{"bugstory"}))

(defn md-for-items-with-tags
  [{:keys [title tags]}]
  (let [notes
        (->>
          (items-with-tags tags)
          (mapcat org-crud.markdown/item->md-body))]
    ;; TODO don't print `nil` when there are none, you dingus
    (when (seq notes)
      (clerk/md
        (str
          "## " title "\n"
          (->> notes (string/join "\n")))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
{::clerk/visibility {:result :show}}

(clerk/md
  (str "# " (:org/name todays-org-item)))

(md-for-items-with-tags
  {:title "TIL"
   :tags  #{"til"}})

(md-for-items-with-tags
  {:title "Bug Stories"
   :tags  #{"bugstory"}})

(md-for-items-with-tags
  {:title "Hammock"
   :tags  #{"hammock"}})
