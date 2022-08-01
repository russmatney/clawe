(ns clawe.workspaces
  (:require
   [defthing.defcom :refer [defcom] :as defcom]
   [defthing.defworkspace :as defworkspace]

   [ralphie.awesome :as awm]
   [ralphie.yabai :as yabai]
   [ralphie.rofi :as rofi]
   [ralphie.notify :as notify]

   clawe.defs.workspaces
   [clawe.client :as client]
   [clawe.config :as clawe.config]
   [clawe.doctor :as clawe.doctor]

   [clojure.string :as string]
   [ralphie.zsh :as zsh]
   [ralphie.tmux :as r.tmux]
   [wing.core :as w]
   [clawe.workspace :as workspace]))

(def home-dir (zsh/expand "~"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspace hydration
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO use a malli schema
(defn ->pseudo-workspace [wsp]
  (let [name (or (:awesome.tag/name wsp)
                 (:yabai.space/label wsp))

        index (or (:awesome.tag/index wsp)
                  (:yabai.space/index wsp))

        n (if-not (empty? name)
            name (str "fallback-name-" index))]
    (-> wsp
        (assoc :name n)
        ((fn [w] (merge w (defworkspace/workspace-defaults w)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; merge-awm-tags
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
                       (:workspace/title wsp)
                       awm-all-tags)
                     wsp)))

       include-unmatched?
       ((fn [wsps]
          (let [matched-tag-names (->> wsps (map :awesome.tag/name) (into #{}))
                unmatched-tags    (->> awm-all-tags
                                       (remove (comp matched-tag-names :awesome.tag/name)))
                pseudo-wsps       (->> unmatched-tags
                                       (map ->pseudo-workspace))]
            (concat wsps pseudo-wsps))))

       ;; unwrap if only one was passed
       is-map?
       first))))

(comment
  (->>
    (defworkspace/list-workspaces)
    (merge-awm-tags {:include-unmatched? true})
    (filter :awesome.tag/name)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; merge-yabai-spaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn merge-yabai-spaces
  ([wsps] (merge-yabai-spaces {} wsps))
  ([{:keys [include-unmatched? prefetched-spaces prefetched-windows]} wsps]
   (let [all-spaces               (or prefetched-spaces (yabai/query-spaces))
         all-spaces-by-label      (->> all-spaces
                                       (w/index-by :yabai.space/label))
         all-windows-by-space-idx (->> (or prefetched-windows (yabai/query-windows))
                                       (w/group-by :yabai.window/space))
         is-map?                  (map? wsps)
         include-unmatched?       (if is-map? false include-unmatched?)
         wsps                     (if is-map? [wsps] wsps)]
     (cond->> wsps
       true
       (map (fn [wsp]
              ;; depends on the :workspace/title matching the space label name
              ;; could also just be a unique id stored from open-workspace
              (merge (all-spaces-by-label (:workspace/title wsp))
                     wsp)))

       include-unmatched?
       ((fn [wsps]
          (let [matched-names  (->> wsps (map :yabai.space/label) (into #{}))
                unmatched-tags (->> all-spaces (remove (comp matched-names :yabai.space/label)))
                pseudo-wsps    (->> unmatched-tags (map ->pseudo-workspace))]
            (concat wsps pseudo-wsps))))

       true ;; could make yabai windows optional
       (map (fn [wsp]
              (let [windows (->> wsp :yabai.space/index all-windows-by-space-idx)]
                (assoc wsp :yabai/windows windows))))

       ;; unwrap if only one was passed
       is-map?
       first))))

(comment
  (->>
    (defworkspace/list-workspaces)
    (merge-yabai-spaces {:include-unmatched? true})
    (filter :yabai.space/label))

  (merge-yabai-spaces []))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Interactive workspace creation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn local-git-users []
  (let [h (str home-dir "/")]
    (->>
      (zsh/expand-many "~/*")
      (filter #(string/starts-with? % h))
      (map #(string/replace % h "")))))

(defn select-local-git-user []
  (rofi/rofi {:msg "Select git user"} (local-git-users)))

(defn path->rofi-obj [path]
  {:rofi/label (str "Install " path) :path path})

(defn do-install-workspaces
  "Walks through selecting one or more repos, converts them to workspaces,
  and add them to the db.

  ;; TODO add options to show all workspaces for all git users.
  Right now you need to know the git user to install a particular repo."
  []
  (let [git-user   (select-local-git-user)
        repo-paths (zsh/expand-many (str home-dir "/" git-user "/*"))
        all-option {:rofi/label (str "Install all " git-user " workspaces")
                    :some/id    "all-option"}
        selected   (->> (concat [all-option] (->> repo-paths (map path->rofi-obj)))
                        (rofi/rofi {:msg "Install workspace for which repo?"}))]
    (cond
      (nil? selected) (notify/notify "No repo selected")

      (= (:some/id all-option) (:some/id selected))
      (do
        (notify/notify (str "installing " (count repo-paths) " workspaces for") git-user)
        (println "repo-paths" repo-paths)
        ;; TODO notify w/ datoms-txed result when osx can handle it
        (defworkspace/install-repo-workspaces repo-paths)
        (notify/notify "installed all workspaces for" git-user))

      :else
      (do
        (notify/notify "installing workspace" (:path selected))
        ;; TODO notify w/ datoms-txed result when osx can handle it
        (defworkspace/install-repo-workspaces (:path selected))
        (notify/notify "Installed workspace" (:path selected))))

    ;; restart doctor db connection after transacting
    (clawe.doctor/db-restart-conn)))

(defcom install-workspaces do-install-workspaces)

(comment
  (do-install-workspaces))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn merge-db-workspaces
  ([wsps]
   (merge-db-workspaces {} wsps))
  ([_opts wsps]
   (let [is-map?          (map? wsps)
         wsps             (if is-map? [wsps] wsps)
         db-wsps-by-title (->> (defworkspace/latest-db-workspaces)
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
  (def --w
    (->>
      (defworkspace/list-workspaces)
      (filter (comp #{"clawe"} :name))
      first))
  (select-keys --w #{:workspace/title})

  (merge-db-workspaces --w))

(comment
  ({:a "a" :b "b"} {:b "b" :c "c"}))

;; TODO move to defworkspace, add tests
(defn db-with-merged-in-memory-workspaces
  "Supports cases where in-memory feats (like functions on wsps) are required.

  In-memory workspaces should always have a basic version in the db.
  If not, call `sync-workspaces-to-db` with no args."
  []
  (let [db-workspaces   (defworkspace/latest-db-workspaces)
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; tmux session merging
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn merge-tmux-sessions
  "assumes the workspace title and tmux session are the same"
  ([wsps] (merge-tmux-sessions {} wsps))
  ([_opts wsps]
   (when-let [sessions-by-name (try (r.tmux/list-sessions)
                                    (catch Exception _e
                                      (println "Tmux probably not running!")
                                      nil))]
     (->> wsps
          (map (fn [{:workspace/keys [title] :as wsp}]
                 (if-let [sesh (sessions-by-name title)]
                   (assoc wsp :tmux/session sesh)
                   wsp)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspaces fetchers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current-workspace-fast
  "Does not mix the current workspace with the db overwritable keys."
  ([] (current-workspace-fast))
  ;; TODO support awm
  ([{:keys [prefetched-windows]}]
   (let [yb-spc     (yabai/query-current-space)
         pseudo-wsp (->pseudo-workspace yb-spc)
         wsp        (if-let [wsp (defworkspace/get-workspace pseudo-wsp)]
                      wsp pseudo-wsp)
         ;; optional? part of yabai request?
         wsp        (merge-yabai-spaces
                      {:prefetched-spaces  (list yb-spc)
                       :prefetched-windows prefetched-windows}
                      wsp)]
     wsp)))

(comment
  (->
    (yabai/query-current-space)
    ->pseudo-workspace
    ;; :name
    defworkspace/get-workspace)
  (->>
    (defworkspace/list-workspaces)
    (filter (comp #{"clawe"} :name))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- workspaces-to-swap-indexes
  "Creates a sorted list of workspaces with an additional key: :new-index.
  This is in support of the `update-workspace-indexes` func below."
  []
  (->> (workspace/all-active)
       (map (fn [spc]
              (assoc spc :sort-key (str (if (:workspace/scratchpad spc) "z" "a") "-"
                                        (format "%03d" (or (:workspace/index spc) 0))))))
       ;; sort and map-indexed to set new_indexes
       (sort-by :sort-key)
       (map-indexed (fn [i wsp] (assoc wsp :new-index
                                       ;; lua indexes start at 1
                                       (+ i 1))))
       (remove #(= (:new-index %) (:workspace/index %)))))

(comment
  (workspaces-to-swap-indexes))

(defn update-workspace-indexes
  []
  (loop [wsps (workspaces-to-swap-indexes)]
    (let [wsp (some-> wsps first)]
      (when wsp
        (let [{:keys [new-index] :as wsp} (merge-awm-tags wsp)
              index                       (:workspace/index wsp)]
          (when (not= new-index index)
            (if (clawe.config/is-mac?)
              (yabai/swap-spaces-by-index index new-index)
              (awm/swap-tags-by-index index new-index)))
          ;; could be optimized....
          (recur (workspaces-to-swap-indexes)))))))

(comment
  (update-workspace-indexes)
  (workspaces-to-swap-indexes))

(defn do-yabai-correct-workspaces []
  ;; some what duplicated in clawe.restart...
  ;; maybe this belongs in c.rules...
  (yabai/set-space-labels)
  (yabai/destroy-unlabelled-empty-spaces)
  (update-workspace-indexes))

(defcom yabai-correct-workspaces
  (do-yabai-correct-workspaces))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Swapping Workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn drag-workspace
  "Drags the current awesome workspace in the given direction"
  [dir]
  (awm/drag-workspace (case dir "up" :drag/up "down" :drag/down)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; consolidate workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn consolidate-workspaces []
  (->>
    (workspace/all-active)
    (remove :awesome.tag/empty)
    (sort-by :workspace/index)
    (map-indexed
      (fn [new-index {:keys [workspace/title workspace/index]}]
        (let [new-index (+ 1 new-index)] ;; b/c lua is 1-based
          (if (== index new-index)
            (prn "nothing to do")
            (do
              (prn "swapping tags" {:title     title
                                    :idx       index
                                    :new-index new-index})
              (awm/swap-tags-by-index index new-index))))))))

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
    (workspace/all-active)
    (filter :awesome.tag/empty)
    (map
      (fn [it]
        (when-let [title (:workspace/title it)]
          (try
            (awm/delete-tag! title)
            (notify/notify "Deleted Tag" title)
            (catch Exception e e
                   (notify/notify "Error deleting tag" e))))))
    doall))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; New create workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-workspace
  "Creates a new tag and focuses it, and run the workspace's on-create hook."
  [wsp]
  (let [title (:workspace/title wsp)]

    (if (clawe.config/is-mac?)
      ;; TODO handle this space/label already existing
      (yabai/create-and-label-space
        {:space-label       title
         :focus             true
         :overwrite-labeled true})

      (do
        ;; create tag if none is found
        (when (not (awm/tag-exists? title))
          (awm/create-tag! title))
        ;; focus the tag
        (awm/focus-tag! title)))

    ;; notify
    (notify/notify (str "Created new workspace: " title))

    ;; create first client if it's not already there
    ;; NOTE might want this to be opt-in/out
    (when (or
            ;; was empty
            (:awesome.tag/empty wsp)
            ;; or had no tag
            (not (:awesome.tag/name wsp)))
      (client/create-client wsp))

    ;; update workspace indexes
    (update-workspace-indexes)

    ;; update topbar
    (clawe.doctor/update-topbar)

    ;; return the workspace
    wsp))

(defn wsp->rofi-opts
  [{:as wsp}]
  (let [title (:workspace/title wsp)
        dir   (:workspace/directory wsp)]
    {:rofi/label       title
     :rofi/description (when dir dir)}))

(defn workspace-rofi-options []
  (->> (workspace/all-defs) (map #(merge % (wsp->rofi-opts %)))))

(defn open-workspace-rofi-options []
  (->>
    (workspace-rofi-options)
    (map #(assoc % :rofi/on-select (fn [wsp]
                                     (create-workspace wsp))))))
