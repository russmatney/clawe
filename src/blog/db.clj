(ns blog.db
  {:nextjournal.clerk/visibility {:code :hide :result :hide}}
  (:require
   [util :refer [ensure-uuid]]
   [garden.core :as garden]
   [org-crud.core :as org-crud]
   [systemic.core :as sys]))

;; imported from org-blog/db.
;; Could/should be rewritten to work with datascript/datalevin/whatever clawe/db offers

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; garden db system
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(declare build-db)

(sys/defsys ^:dynamic *notes-db*
  :start
  (println "[DB]: *notes-db* (re)started")
  (atom (build-db)))

(defn refresh-notes []
  (sys/restart! `*notes-db*))

(defn build-db []
  (println "[DB]: building blog.db")
  (let [all-nested (garden/all-garden-notes-nested)]
    (println "[DB]: blog.db built")
    {:all-notes all-nested}))

(defn all-notes []
  (sys/start! `*notes-db*)
  (println "[DB]: passing :all-notes")
  (:all-notes @*notes-db*))

(defn all-flattened-notes-by-id []
  (->> (all-notes)
       (mapcat org-crud/nested-item->flattened-items)
       (filter :org/id)
       (map (fn [item]
              [(:org/id item) item]))))

(defn notes-by-short-path []
  (->>
    (all-notes)
    (map (fn [n] [(:org/short-path n) n]))))

(defn notes-by-id []
  (->>
    (all-notes)
    (map (fn [n] [(:org/id n) n]))))

(defn root-note-by-child-id []
  (->>
    (notes-by-id)
    (mapcat (fn [[p-id note]]
              (concat
                [[p-id note]]
                (->> note
                     org-crud/nested-item->flattened-items
                     (map :org/id)
                     (remove nil?)
                     (map (fn [c-id]
                            [c-id p-id]))))))))

(defn root-note-for-any-id [id]
  (let [note ((root-note-by-child-id) id)]
    (if (map? note)
      note
      ((into {} (notes-by-id)) note))))

(defn fetch-with-id [id]
  (root-note-for-any-id (ensure-uuid id)))

(comment
  (count (all-notes))
  (count (notes-by-id)))

(defn root-ids-by-link-id []
  (->>
    (all-notes)
    (mapcat org-crud/nested-item->flattened-items)
    (reduce
      (fn [agg item]
        (let [item-id (:org/id item
                               ;; we need an id for what is linking to this thing
                               ;; yuck, this is a set, we want... the first? or last?
                               ;; TODO move to a sensible stack here (org-crud)
                               ;; this is used for backlinks, so we really want the linking context...
                               ;; maybe want to require ids for items with links
                               (some-> item :org/parent-ids first))]
          (if-not item-id
            #_(println "[WARN] no id/parent-id for link" item)
            agg
            (let [link-ids (->> item :org/links-to (map :link/id))]
              (reduce (fn [agg link-id]
                        (if (get agg link-id)
                          (update agg link-id conj item-id)
                          (assoc agg link-id #{item-id})))
                      agg
                      link-ids)))))
      {})))

(defn ids-linked-from
  "Returns a list of items that link to the passed id."
  [id]
  ((root-ids-by-link-id) id))

(defn notes-linked-from
  "Returns a list of items that link to the passed id."
  [id]
  (let [n-by-id (into {} (all-flattened-notes-by-id))]
    (->> (ids-linked-from id)
         (map
           ;; linking to child items? or roots only?
           (fn [id] (n-by-id id))
           #_fetch-with-id))))


(comment
  (count
    (root-ids-by-link-id))

  (root-ids-by-link-id)

  (->>
    (all-notes)
    (sort-by :file/last-modified)
    (reverse)
    (take 2))

  (fetch-with-id #uuid "b4e38bb2-2e05-43e4-9a88-808a55602932")
  (ids-linked-from #uuid "b4e38bb2-2e05-43e4-9a88-808a55602932")
  (notes-linked-from #uuid "b4e38bb2-2e05-43e4-9a88-808a55602932")

  (fetch-with-id #uuid "8b22b22a-c442-4859-9927-641f8405ec8d")
  (notes-linked-from #uuid "8b22b22a-c442-4859-9927-641f8405ec8d"))
