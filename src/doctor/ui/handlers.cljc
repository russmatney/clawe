(ns doctor.ui.handlers
  (:require
   [plasma.core :refer [defhandler]]
   [dates.tick :as dates.tick]
   #?@(:clj [[garden.core :as garden]
             [ralphie.emacs :as emacs]
             [git.core :as git]
             [chess.core :as chess]
             [screenshots.core :as screenshots]
             [clips.core :as clips]
             [chess.db :as chess.db]
             [wallpapers.core :as wallpapers]
             [clawe.wm :as wm]
             [db.core :as db]
             [datascript.core :as d]
             [org-crud.api :as org-crud.api]
             [garden.db :as garden.db]
             [api.blog :as api.blog]
             [blog.db :as blog.db]
             [api.todos :as api.todos]
             [api.pomodoros :as api.pomodoros]
             [taoensso.timbre :as log]]
       :cljs [[hiccup-icons.fa :as fa]
              [components.icons :as components.icons]
              [components.colors :as colors]
              ["@heroicons/react/20/solid" :as HIMini]
              [hooks.workspaces :as hooks.workspaces]
              [hiccup-icons.octicons :as octicons]
              [doctor.ui.db :as ui.db]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; db items
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler delete-from-db [item]
  (log/info "deleting item from db" item)
  (db/retract-entities (:db/id item))
  :ok)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; garden
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler refresh-blog-db []
  (blog.db/refresh-notes)
  :ok)

(defhandler reingest-todos []
  (api.todos/reingest-todos)
  :ok)

(defhandler ingest-garden-full []
  (garden.db/sync-all-garden-files)
  :ok)

(defhandler ingest-garden-latest []
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
  (log/info "purging-org-source-file from db" (:org/source-file item))
  (when (:org/source-file item)
    ;; TODO rewrite to delete without query, using an identifier
    (->>
      (db/query '[:find ?e
                  :in $ ?source-file
                  :where
                  ;; TODO isn't this on multiple entities (children)?
                  [?e :org/source-file ?source-file]]
                (:org/source-file item))
      (map first)
      db/retract-entities))
  :ok)

(defhandler add-tag [item tag]
  (log/debug "adding tag to item" tag (:org/name-string item))
  (when (:db/id item)
    (db/transact {:db/id (:db/id item) :org/tags
                  (into (:org/tags item) tag)}))
  (org-crud.api/update! item {:org/tags tag})
  :ok)

(defhandler remove-tag [item tag]
  (db/retract! [:db/retract (:db/id item) :org/tags tag])
  (org-crud.api/update! item {:org/tags [:remove tag]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; screenshots
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler ingest-screenshots []
  (screenshots/ingest-screenshots)
  :ok)

(defhandler ingest-clips []
  (clips/ingest-clips)
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
  (log/info "ingesting commits for repo" repo)
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
;; pomodoros
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler pomodoro-start-new []
  (api.pomodoros/start-new)
  :ok)

(defhandler pomodoro-end-current []
  (api.pomodoros/end-current)
  :ok)

#?(:cljs
   (defn pomodoro-actions [conn]
     (let [{:keys [current]} (ui.db/pomodoro-state conn)]
       [(if current
          {:action/label    "End"
           :action/on-click #(pomodoro-end-current)
           :action/icon     octicons/stop16}
          {:action/label    "Start"
           :action/on-click #(pomodoro-start-new)
           :action/icon     octicons/stopwatch16})])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; todos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler clear-current-todos []
  (api.todos/clear-current-todos)
  :ok)

(defhandler queue-todo [todo]
  (let [new-id (random-uuid)]
    (log/debug "queuing todo")
    (when-not (:org/id todo)
      (org-crud.api/update! todo {:org/id new-id})
      (log/debug "added id to org"))
    (cond-> todo
      (:org/id todo) (assoc :org/id new-id)
      true           (assoc :todo/queued-at (System/currentTimeMillis))
      true           db/transact)
    (log/debug "transacted new item"))
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
         (#{:status/in-progress} status)
         (cond->
             (not (:todo/queued-at it))
           (assoc :todo/queued-at (dates.tick/now))
           true (assoc :org/tags "current"))
         (#{:status/done :status/not-started
            :status/skipped :status/cancelled}
           status)
         (assoc :org/tags [:remove "current"])
         (not (:org/id it)) (assoc :org/id (random-uuid)))))

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

(defhandler mark-not-started [todo]
  (todo-set-new-status todo :status/not-started)
  :ok)

(defhandler clear-status [todo]
  ;; when an item shouldn't be a todo
  ;; TODO test, may not work yet
  ;; org-crud doesn't support it, and the db doesn't auto-retract attrs yet
  (org-crud.api/update! todo {:org/status nil})
  :ok)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; tag crud
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler remove-priority [item]
  (when (:db/id item)
    (->> [[:db.fn/retractAttribute (:db/id item) :org/priority]]
         (d/transact db/*conn*)))
  (org-crud.api/update! item {:org/priority nil}))

(defhandler increase-priority [todo]
  (let [priority
        (if (contains? #{"A" "B" "C" nil} (:org/priority todo))
          (case (:org/priority todo)
            "A" "A"
            "B" "A"
            "C" "B"
            nil "C")
          (do
            (log/warn "inc-pri - unexpected priority found, overwriting:" (:org/priority todo))
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
            (log/warn "dec-pri - unexpected priority found, overwriting:" (:org/priority todo))
            nil))]
    (when-not priority
      (->> [[:db.fn/retractAttribute (:db/id todo) :org/priority]]
           (d/transact db/*conn*)))
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

          ;; mark-not-started
          {:action/label    "mark-not-started"
           :action/on-click #(mark-not-started todo)
           :action/disabled (#{:status/not-started} status)
           :action/icon     fa/box-open-solid}

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
     [{:action/label    "Focus"
       :action/icon     octicons/pin16
       :action/on-click #(hooks.workspaces/focus-workspace wsp)}
      {:action/label "Close"
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
      (let [inferred (:actions/inferred item)
            item     (dissoc item :actions/inferred)]
        (->>
          (cond
            ;; huh? should these be concated with the defaults?
            (seq actions)
            actions

            ;; workspace
            (:workspace/index item)
            (workspace->actions item)

            ;; client
            (:client/id item (:client/window-title item))
            (client->actions item)

            ;; todos
            (#{:type/todo} (:doctor/type item))
            (todo->actions item)

            ;; garden note
            (#{:type/note} (:doctor/type item))
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
          (concat (or inferred []))
          (map-indexed vector)
          (map (fn [[i ax]]
                 (merge
                   {:action/class (colors/color-wheel-classes {:type :line :i i})}
                   ax ;; let the ax overwrite/maintain a color
                   ))))))))
