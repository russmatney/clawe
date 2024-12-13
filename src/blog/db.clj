(ns blog.db
  (:require
   [babashka.fs :as fs]
   [clojure.string :as string]
   [tick.core :as t]
   [taoensso.telemere :as log]
   [org-crud.core :as org-crud]
   [org-crud.update :as org-crud.update]
   [systemic.core :as sys]

   [dates.tick :as dates]
   [util :refer [ensure-uuid]]
   [garden.core :as garden]
   [blog.config :as blog.config]))

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

        all-link-ids (concat (or (some->> note :org/links-to (map :link/id)) [])
                             (->> children
                                  (mapcat :org/links-to)
                                  (map :link/id)))]
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
  (log/log! :info "[DB]: building blog.db")
  (let [start-t     (t/now)
        blog-config @blog.config/*config*
        blog-db     (->> (garden/all-garden-notes-nested)
                         (reduce
                           (partial add-note-to-db blog-config)
                           initial-db))]
    (log/log! :info ["[DB]: blog.db built"
                     (str (dates/millis-since start-t) "ms")])
    blog-db))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; systemic overhead
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(sys/defsys ^:dynamic *notes-db*
  :start
  (blog.config/reload-config)
  (log/log! :info "[BLOG-DB]: Restarting *notes-db*")
  (atom (build-db)))


(defn update-db-note [note]
  (let [note        (-> note :org/source-file org-crud/path->nested-item)
        blog-config @blog.config/*config*]
    (log/log! :info ["Updating *notes-db* with note" (:org/short-path note)])
    ;; TODO we will need to remove references first
    ;; e.g. when links are updated/deleted
    (swap! *notes-db* (fn [db]
                        (log/log! :info "updating db")
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

(defn root-notes []
  (sys/start! `*notes-db*)
  (vals (:root-notes-by-id @*notes-db*)))

(defn all-notes []
  (->> (root-notes)
       (mapcat org-crud/nested-item->flattened-items)
       (remove nil?)
       (remove empty?)))

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

(defn find-notes [note-name]
  (->> (root-notes)
       (filter (fn [note]
                 (or
                   (and (:org/name-string note)
                        (string/includes? (:org/name-string note) note-name))
                   (and (:org/source-file note)
                        (string/includes? (:org/source-file note) note-name)))))))

(defn find-note [note-name]
  (->> (find-notes note-name) first))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; published note api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn published-notes []
  (->> (get-db) :published-by-id vals))

(defn published-id? [id]
  (-> (get-db) :published-by-id (get id)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; uris
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn path->uri [path]
  (-> path fs/file-name fs/strip-ext
      (string/replace "[*`()]" "")
      (#(str (cond
               (string/includes? path "/daily/") "/daily"
               :else                             "/note")
             "/" % ".html"))))

(defn note->uri [note]
  (-> note :org/source-file path->uri))

(defn id->note
  "Needs to support published and unpublished, to show titles for unpublished notes."
  [id]
  (fetch-root-note-with-id id))

(defn note->published-uri
  "Only returns a uri if the note is published."
  [note]
  (when (and note (:blog/published note))
    (note->uri note)))

(defn id->link-uri
  [id]
  (let [note (id->note id)]
    (if note
      (note->published-uri note)
      (log/log! :warn ["[WARN: bad data]: could not find org note with id:" id]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; scratch pad
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (refresh-notes)

  ;; update existing posts to set :published: tag on original notes
  (->>
    (published-notes)
    (sort-by :file/last-modified)
    (map (fn [note]
           (org-crud.update/update!
             note {:org.update/reset-last-modified true
                   :org/tags                       "published"})))
    doall))
