(ns api.todos
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [org-crud.core :as org-crud]
   [ralphie.zsh :as r.zsh]
   [tick.core :as t]
   [babashka.fs :as fs]
   [clojure.string :as string]
   [defthing.db :as db]
   [wing.core :as w]
   [dates.tick :as dt]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB todo crud
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-todo-db
  "Matches on just :org/name for now."
  [item]
  (some->>
    (db/query
      '[:find (pull ?e [*])
        :in $ ?name
        :where
        [?e :org/name ?name]]
      (:org/name item))
    ffirst))

(defn upsert-todo-db [item]
  (let [existing (get-todo-db item)
        merged   (merge existing item)
        merged   (update merged :org/id #(or % (java.util.UUID/randomUUID)))]
    (db/transact [merged])))

(defn list-todos-db []
  (some->>
    (db/query
      '[:find (pull ?e [*])
        :where [?e :todo/name ?name]])
    (map first)))

(comment
  ;; (db/query
  ;;   '[:find (pull ?e [*])
  ;;     :in $ ?name
  ;;     :where
  ;;     [?e :org/name ?name]]
  ;;   (:org/name --i))

  (db/query
    '[:find (pull ?e [*])
      :where
      [?e :todo/status :status/in-progress]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def org-item-date-keys
  #{:org/closed
    :org/scheduled
    :org/deadline
    :org.prop/archive-time
    :org.prop/created-at})


(defn org-item->todo
  [{:org/keys [name source-file status] :as item}]
  (let [parsed-date-fields (->> org-item-date-keys
                                (map (fn [k]
                                       [k (-> item k dt/parse-time-string)]))
                                (into {}))]
    (->
      item
      (assoc :todo/name name
             :todo/file-name (str (-> source-file fs/parent fs/file-name) "/" (fs/file-name source-file))
             :todo/status status)
      (merge parsed-date-fields))))

(def zsh-org-roots
  ["~/todo/{journal,projects}.org"
   "~/todo/garden/workspaces/*.org"
   ])

(defn org-file-paths
  ([] (org-file-paths {}))
  ([{:keys [additional-roots]}]
   (let [additional-roots (or additional-roots [])]
     (->> zsh-org-roots
          (concat additional-roots)
          (mapcat #(-> %
                       r.zsh/expand
                       (string/split #" ")))
          concat))))

(defn daily-path [day]
  (str "~/todo/daily/" day ".org"))

(comment
  (count
    (t/range
      (-> (t/today) (t/<< (t/new-period 14 :days)))
      (t/today)
      (t/new-period 1 :days)
      ))
  )

(defn days
  "Returns dates for the last n days, including today."
  [n]
  (t/range
    (-> (t/today) (t/<< (t/new-period (dec n) :days)))
    (t/tomorrow)))

(comment
  (days 14))

(defn monthly-archive-path [year-month]
  (str "~/todo/archive/" year-month ".org")
  )

(defn months []
  (->>
    [
     (t/today)
     (t/<<
       (t/today)
       (t/new-period 1 :months))
     (t/<<
       (t/today)
       (t/new-period 2 :months))]
    (map (fn [t]
           (t/format (t/formatter "yyyy-MM") t)))))

(comment
  (->> (months) (map monthly-archive-path))
  )



(comment
  (org-file-paths)
  (org-file-paths
    {:additional-roots ["~/russmatney/{doctor,clawe,org-crud}/{readme,todo}.org"]})

  (org-file-paths
    {:additional-roots (->> (days 14) (map daily-path))}))

(defn build-org-todos []
  (->> (org-file-paths
         {:additional-roots
          (concat (->> (days 14) (map daily-path)))})
       (map fs/file)
       (filter fs/exists?)
       (mapcat org-crud/path->flattened-items)
       (filter :org/status) ;; this is set for org items with a todo state
       (map org-item->todo)
       (map #(merge % (get-todo-db %)))))

(comment
  (->> (build-org-todos)
       (filter :todo/status)
       (group-by :todo/status)
       (map (fn [[s xs]]
              [s (count xs)])))

  (->>
    (build-org-todos)
    (take 7))

  (some->>
    (db/query
      '[:find (pull ?e [*])
        :where
        [?e :todo/status :status/in-progress]])))

(defn sorted-todos []
  (->> (build-org-todos)
       (sort-by :db/id)
       reverse
       (sort-by :todo/status)
       (sort-by (comp not #{:status/in-progress} :todo/status))))


(defn recent-org-items []
  ;; TODO consider db pulling/overwriting/syncing
  (->> (org-file-paths
         {:additional-roots
          (concat
            (->> (days 14) (map daily-path))
            (->> (months) (map monthly-archive-path)))})
       (map fs/file)
       (filter fs/exists?)
       (mapcat org-crud/path->flattened-items)
       (map org-item->todo)))

(comment
  (recent-org-items))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; get-todos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-todos []
  (let [org-todos (build-org-todos)
        db-todos  (list-todos-db)
        ;; should these be merged instead of concat/deduped?
        all       (->> (concat org-todos db-todos)
                       (w/distinct-by :org/name))]
    all))

(comment
  (->>
    (get-todos)
    (filter :db/id)
    (count)))

(defsys *todos-stream*
  :start (s/stream)
  :stop (s/close! *todos-stream*))

(comment
  (sys/start! `*todos-stream*))

(defn update-todos []
  (s/put! *todos-stream* (get-todos)))
