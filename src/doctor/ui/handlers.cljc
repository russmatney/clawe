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
             [org-crud.api :as org-crud.api]
             [garden.db :as garden.db]]
       :cljs [[hiccup-icons.fa :as fa]
              [components.icons :as components.icons]
              [components.colors :as colors]
              ["@heroicons/react/20/solid" :as HIMini]])))

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

(defhandler ingest-garden []
  (garden.db/sync-last-touched-garden-files {:n 20})
  :ok)

(defhandler open-in-journal [item]
  (when-let [path (:org/source-file item)]
    (wm/show-client "journal")
    (emacs/open-in-emacs {:emacs/file-path  path
                          :emacs/frame-name "journal"}))
  :ok)

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
   (defn open-in-journal-action [item]
     {:action/label    "open-in-emacs"
      :action/on-click #(open-in-journal item)
      :action/class    ["text-city-purple-500" "border-city-purple-500"]
      :action/icon
      [components.icons/icon-comp
       {:class ["w-6"]
        :src   "/assets/candy-icons/emacs.svg"}]}))

#?(:cljs
   (defn todo->actions [todo]
     (let [{:keys [org/status]} todo]
       (->>
         [(open-in-journal-action todo)
          {:action/label    "add-uuid"
           :action/on-click #(add-uuid todo)
           :action/icon
           [:> HIMini/FingerPrintIcon {:class ["w-6" "h-6"]}]
           :action/disabled (:org/id todo)
           :action/priority -10} ;; low-prority
          {:action/label    "add-tag"
           :action/on-click (fn [_]
                              (let [res (js/prompt "Add tag")]
                                (when (seq res)
                                  (add-tag todo res))))
           :action/icon     fa/hashtag-solid
           ;; higher priority if missing tags
           :action/priority (if (seq (:org/tags todo)) 0 3)}
          {:action/label    "delete-from-db"
           :action/on-click #(delete-from-db todo)
           :action/icon     fa/trash-alt-solid}
          {:action/label    "purge-file"
           :action/on-click #(purge-org-source-file todo)
           :action/icon     fa/trash-solid}
          {:action/label    (if (:todo/queued-at todo) "(un)queue-todo" "queue-todo")
           :action/on-click (fn [_] (if (:todo/queued-at todo) (unqueue-todo todo) (queue-todo todo)))
           :action/icon     (if (:todo/queued-at todo)
                              [:> HIMini/BoltSlashIcon {:class ["w-6" "h-6"]}]
                              [:> HIMini/BoltIcon {:class ["w-6" "h-6"]}])
           :action/priority 1}
          {:action/label    "requeue-todo"
           :action/on-click #(queue-todo todo)
           :action/icon
           [:> HIMini/ArrowPathIcon {:class ["w-6" "h-6"]}]
           :aciton/disabled (not
                              (and (not (#{:status/cancelled :status/done} status))
                                   (:todo/queued-at todo)))
           :action/priority 1}
          {:action/label    "start-todo"
           :action/on-click #(start-todo todo)
           :action/icon     [:> HIMini/PlayIcon {:class ["w-4" "h-6"]}]
           :action/disabled (#{:status/in-progress} status)
           ;; higher priority if queued
           :action/priority (if  (:todo/queued-at todo) 1 0)}
          {:action/label    "complete-todo"
           :action/on-click #(complete-todo todo)
           :action/icon     fa/check-circle-solid
           :action/disabled (#{:status/done} status)
           :action/priority (if (or (:todo/queued-at todo)
                                    (:status/in-progress todo)) 2 0)}
          {:action/label    "skip"
           :action/disabled (#{:status/skipped} status)
           :action/on-click #(skip-todo todo)
           :action/icon     [:> HIMini/ArchiveBoxIcon {:class ["w-4" "h-6"]}]}
          {:action/label    "clear-status"
           :action/on-click #(clear-status todo)
           :action/disabled (not status)
           :action/icon     [:> HIMini/XCircleIcon {:class ["w-4" "h-6"]}]}
          {:action/label    "cancel-todo"
           :action/on-click #(cancel-todo todo)
           :action/disabled (#{:status/cancelled} status)
           :action/icon     fa/ban-solid}
          ]
         (remove nil?)))))


#?(:cljs
   (defn garden-file->actions [item]
     [(open-in-journal-action item)
      {:action/label    "add-tag"
       :action/on-click (fn [_]
                          (let [res (js/prompt "Add tag")]
                            (when (seq res)
                              (add-tag item res))))
       :action/icon     fa/hashtag-solid}
      {:action/label    "purge-source-file"
       :action/on-click #(purge-org-source-file item)
       :action/icon     fa/trash-alt-solid}]))

#?(:cljs
   (defn repo->actions [item]
     [{:action/on-click #(delete-from-db item)
       :action/label    "delete-from-db"
       :action/icon     fa/trash-alt-solid}
      {:action/label    "ingest-commits"
       :action/on-click #(ingest-commits-for-repo item)
       :action/icon     [:> HIMini/ArrowDownOnSquareStackIcon {:class ["w-4" "h-6"]}]}]))

#?(:cljs
   (defn wallpaper->actions [item]
     [{:action/label    "set-wallpaper"
       :action/on-click #(set-wallpaper item)
       :action/icon     [:> HIMini/PhotoIcon {:class ["w-4" "h-6"]}]}
      {:action/label    "ingest-wallpapers"
       :action/on-click #(ingest-wallpapers)
       :action/icon     [:> HIMini/ArrowDownOnSquareStackIcon {:class ["w-4" "h-6"]}]}
      {:action/on-click #(delete-from-db item)
       :action/label    "delete-from-db"
       :action/icon     fa/trash-alt-solid}]))

#?(:cljs
   (defn ->actions [item]
     (->>
       (cond
         ;; todos
         (and (#{:type/garden} (:doctor/type item))
              (:org/status item))
         (todo->actions item)

         ;; garden note
         (#{:type/garden} (:doctor/type item))
         (garden-file->actions item)

         ;; repo actions
         (#{:type/repo} (:doctor/type item))
         (repo->actions item)

         ;; wallpaper actions
         (#{:type/wallpaper} (:doctor/type item))
         (wallpaper->actions item)

         :else
         [{:action/on-click #(delete-from-db item)
           :action/label    "delete-from-db"
           :action/icon     fa/trash-alt-solid}])
       (map-indexed vector)
       (map (fn [[i ax]]
              (merge
                {:action/class (colors/color-wheel-classes {:type :line :i i})}
                ax ;; let the ax overwrite/maintain a color
                ))))))
