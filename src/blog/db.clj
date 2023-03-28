(ns blog.db
  (:require
   [babashka.fs :as fs]
   [clojure.string :as string]
   [tick.core :as t]
   [taoensso.timbre :as log]
   [util :refer [ensure-uuid]]
   [garden.core :as garden]
   [org-crud.core :as org-crud]
   [blog.config :as blog.config]
   [systemic.core :as sys]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; db shape and build
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def initial-db
  {:root-notes-by-id              {}
   :all-notes-by-id               {}
   :published-by-id               {}
   :any-id->root-note             {}
   :link-id->linking-root-note-id {}})

(defn add-note-to-db [blog-config db note]
  (let [blog-def (-> blog-config :notes (get (:org/short-path note)))
        note     (if blog-def
                   (merge note blog-def {:blog/published true})
                   note)

        children          (->> note (org-crud/nested-item->flattened-items))
        children-with-ids (->> children (filter :org/id))

        all-link-ids (->>
                       (concat (or (some->> note :org/links-to (map :link/id)) [])
                               (->> children
                                    (mapcat :org/links-to)
                                    (map :link/id))))]
    (cond-> db
      blog-def (update :published-by-id assoc (:org/id note) note)
      true     (update :root-notes-by-id assoc (:org/id note) note)
      true     (update :all-notes-by-id
                       (fn [all-notes]
                         (reduce
                           (fn [all ch] (assoc all (:org/id ch) ch))
                           all-notes
                           children-with-ids)))

      (seq children-with-ids)
      (update :any-id->root-note
              (fn [m] (reduce (fn [m ch] (assoc m (:org/id ch) note))
                              m
                              ;; includes parent
                              children-with-ids)))

      (seq all-link-ids)
      (update :link-id->linking-root-note-id
              (fn [m]
                (reduce
                  (fn [m link-id]
                    (update m link-id
                            (fn [s]
                              (if s (conj s (:org/id note))
                                  #{(:org/id note)}))))
                  m all-link-ids))))))

(defn build-db []
  (log/info "[DB]: building blog.db")
  (let [start-t     (t/now)
        blog-config @blog.config/*config*
        blog-db     (->>
                      (garden/all-garden-notes-nested)
                      (reduce
                        (partial add-note-to-db blog-config)
                        initial-db))]
    (log/info "[DB]: blog.db built"
              (str (t/millis (t/duration {:tick/beginning start-t
                                          :tick/end       (t/now)})) "ms"))
    blog-db))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; systemic overhead
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(sys/defsys ^:dynamic *notes-db*
  :start
  (log/info "[DB]: *notes-db* (re)started")
  (atom (build-db)))

(defn update-db-note [note]
  (let [note        (-> note :org/source-file org-crud/path->nested-item)
        blog-config @blog.config/*config*]
    (log/info "Updating *notes-db* with note" (:org/short-path note))
    ;; TODO we will need to remove references first
    ;; e.g. when links are updated/deleted
    (swap! *notes-db* (fn [db]
                        (log/info "updating db")
                        (add-note-to-db blog-config db note)))))

(defn refresh-notes []
  (if (sys/running? `*notes-db*)
    (sys/restart! `*notes-db*)
    (sys/start! `*notes-db*)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; getters
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-db []
  (sys/start! `*notes-db*)
  @*notes-db*)

(comment
  (->>
    @blog.config/*config*
    :notes
    vals
    (filter :org/name-string)
    (filter (fn [note]
              (re-seq #"all my patrons" (:org/name-string note)))))

  (->>
    (get-db)
    :root-notes-by-id
    vals
    (filter (fn [note]
              (re-seq #"all my patrons" (:org/name-string note))))
    first
    :blog/published
    ))

(defn root-notes []
  (sys/start! `*notes-db*)
  (vals (:root-notes-by-id @*notes-db*)))

(defn fetch-root-note-with-id
  "Fetches a root note for the passed id. Supports fetching the root note with a child id."
  [id]
  (-> (get-db) :any-id->root-note (get (ensure-uuid id))))

(defn id->root-notes-linked-from
  "Returns a list of items that link to the passed id."
  [id]
  (let [n-by-id (:all-notes-by-id (get-db))]
    (-> (get-db) :link-id->linking-root-note-id
        (get (ensure-uuid id))
        (->> (map n-by-id)))))

;; TODO re-impl and consume, if needed
;; (defn linked-to [_note]
;;   (let [links       nil #_ (item/item->all-links note)
;;         notes-by-id (notes-by-id)]
;;     (->> links
;;          (map :link/id)
;;          (map notes-by-id))))

(comment
  (count
    (-> (get-db) :link-id->linking-root-note-id))

  (->>
    (get-db)
    :root-notes-by-id
    vals
    (sort-by :file/last-modified)
    (reverse)
    (take 2))

  (fetch-root-note-with-id #uuid "b4e38bb2-2e05-43e4-9a88-808a55602932")
  (id->root-notes-linked-from #uuid "b4e38bb2-2e05-43e4-9a88-808a55602932")

  (fetch-root-note-with-id #uuid "8b22b22a-c442-4859-9927-641f8405ec8d")
  (id->root-notes-linked-from #uuid "8b22b22a-c442-4859-9927-641f8405ec8d"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; published note api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn published-notes []
  (->> (get-db) :published-by-id vals))

(comment
  (count (published-notes))

  (->>
    (published-notes)
    (filter (fn [note]
              (re-seq #"github" (:org/name-string note)))))
  )

(defn published-id? [id]
  (-> (get-db) :published-by-id (get id)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; uris
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn path->uri [path]
  (-> path fs/file-name fs/strip-ext
      (#(str (cond
               (string/includes? path "/daily/")      "/daily"
               (string/includes? path "/workspaces/") "/note/workspaces"
               :else                                  "/note")
             "/" % ".html"))))

(defn note->uri [note]
  (-> note :org/source-file path->uri))

(defn ^:dynamic *id->link-uri*
  "Passed into org-crud to determine if a text link should be included or ignored."
  [id]
  (let [note (fetch-root-note-with-id id)]
    (if-not note
      (log/warn "[WARN: bad data]: could not find org note with id:" id)
      (let [linked-id (:org/id note)]
        (if (published-id? linked-id)
          (note->uri note)
          ;; returning nil here to signal the link's removal
          nil)))))
