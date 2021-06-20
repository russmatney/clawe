(ns clawe.workspaces
  (:require
   [clawe.defs.workspaces :as defs.wsp]
   [clawe.awesome :as awm]
   [clawe.workspaces.create :as wsp.create]
   [defthing.defcom :refer [defcom] :as defcom]
   [ralphie.rofi :as rofi]
   [ralphie.awesome :as r.awm]
   [ralphie.git :as r.git]
   [ralphie.item :as item]
   [ralphie.notify :as notify]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspace helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspace-name [wsp]
  (:workspace/title wsp))

(defn workspace-repo [wsp]
  (or (:workspace/directory wsp)
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

(defn merge-awm-tags
  "Fetches all awm tags and merges those with matching
  `tag-name == :workspace/title` into the passed workspaces.

  Fetching awm tags one at a time can be a bit slow,
  so we just get them all up-front."
  [wsps]
  (let [awm-all-tags (r.awm/all-tags)
        is-map?      (map? wsps)
        wsps         (if is-map? [wsps] wsps)]
    (cond->> wsps
      true
      (map (fn [wsp]
             (merge (r.awm/workspace-for-name
                      (workspace-name wsp)
                      awm-all-tags)
                    wsp)))

      ;; unwrap if only one was passed
      is-map?
      first)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspaces fetchers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current-workspace
  "Returns the current workspace.
  Sorts scratchpads to the end, intending to return the 'base' workspace,
  which is usually what we want."
  []
  (some->> (r.awm/current-tag-names)
           (map defs.wsp/get-workspace)
           (sort-by :workspace/scratchpad)
           ;; (take 1)
           ;; merge-awm-tags
           first))

(comment
  (->> (r.awm/current-tag-names)
       (map defs.wsp/get-workspace)
       (sort-by :workspace/scratchpad)))

(defn all-workspaces
  "Returns all defs.workspaces, merged with awesome tags."
  []
  (->>
    (defs.wsp/list-workspaces)
    merge-awm-tags))

(defn for-name [name]
  (some->>
    (all-workspaces)
    (filter (comp #{name} workspace-name))
    first))

(comment
  (for-name "ralphie"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Active workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active-workspaces
  "Pulls workspaces to show in the workspaces-widget."
  []
  (->> (all-workspaces)
       (filter :awesome/tag)
       (map apply-git-status)
       (map (fn [spc]
              {;; consider flags for is-scratchpad/is-app/is-repo
               :name          (:awesome/name spc)
               :awesome_index (:awesome/index spc)
               :key           (:workspace/key spc)
               :fa_icon_code  (when-let [code (:workspace/fa-icon-code spc)]
                                (str "\\u{" code "}"))
               :scratchpad    (:workspace/scratchpad spc)
               :selected      (:awesome/selected spc)
               :empty         (:awesome/empty spc)
               :dirty         (:git/dirty? spc)
               :needs_pull    (:git/needs-pull? spc)
               :needs_push    (:git/needs-push? spc)
               :color         (:workspace/color spc)
               :title_pango   (:workspace/title-pango spc)}))
       (map (fn [spc]
              (assoc spc
                     :sort-key (str (if (:scratchpad spc) "z" "a") "-"
                                    (format "%03d" (or (:awesome_index spc)
                                                       0))))))
       ;; sort and map-indexed to set new_indexes
       (sort-by :sort-key)
       (map-indexed (fn [i wsp] (assoc wsp :new_index
                                       ;; lua indexes start at 1
                                       (+ i 1))))))

(comment
  (format "%03d" nil)
  (->>
    (all-workspaces)
    (filter :awesome/tag)
    (map workspace-name))

  (->>
    (active-workspaces)
    (map :sort-key)))

(defn update-workspaces-widget
  []
  (let [fname "update_workspaces_widget"]
    (awm/awm-cli
      {:quiet? true}
      (awm/awm-fn fname (active-workspaces)))))

(comment
  (->>
    (active-workspaces)
    (map :awesome-index))
  (awm/awm-fn "update_workspaces_widget" (active-workspaces))

  (update-workspaces-widget))

(defcom update-workspaces
  "updates the workspaces widget to reflect the current workspaces state."
  "expects a function-name as an argument,
which is called with a list of workspaces maps."
  (update-workspaces-widget))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Swapping Workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn drag-workspace
  "Drags the current awesome workspace in the given direction"
  [dir]
  (let [up?   (= dir "up")
        down? (= dir "down")]
    (if (or up? down?)
      (do
        (awm/awm-cli
          {:quiet? true}
          (str
            "tags = awful.screen.focused().tags; "
            "current_index = s.selected_tag.index; "
            "new_index = current_index " (cond up? "+ 1" down? "- 1" ) "; "
            "new_tag = tags[new_index]; "
            "if new_tag then s.selected_tag:swap(new_tag) end; "
            ))
        (update-workspaces-widget))
      (notify/notify "drag-workspace called without 'up' or 'down'!"))))

(comment
  (println "hi")
  (drag-workspace "up"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; consolidate workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn consolidate-workspaces []
  (->>
    (all-workspaces)
    (remove :awesome/empty)
    (sort-by :awesome/index)
    (map-indexed
      (fn [new-index {:keys [awesome/name awesome/index]}]
        (let [new-index (+ 1 new-index)] ;; b/c lua 1-based
          (if (== index new-index)
            (prn "nothing to do")
            (do
              (prn "swapping tags" {:name      name
                                    :idx       index
                                    :new-index new-index})
              (awm/awm-cli
                (str "local tag = awful.tag.find_by_name(nil, \"" name "\");"
                     "local tags = awful.screen.focused().tags;"
                     "local tag2 = tags[" new-index "];"
                     "tag:swap(tag2);")))))))))

(defcom consolidate-workspaces-cmd
  "Groups active workspaces closer together"
  "Moves active workspaces to the front of the list."
  (do
    (consolidate-workspaces)
    (update-workspaces-widget)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clean up workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clean-workspaces
  "Closes workspaces with 0 clients."
  []
  (notify/notify {:subject "Cleaning up workspaces"})
  (->>
    (all-workspaces)
    (filter :awesome/empty)
    (map
      (fn [it]
        (when-let [name (item/awesome-name it)]
          (try
            (r.awm/delete-tag! name)
            (notify/notify "Deleted Tag" name)
            (catch Exception e e
                   (notify/notify "Error deleting tag" e))))))
    doall))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; New create workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-workspace
  "Creates a new tag, focuses it, and run the workspace's on-create hook."
  [wsp]
  (let [name (workspace-name wsp)]

    (println "create-workspace")

    ;; create tag if none is found
    (when (not (r.awm/tag-for-name name))
      (println "create-tag")
      (r.awm/create-tag! name))

    (println "focus-tag")
    ;; focus the tag
    (r.awm/focus-tag! name)

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
            (:awesome/empty wsp)
            ;; or had no tag
            (not (:awesome/tag wsp)))
      (wsp.create/create-client wsp))

    ;; return the workspace
    wsp))

(comment
  (r.awm/tag-for-name "ralphie")
  (r.awm/create-tag! "ralphie")
  (->
    "ralphie"
    (for-name)
    (create-workspace)
    ))


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

(defn select-workspace
  "Opens a list of workspaces in rofi.
  Returns the selected workspace."
  []
  (rofi/rofi
    {:msg "New Workspace Name?"}
    (->>
      (all-workspaces)
      (map apply-git-status)
      (sort-by (comp not (some-fn :git/dirty? :git/needs-push? :git/needs-pull?)))
      ;; TODO create :rofi/label multimethod
      (map #(assoc % :rofi/label (wsp->repo-and-status-label %)))
      seq)))

(defn open-workspace
  ([] (open-workspace nil))
  ([name]
   ;; select and create
   (if name
     (-> name for-name create-workspace)
     ;; no name passed, get from rofi
     (some-> (select-workspace) create-workspace))
   (update-workspaces-widget)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; toggle workspace names
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom toggle-scratchpad-names
  (do
    (awm/awm-cli "_G.toggle_show_scratchpad_names();")
    (update-workspaces-widget)))
