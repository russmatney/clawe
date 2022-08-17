(ns doctor.ui.handlers
  (:require
   [plasma.core :refer [defhandler]]
   #?@(:clj [[garden.core :as garden]
             [ralphie.emacs :as emacs]
             [git.core :as git]
             [chess.core :as chess]
             [screenshots.core :as screenshots]
             [chess.db :as chess.db]
             [wallpapers.core :as wallpapers]
             [clawe.wm :as wm]
             [db.core :as db]
             [datascript.core :as d]
             [org-crud.api :as org-crud.api]]
       :cljs [[hiccup-icons.fa :as fa]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; db items
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler delete-from-db [item]
  (println "deleting item from db" item)
  (db/retract (:db/id item))
  :ok)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; garden
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler open-in-journal [item]
  (when-let [path (:org/source-file item)]
    (wm/show-client "journal")
    (emacs/open-in-emacs {:emacs/file-path  path
                          :emacs/frame-name "journal"}))
  :ok)

(comment
  (open-in-journal {:org/source-file "/home/russ/todo/journal.org"}))


(defhandler open-in-emacs [item]
  (when-let [path (:org/source-file item)]
    (emacs/open-in-emacs {:emacs/file-path path}))
  :ok)

(defhandler full-garden-items [paths]
  (->> paths
       (map garden/full-item)
       (into [])))

(defhandler full-garden-item [item]
  (garden/full-item item))


(defhandler purge-org-source-file [item]
  (println "purging-org-source-file from db" (:org/source-file item))
  (when (:org/source-file item)
    (->>
      (db/query '[:find ?e
                  :in $ ?source-file
                  :where
                  [?e :org/source-file ?source-file]]
                (:org/source-file item))
      (map first)
      db/retract))
  :ok)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; screenshots
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler ingest-screenshots []
  (screenshots/ingest-screenshots)
  :ok)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; lichess
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler ingest-lichess-games []
  (chess.db/ingest-lichess-games)
  :ok)

(defhandler clear-lichess-games-cache []
  (chess/clear-cache)
  :ok)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; repos/commits
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler ingest-clawe-repos []
  (git/ingest-clawe-repos)
  :ok)

(defhandler ingest-commits-for-repo [repo]
  (println "ingesting commits for repo" repo)
  (git/ingest-commits-for-repo repo)
  :ok)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; wallpapers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler ingest-wallpapers []
  (wallpapers/ingest-wallpapers)
  :ok)

(defhandler set-wallpaper [item]
  (wallpapers/set-wallpaper item)
  :ok)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; todos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler queue-todo [todo]
  (-> todo
      (assoc :todo/queued-at (System/currentTimeMillis))
      db/transact)
  :ok)

(defhandler unqueue-todo [todo]
  (->> [[:db.fn/retractAttribute (:db/id todo) :todo/queued-at]]
       (d/transact db/*conn*))
  :ok)

(defhandler add-uuid [item]
  (when-not (:org/id item)
    (org-crud.api/update! item {:org/id (random-uuid)}))
  :ok)

(defhandler cancel-todo [todo]
  (org-crud.api/update! todo {:org/status :status/cancelled})
  :ok)

(defhandler complete-todo [todo]
  (org-crud.api/update! todo {:org/status :status/done})
  :ok)

(defhandler start-todo [todo]
  (org-crud.api/update! todo {:org/status :status/in-progress})
  :ok)

(defhandler skip-todo [todo]
  (org-crud.api/update! todo {:org/status :status/skipped})
  :ok)

(defhandler clear-status [todo]
  ;; when an item shouldn't be a todo
  ;; TODO support! (doesn't work yet)
  ;; org-crud doesn't support it, and the db doesn't auto-retract attrs yet
  (org-crud.api/update! todo {:org/status nil})
  :ok)

(defhandler add-tag [item tag]
  (org-crud.api/update! item {:org/tags tag})
  :ok)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; action lists
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn todo->actions [todo]
     (let [{:keys [org/status]} todo]
       (->>
         [{:action/label    "open-in-emacs"
           :action/on-click #(open-in-journal todo)
           :action/icon     fa/pencil-alt-solid}
          (when-not (:org/id todo)
            {:action/label    "add-uuid"
             :action/on-click #(add-uuid todo)
             :action/icon     fa/hashtag-solid})
          {:action/label    "add-tag"
           :action/on-click (fn [_]
                              (let [res (js/prompt "Add tag")]
                                (when (seq res)
                                  (add-tag todo res))))
           :action/icon     fa/tag-solid}
          {:action/label    "delete-from-db"
           :action/on-click #(delete-from-db todo)
           :action/icon     fa/trash-alt-solid}
          {:action/label    "purge-source-file"
           :action/on-click #(purge-org-source-file todo)
           :action/icon     fa/trash-solid}
          (when-not (or (#{:status/cancelled :status/done} status)
                        (:todo/queued-at todo))
            {:action/label    "queue-todo"
             :action/on-click #(queue-todo todo)
             :action/icon     fa/tasks-solid})
          (when (:todo/queued-at todo)
            {:action/label    "unqueue-todo"
             :action/on-click #(unqueue-todo todo)
             :action/icon     fa/quidditch-solid})
          (when-not (#{:status/in-progress} status)
            {:action/label    "start-todo"
             :action/on-click #(start-todo todo)
             :action/icon     fa/golf-ball-solid})
          (when-not (#{:status/done} status)
            {:action/label    "complete-todo"
             :action/on-click #(complete-todo todo)
             :action/icon     fa/check-circle-solid})
          (when-not (#{:status/skipped} status)
            {:action/label    "skip-todo"
             :action/on-click #(skip-todo todo)
             :action/icon     fa/eject-solid})
          (when status
            {:action/label    "clear-status"
             :action/on-click #(clear-status todo)
             :action/icon     fa/step-backward-solid})
          (when-not (#{:status/cancelled} status)
            {:action/label    "cancel-todo"
             :action/on-click #(cancel-todo todo)
             :action/icon     fa/ban-solid})
          ]
         (remove nil?)))))


#?(:cljs
   (defn garden-file->actions [item]
     [{:action/label    "open-in-emacs"
       :action/on-click #(open-in-journal item)
       :action/icon     fa/pencil-alt-solid}
      {:action/label    "add-tag"
       :action/on-click (fn [_]
                          (let [res (js/prompt "Add tag")]
                            (when (seq res)
                              (add-tag item res))))
       :action/icon     fa/tag-solid}
      {:action/label    "purge-source-file"
       :action/on-click #(purge-org-source-file item)
       :action/icon     fa/trash-alt-solid}]))
