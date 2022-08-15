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
             [org-crud.api :as org-crud.api]]
       :cljs [[hiccup-icons.fa :as fa]])))

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
;; garden actions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler delete-from-db [item]
  (println "deleting-item-from-db" item)
  (db/retract (:db/id item))
  :ok)

(defhandler queue-todo [todo]
  (-> todo
      (assoc :todo/queued-at (System/currentTimeMillis))
      db/transact)
  :ok)

(defhandler add-to-db [todo]
  (println "upserting-to-db" todo)
  ;; (todos/upsert-todo-db todo)
  ;; (todos/update-todos)
  :ok)

(defhandler mark-done [todo]
  (println "marking-done" todo)
  (-> todo
      (assoc :todo/status :status/done)
      (assoc :todo/last-completed-at (System/currentTimeMillis))
      #_todos/upsert-todo-db)
  #_(todos/update-todos)
  :ok)

(defhandler mark-in-progress [todo]
  (println "marking-in-progress" todo)
  (-> todo
      (assoc :todo/status :status/in-progress)
      (assoc :todo/last-started-at (System/currentTimeMillis))
      #_todos/upsert-todo-db)
  #_(todos/update-todos)
  :ok)

(defhandler mark-not-started [todo]
  (println "marking-not-started" todo)
  (-> todo
      (assoc :todo/status :status/not-started)
      (assoc :todo/last-stopped-at (System/currentTimeMillis))
      #_todos/upsert-todo-db)
  #_(todos/update-todos)
  :ok)

(defhandler mark-cancelled [todo]
  (println "marking todo cancelled" todo)

  ;; if the todo is still 'in-sync', this will propogate back around just fine
  ;; if not..... good luck! we may want to cancel in the db ourselves in that case.
  ;; could be resolved with a returned result signal here - did anything get updated?
  (org-crud.api/update! todo {:org/status :status/cancelled})

  (-> todo
      ;; (assoc :todo/status :status/cancelled)
      ;; (assoc :todo/last-cancelled-at (System/currentTimeMillis))
      #_todos/upsert-todo-db)
  #_(todos/update-todos)
  :ok)

#?(:cljs
   (defn todo->actions [todo]
     (let [{:keys [org/status]} todo]
       (->>
         [
          {:action/label    "open-in-emacs"
           :action/on-click #(open-in-journal todo)
           :action/icon     fa/arrow-circle-down-solid}
          {:action/label    "delete-from-db"
           :action/on-click #(delete-from-db todo)
           :action/icon     fa/trash-alt-solid}
          (when-not (#{:status/cancelled
                       :status/done} status)
            {:action/label    "queue-todo"
             :action/on-click #(queue-todo todo)
             :action/icon     fa/tasks-solid})
          (when-not (#{:status/cancelled} status)
            {:action/label    "mark-cancelled"
             :action/on-click #(mark-cancelled todo)
             :action/icon     fa/ban-solid})
          (when-not (:db/id todo)
            {:action/label    "add-to-db"
             :action/on-click #(add-to-db todo)})
          (when-not (#{:status/done} status)
            {:action/label    "mark-done"
             :action/on-click #(mark-done todo)
             :action/icon     fa/check-circle})
          (when-not (#{:status/in-progress} status)
            {:action/label    "mark-in-progress"
             :action/on-click #(mark-in-progress todo)
             :action/icon     fa/pencil-alt-solid})
          (when-not (#{:status/not-started} status)
            {:action/label    "mark-not-started"
             :action/on-click #(mark-not-started todo)
             :action/icon     fa/sticky-note})]
         (remove nil?)))))
