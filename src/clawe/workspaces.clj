(ns clawe.workspaces
  (:require
   [defthing.defcom :refer [defcom] :as defcom]
   [defthing.defworkspace :as defworkspace]
   [ralphie.awesome :as awm]
   [ralphie.rofi :as rofi]
   [ralphie.git :as r.git]
   [ralphie.notify :as notify]

   [clawe.db.core :as db]

   ;; be sure to require all workspaces here
   ;; otherwise (all-workspaces) will be incomplete from consumers like doctor
   [clawe.defs.workspaces :as defs.workspaces]
   clawe.defs.local.workspaces

   [clawe.workspaces.create :as wsp.create]
   [clojure.string :as string]
   [ralphie.zsh :as zsh]))

(defn update-topbar []
  (slurp "http://localhost:3334/topbar/update"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspace helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspace-name [wsp]
  (:workspace/title wsp))

(defn workspace-repo [wsp]
  (or
    (:git/repo wsp)
    (:workspace/directory wsp)
    (:org.prop/directory wsp)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspace hydration
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn apply-git-status
  "Performs a git-status check and updates the passed workspace.

  Workspaces need to opt-in via :git/check-status?, and should
  specify a repo with a .git directory via :workspace/directory."
  [wsp]
  (if (:git/check-status? wsp)
    (let [dir (workspace-repo wsp)]
      (if (r.git/repo? dir)
        (merge wsp (r.git/status dir))
        wsp))
    wsp))

(defn tag->pseudo-workspace
  [{:as tag :keys [awesome.tag/name
                   awesome.tag/index
                   ]}]
  (let [n (str name "-" index)]
    ;; NOTE this :workspace/title is used to get/find the key in the clawe-db
    (-> tag
        (assoc :name n)
        (assoc :workspace/title n))))

(defn merge-awm-tags
  "Fetches all awm tags and merges those with matching
  `tag-name == :workspace/title` into the passed workspaces.

  Fetching awm tags one at a time can be a bit slow,
  so we just get them all up-front.

  If :include-unmatched? is truthy, tags without matching workspaces
  will have basic workspaces created and included.
  Note that this only applies if a list of wsps is passed.
  "
  ([wsps]
   (merge-awm-tags {} wsps))
  ([{:keys [include-unmatched?]} wsps]
   (let [awm-all-tags       (awm/fetch-tags)
         is-map?            (map? wsps)
         include-unmatched? (if is-map? false include-unmatched?)
         wsps               (if is-map? [wsps] wsps)]
     (cond->> wsps
       true
       (map (fn [wsp]
              ;; depends on the :workspace/title matching the awm tag name
              ;; could also just be a unique id stored from open-workspace
              (merge (awm/tag-for-name
                       (workspace-name wsp)
                       awm-all-tags)
                     wsp)))

       include-unmatched?
       ((fn [wsps]
          (let [matched-tag-names (->> wsps (map :awesome.tag/name) (into #{}))
                unmatched-tags    (->> awm-all-tags
                                       (remove (comp matched-tag-names :awesome.tag/name)))
                pseudo-wsps       (->> unmatched-tags
                                       (map tag->pseudo-workspace))]
            (concat wsps pseudo-wsps))))

       ;; unwrap if only one was passed
       is-map?
       first))))

(comment
  (->>
    (defworkspace/list-workspaces)
    (merge-awm-tags {:include-unmatched? true})
    (filter :awesome.tag/name))
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; db workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-db-workspace
  "Assumes the first match is the one we want.
  TODO May one day need to fetch latest by sorting, if we end with multiple ents somehow
  "
  [w]
  (let [title (cond (string? w) w
                    (map? w)    (:workspace/title w))]
    (some->>
      (db/query
        '[:find (pull ?e [*])
          :in $ ?workspace-title
          :where
          [?e :workspace/title ?workspace-title]]
        title)
      ffirst)))

(defn latest-db-workspaces []
  (let [wsps
        (->>
          (db/query
            '[:find (pull ?e [*])
              :where
              [?e :workspace/title ?workspace-title]])
          (map first))]
    (->> wsps
         (group-by :workspace/title)
         (map (fn [[_k vs]]
                (->> vs
                     (sort-by :workspace/updated-at)
                     first)))))
  )

(comment
  (->>
    (latest-db-workspaces)
    count
    )
  (->>
    (defworkspace/list-workspaces)
    count)


  (type 6)
  (type "B")
  (type true)
  )

(def supported-types (->> [6 "hi" true]
                          (map type)
                          (into #{})))

(defn supported-type-keys [m]
  (->>
    m
    (filter (fn [[_k v]]
              (supported-types (type v))))
    (map first)))

(comment
  (supported-type-keys {:hello     "goodbye"
                        :some-int  5
                        :some-bool false
                        :some-fn   (fn [] (print "complexity!"))
                        })
  )

;; TODO roundtrip test these
(defn update-db-workspace [w]
  (let [existing   (get-db-workspace w)
        db-id      (:db/id existing)
        basic-keys (supported-type-keys w)]
    (db/transact [(cond-> w
                    true
                    (select-keys basic-keys)

                    true
                    (assoc :workspace/updated-at (System/currentTimeMillis))

                    (and db-id (not (:db/id w)))
                    (assoc :db/id db-id))])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Interactive workspace creation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn local-git-users []
  (->>
    (zsh/expand-many "~/*")
    (filter #(string/starts-with? % "/home/russ/"))
    (map #(string/replace % "/home/russ/" ""))))

(defn select-local-git-user []
  (rofi/rofi {:msg "Select git user"} (local-git-users)))

(defcom install-workspaces
  "Walks through selecting one or more repos, converts them to workspaces,
  and add them to the db."

  (let [git-user   (select-local-git-user)
        wsps       (defs.workspaces/build-workspaces-for-git-user git-user)
        all-option {:rofi/label (str "Install all " git-user " workspaces")
                    :some/id    "all-option"}
        selected   (->> (concat [all-option]
                                (->> wsps
                                     (map (fn [wsp]
                                            (assoc wsp :rofi/label
                                                   (str "Install " git-user "/" (:workspace/title wsp)))))))
                        (rofi/rofi {:msg "Install workspace for which repo?"}))]
    (if (= (:some/id all-option) (:some/id selected))
      (do
        (notify/notify "installing all workspaces for" git-user)
        (let [ct (count (map update-db-workspace wsps))]
          (notify/notify (str "Installed " ct " workspaces"))))
      (do
        (notify/notify "installing workspace"
                       (str git-user "/" (:workspace/title selected)))
        (update-db-workspace selected)
        (notify/notify "Installed workspace")))

    ;; TODO maybe rewrite/update awm rules?

    ))

(comment

  (defcom/exec install-workspaces)

  (def w
    (->>
      (defworkspace/list-workspaces)
      (filter (comp (fnil #(string/includes? % "aave") "") :workspace/title))
      first
      ))
  (update-db-workspace w)

  (->>
    (latest-db-workspaces)
    ;; (filter (comp (fnil #(string/includes? % "aave") "") :workspace/title))
    (map :workspace/title)
    )

  (get-db-workspace w)

  (defs.workspaces/load-workspaces ["teknql"])
  (defs.workspaces/load-workspaces
    ["russmatney"
     "teknql"
     "borkdude"
     "godot"])

  (defs.workspaces/load-workspaces
    [["urbint" #{"grid" "lens" "gitops"
                 "worker-safety-service"
                 "worker-safety-client"}]])

  (->>
    (defworkspace/list-workspaces)
    (filter (comp (fnil #(string/includes? % "urbint") "") :workspace/directory))
    count)

  ;; load workspaces into memory
  (defs.workspaces/load-workspaces [["russmatney" #{"dotfiles"}]])
  (defs.workspaces/load-workspaces ["teknql"])

  ;; write in-memory workspaces to the db
  (->> (defworkspace/list-workspaces) (map update-db-workspace))

  (->> (defworkspace/list-workspaces) count)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn merge-db-workspaces
  ([wsps]
   (merge-db-workspaces {} wsps))
  ([_opts wsps]
   (let [is-map?          (map? wsps)
         wsps             (if is-map? [wsps] wsps)
         db-wsps-by-title (->> (latest-db-workspaces)
                               (map (fn [w]
                                      [(:workspace/title w) w]))
                               (into {}))]
     (cond->> wsps
       true (map (fn [wsp]
                   (if-let [db-wsp (db-wsps-by-title (:workspace/title wsp))]
                     ;; TODO who should overwrite?
                     (merge db-wsp wsp)
                     wsp)))

       is-map?
       first))))

(comment
  (select-keys --w #{:workspace/title})
  (def --w
    (->>
      (defworkspace/list-workspaces)
      (filter (comp #{"clawe"} :name))
      first))

  (get-db-workspace --w)
  (update-db-workspace --w)
  (merge-db-workspaces --w)
  )

(comment
  ({:a "a" :b "b"} {:b "b" :c "c"})
  )


;; TODO tests for this
(defn db-with-merged-in-memory-workspaces []
  (let [db-workspaces   (latest-db-workspaces)
        ->title         (fn [wsp] (:workspace/title wsp))
        by-title        (fn [wsps] (->> wsps
                                        (map (fn [wsp] [(->title wsp) wsp]))
                                        (into {})))
        in-mem-wspcs    (defworkspace/list-workspaces)
        in-mem-by-title (by-title in-mem-wspcs)
        db-by-title     (by-title db-workspaces)

        db-only (->> db-workspaces
                     (remove (comp in-mem-by-title ->title)))

        in-mem-only (->> in-mem-wspcs
                         (remove (comp db-by-title ->title)))

        merged (->> db-workspaces
                    (filter (comp in-mem-by-title ->title))
                    (map (fn [db-wsp]
                           (let [in-mem-wsp (in-mem-by-title (->title db-wsp))]
                             (merge db-wsp in-mem-wsp)))))]
    (concat
      merged
      db-only
      in-mem-only)))

(comment
  (->>
    (db-with-merged-in-memory-workspaces)
    count)

  (->>
    (db-with-merged-in-memory-workspaces)
    (filter (comp (fnil #(string/includes? % "urbint") "") :workspace/directory))
    count
    )
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspaces fetchers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current-workspace
  "Returns the current workspace.
  Sorts scratchpads to the end, intending to return the 'base' workspace,
  which is usually what we want."
  []
  (some->> (awm/current-tag-names)
           (map get-db-workspace)
           ;; assumes local overwrites have hit db already
           ;; otherwise we may need to merge the static wsps in here
           (sort-by :workspace/scratchpad)
           ;; (take 1)
           ;; merge-awm-tags
           first))

(comment
  (->> (awm/current-tag-names)
       (map defworkspace/get-workspace)
       (sort-by :workspace/scratchpad))
  (current-workspace)

  (some->> (awm/current-tag-names)
           (map get-db-workspace)
           ;; assumes local overwrites have hit db already
           ;; otherwise we may need to merge the static wsps in here
           (sort-by :workspace/scratchpad)
           ;; (take 1)
           ;; merge-awm-tags
           first)
  )

(defn all-workspaces-fast
  "Returns all defs.workspaces, without any awm data"
  []
  (->>
    (defworkspace/list-workspaces)))

(defn all-workspaces
  "Returns all defs.workspaces, merged with awesome tags."
  []
  ;; TODO why don't `load-workspaces` wsps get included here? can it be tested?
  (->>
    (db-with-merged-in-memory-workspaces)
    (merge-awm-tags {:include-unmatched? true})
    ;; merge-db-workspaces
    ;; (map apply-git-status)
    ))

(comment
  (set! *print-length* 10)
  (->>
    (all-workspaces)
    count)
  (->>
    (db-with-merged-in-memory-workspaces)
    (merge-awm-tags {:include-unmatched? true}))
  (->>
    (defworkspace/list-workspaces)
    (merge-awm-tags {:include-unmatched? true})
    merge-db-workspaces
    count)
  (->>
    (defworkspace/list-workspaces)
    (merge-awm-tags {:include-unmatched? true})
    merge-db-workspaces
    count
    )
  )

(defn for-name [name]
  (some->>
    (all-workspaces)
    (filter (comp #{name} workspace-name))
    first))

(comment
  (for-name "ralphie"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspaces-to-swap-indexes
  []
  (->> (all-workspaces)
       (filter :awesome.tag/name)
       (map (fn [spc]
              (assoc spc :sort-key (str (if (:workspace/scratchpad spc) "z" "a") "-"
                                        (format "%03d" (or (:awesome.tag/index spc)
                                                           0))))))
       ;; sort and map-indexed to set new_indexes
       (sort-by :sort-key)
       (map-indexed (fn [i wsp] (assoc wsp :new-index
                                       ;; lua indexes start at 1
                                       (+ i 1))))
       (remove #(= (:new-index %) (:awesome.tag/index %)))))

(comment
  (workspaces-to-swap-indexes)
  )

(defn update-workspace-indexes
  []
  (loop [wsps (workspaces-to-swap-indexes)]
    (let [wsp (some-> wsps first)]
      (when wsp
        (let [{:keys             [new-index]
               :awesome.tag/keys [index]} (merge-awm-tags wsp)]
          (when (not= new-index index)
            (awm/awm-cli {:quiet? true}
                         (str
                           "local tags = awful.screen.focused().tags;"
                           "local tag = tags[" index "];"
                           "local tag2 = tags[" new-index "];"
                           "tag:swap(tag2);")))
          ;; could be optimized....
          (recur (workspaces-to-swap-indexes)))))))

(comment
  (update-workspace-indexes)
  (workspaces-to-swap-indexes)

  (awm/awm-cli {:quiet? false}
               (str
                 "local tags = awful.screen.focused().tags;"
                 "local tag = tags[" 2 "];"
                 "local tag2 = tags[" 1 "];"
                 "tag:swap(tag2);"))
  )

;; TODO the rest of this should be refactored into ralphie.awesome or clawe.defs.bindings
;; maybe the workspace functions stay, but should dispatch to whatever window manager

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Swapping Workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn drag-workspace
  "Drags the current awesome workspace in the given direction"
  [dir]
  (let [up?   (= dir "up")
        down? (= dir "down")]
    (if (or up? down?)
      (awm/awm-cli
        {:quiet? true}
        (str
          "tags = awful.screen.focused().tags; "
          "current_index = s.selected_tag.index; "
          "new_index = current_index " (cond up? "+ 1" down? "- 1" ) "; "
          "new_tag = tags[new_index]; "
          "if new_tag then s.selected_tag:swap(new_tag) end; "
          ))
      (notify/notify "drag-workspace called without 'up' or 'down'!"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; consolidate workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn consolidate-workspaces []
  (->>
    (all-workspaces)
    (remove :awesome.tag/empty)
    (sort-by :awesome.tag/index)
    (map-indexed
      (fn [new-index {:keys [awesome.tag/name awesome.tag/index]}]
        (let [new-index (+ 1 new-index)] ;; b/c lua is 1-based
          (if (== index new-index)
            (prn "nothing to do")
            (do
              (prn "swapping tags" {:name      name
                                    :idx       index
                                    :new-index new-index})
              (awm/awm-cli {:quiet? true}
                           (str "local tag = awful.tag.find_by_name(nil, \"" name "\");"
                                "local tags = awful.screen.focused().tags;"
                                "local tag2 = tags[" new-index "];"
                                "tag:swap(tag2);")))))))))

(defcom consolidate-workspaces-cmd
  "Groups active workspaces closer together"
  "Moves active workspaces to the front of the list."
  (do
    (consolidate-workspaces)
    (update-workspace-indexes)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clean up workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clean-workspaces
  "Closes workspaces with 0 clients."
  []
  (notify/notify {:subject "Cleaning up workspaces"})
  (->>
    (all-workspaces)
    (filter :awesome.tag/empty)
    (map
      (fn [it]
        (when-let [name (:awesome.tag/name it)]
          (try
            (awm/delete-tag! name)
            (notify/notify "Deleted Tag" name)
            (catch Exception e e
                   (notify/notify "Error deleting tag" e))))))
    doall))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; New create workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-workspace
  "Creates a new tag and focuses it, and run the workspace's on-create hook."
  [wsp]
  (let [name (workspace-name wsp)]

    ;; create tag if none is found
    (when (not (awm/tag-exists? name))
      (awm/create-tag! name))

    ;; focus the tag
    (awm/focus-tag! name)

    ;; run on-create hook
    ;; (when-let [f (:workspace/on-create wsp)]
    ;;   (println "found on-create" f)
    ;;   (f wsp))

    ;; notify
    (notify/notify (str "Created new workspace: " name))

    ;; create first client if it's not already there
    ;; NOTE might want this to be opt-in/out
    (when (or
            ;; was empty
            (:awesome.tag/empty wsp)
            ;; or had no tag
            (not (:awesome.tag/name wsp)))
      (wsp.create/create-client wsp))

    ;; update workspace indexes
    (update-workspace-indexes)

    ;; update topbar
    (update-topbar)

    ;; return the workspace
    wsp))

(comment
  (awm/tag-for-name "ralphie")
  (awm/create-tag! "ralphie")
  (->
    "ralphie"
    (for-name)
    (create-workspace)))

(defn wsp->repo-and-status-label
  [{:as   wsp
    :keys [git/dirty? git/needs-push? git/needs-pull?]}]
  (let [default-name "noname"
        name         (or (workspace-name wsp) default-name)
        repo         (workspace-repo wsp)]
    (when (= name default-name)
      (println "wsp without name" wsp))

    (str
      "<span>" (or name default-name) " </span> "
      (when repo (str "<span color='gray' size='small'>" repo "</span> "))
      (when dirty? (str "<span color='#88aadd' size='small'>" "#dirty" "</span> "))
      (when needs-push? (str "<span color='#aa88ee' size='small'>" "#needs-push" "</span> "))
      (when needs-pull? (str "<span color='#38b98a' size='small'>" "#needs-pull" "</span> ")))))

(defn workspace-rofi-options []
  (->> (all-workspaces)
       (map #(assoc % :rofi/label (wsp->repo-and-status-label %)))))

(defn open-workspace-rofi-options []
  (->>
    (workspace-rofi-options)
    (map #(assoc % :rofi/on-select (fn [wsp]
                                     (create-workspace wsp)
                                     )))))


(defn select-workspace
  "Opens a list of workspaces in rofi.
  Returns the selected workspace."
  []
  (rofi/rofi
    {:msg "New Workspace Name?"}
    (workspace-rofi-options)))

(comment
  (select-workspace)

  (repeat 100)

  (workspace-rofi-options)

  )

(defn open-workspace
  ([] (open-workspace nil))
  ([name]
   ;; select and create
   (if name
     (-> name for-name create-workspace)
     ;; no name passed, get from rofi
     (some-> (select-workspace) create-workspace))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; toggle workspace names
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom toggle-scratchpad-names
  (do
    (awm/awm-cli "_G.toggle_show_scratchpad_names();")
    (update-workspace-indexes)))
