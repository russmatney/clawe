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
             [garden.db :as garden.db]
             [api.blog :as api.blog]]
       :cljs [[hiccup-icons.fa :as fa]
              [components.icons :as components.icons]
              [components.colors :as colors]
              ["@heroicons/react/20/solid" :as HIMini]
              [hooks.workspaces :as hooks.workspaces]
              [hiccup-icons.octicons :as octicons]])))

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

(defhandler ensure-uuid [item]
  (when-not (:org/id item)
    (org-crud.api/update! item {:org/id (random-uuid)}))
  :ok)

(defhandler update-todo [todo up]
  (org-crud.api/update! todo up)
  :ok)

(defn todo-set-new-status [it status]
  ;; TODO queue-todo as well - maybe just write to both places and cross your fingers
  ;; this, but needs the updated data as well?
  ;; (-> todo
  ;;     (assoc :todo/queued-at (System/currentTimeMillis))
  ;;     db/transact)
  (update-todo
    it (cond-> {:org/status status}
         (#{:status/in-progress} status) (assoc :org/tags "current")
         (#{:status/done} status)        (assoc :org/tags [:remove "current"])
         (not (:org/id it))              (assoc :org/id (random-uuid)))))

(defhandler cancel-todo [todo]
  (todo-set-new-status todo :status/cancelled)
  :ok)

(defhandler complete-todo [todo]
  (todo-set-new-status todo :status/done)
  :ok)

(defhandler start-todo [todo]
  (todo-set-new-status todo :status/in-progress)
  :ok)

(defhandler skip-todo [todo]
  (todo-set-new-status todo :status/skipped)
  :ok)

(defhandler clear-status [todo]
  ;; when an item shouldn't be a todo
  ;; TODO test, may not work yet
  ;; org-crud doesn't support it, and the db doesn't auto-retract attrs yet
  (org-crud.api/update! todo {:org/status nil})
  :ok)

(defhandler add-tag [item tag]
  (org-crud.api/update! item
                        (cond->
                            {:org/tags tag}
                          (not (:org/id item))
                          (assoc :org/id (random-uuid))))
  :ok)

(defhandler increase-priority [todo]
  (let [priority
        (if (contains? #{"A" "B" "C" nil} (:org/priority todo))
          (case (:org/priority todo)
            "A" "A"
            "B" "A"
            "C" "B"
            nil "C")
          (do
            (println "inc-pri - unexpected priority found, overwriting:" (:org/priority todo))
            "C"))]
    (org-crud.api/update! todo
                          (cond->
                              {:org/priority priority}
                            (not (:org/id todo))
                            (assoc :org/id (random-uuid))))
    :ok))

(defhandler decrease-priority [todo]
  (let [priority
        (if (contains? #{"A" "B" "C"} (:org/priority todo))
          (case (:org/priority todo)
            "A" "B"
            "B" "C"
            "C" nil)
          (do
            (println "dec-pri - unexpected priority found, overwriting:" (:org/priority todo))
            nil))]
    (org-crud.api/update! todo
                          (cond->
                              {:org/priority priority}
                            (not (:org/id todo))
                            (assoc :org/id (random-uuid))))
    :ok))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; action lists
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn open-in-journal-action [item]
     {:action/label    "open-in-journal"
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
         [;; open in emacs
          (assoc (open-in-journal-action todo)
                 :action/priority -10)

          ;; ensure uuid
          {:action/label    "ensure-uuid"
           :action/on-click #(ensure-uuid todo)
           :action/icon
           [:> HIMini/FingerPrintIcon {:class ["w-6" "h-6"]}]
           :action/disabled (:org/id todo)
           :action/priority -10} ;; low-prority

          ;; priority
          {:action/label    "increase-priority"
           :action/on-click #(increase-priority todo)
           :action/disabled (or (not status)
                                (#{"A"} (:org/priority todo)))
           :action/icon     fa/chevron-circle-up-solid
           :action/priority 1}
          {:action/label    "decrease-priority"
           :action/on-click #(decrease-priority todo)
           :action/disabled (or (not status)
                                (not (:org/priority todo)))
           :action/icon     fa/chevron-circle-down-solid
           :action/priority 1}

          ;; start-todo
          {:action/label       "start-todo"
           :action/description "
- Adds 'current' tag
- sets :status/in-progress
- ensures uuid
;; TODO queue it
;; TODO ensure pomodoro
;; TODO offer/suggest/M-x to change-mode
"
           :action/on-click    (fn [_] (start-todo todo))
           :action/icon        #_fa/hashtag-solid
           [:> HIMini/PlayIcon {:class ["w-4" "h-6"]}]
           ;; disabled if already tagged current or already completed/skipped
           :action/disabled
           (or ((or (some->> todo :org/tags (into #{})) #{}) "current")
               (#{:status/done :status/cancelled :status/skipped}
                 status))
           ;; higher priority if priority set
           :action/priority    (if (:org/priority todo) 3 0)}

          ;; add-tag
          {:action/label    "add-tag"
           :action/on-click (fn [_]
                              (let [res (js/prompt "Add tag")]
                                (when (seq res)
                                  (add-tag todo res))))
           :action/icon     fa/hashtag-solid
           ;; higher priority if missing tags
           :action/priority (if (seq (:org/tags todo)) 0 1)}

          ;; db-only commands: delete-from-db, purge-file (for reingestion)
          {:action/label    "delete-from-db"
           :action/on-click #(delete-from-db todo)
           :action/icon     fa/trash-alt-solid
           :action/disabled (not (:db/id todo))}
          {:action/label    "purge-file"
           :action/on-click #(purge-org-source-file todo)
           :action/icon     fa/trash-solid
           :action/disabled (not (:db/id todo))}

          ;; queue toggle
          {:action/label    (if (:todo/queued-at todo) "(un)queue-todo" "queue-todo")
           :action/on-click (fn [_] (if (:todo/queued-at todo)
                                      (unqueue-todo todo)
                                      (queue-todo todo)))
           :action/icon     (if (:todo/queued-at todo)
                              [:> HIMini/BoltSlashIcon {:class ["w-6" "h-6"]}]
                              [:> HIMini/BoltIcon {:class ["w-6" "h-6"]}])
           :action/disabled (not (:org/id todo))
           :action/priority 1}

          ;; requeue
          {:action/label    "requeue-todo"
           :action/on-click #(queue-todo todo)
           :action/icon
           [:> HIMini/ArrowPathIcon {:class ["w-6" "h-6"]}]
           :action/disabled
           (not
             (and (not (#{:status/cancelled :status/done} status))
                  (:todo/queued-at todo)))
           :action/priority 1}

          ;; finish todo
          {:action/label    "mark-complete"
           :action/on-click #(complete-todo todo)
           :action/icon     fa/check-circle-solid
           :action/disabled (#{:status/done} status)
           :action/priority (if (or (:todo/queued-at todo)
                                    (:status/in-progress todo)) 2 0)}

          ;; mark-skipped
          {:action/label    "mark-skipped"
           :action/disabled (#{:status/skipped} status)
           :action/on-click #(skip-todo todo)
           :action/icon     [:> HIMini/ArchiveBoxIcon {:class ["w-4" "h-6"]}]}

          ;; mark-cancelled
          {:action/label    "mark-cancelled"
           :action/on-click #(cancel-todo todo)
           :action/disabled (#{:status/cancelled} status)
           :action/icon     fa/ban-solid}

          ;; mark not-a-todo (clear todo status)
          {:action/label    "clear-todo-status"
           :action/on-click #(clear-status todo)
           :action/disabled (not status)
           :action/icon     [:> HIMini/XCircleIcon {:class ["w-4" "h-6"]}]}]
         (remove nil?)))))

(defhandler publish-note [item]
  (api.blog/publish item)
  :ok)

(defhandler unpublish-note [item]
  (api.blog/unpublish item)
  :ok)

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
       :action/icon     fa/trash-alt-solid}
      {:action/label    "Publish"
       :action/on-click (fn [_] (publish-note item))
       :action/priority 1
       :action/disabled (:blog/published item)}
      {:action/label    "Unpublish"
       :action/on-click (fn [_] (unpublish-note item))
       :action/priority 1
       :action/disabled (not (:blog/published item))}]))

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
   (defn workspace->actions [wsp]
     [{:action/label "Close"
       :action/icon  octicons/trash
       :action/on-click
       #(hooks.workspaces/close-workspaces wsp)}]))

#?(:cljs
   (defn client->actions [client]
     [{:action/label    "focus"
       :action/icon     nil
       :action/on-click #(hooks.workspaces/focus-client client)}]))

#?(:cljs
   (defn ->actions
     ([item] (->actions item nil))
     ([item actions]
      (->>
        (cond
          (seq actions)
          actions

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

          (:db/id item)
          [{:action/on-click #(delete-from-db item)
            :action/label    "delete-from-db"
            :action/icon     fa/trash-alt-solid}]

          :else [])
        (map-indexed vector)
        (map (fn [[i ax]]
               (merge
                 {:action/class (colors/color-wheel-classes {:type :line :i i})}
                 ax ;; let the ax overwrite/maintain a color
                 )))))))
