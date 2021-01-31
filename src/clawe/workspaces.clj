(ns clawe.workspaces
  (:require
   [ralph.defcom :refer [defcom]]
   [ralphie.workspace :as r.workspace]
   [ralphie.rofi :as rofi]
   [clawe.defs :as defs]
   [clawe.awesome :as awm]
   [ralphie.awesome :as r.awm]
   [ralphie.git :as r.git]
   [ralphie.item :as item]
   [ralphie.notify :as notify]
   [ralphie.scratchpad :as r.scratchpad]))

(defn current-workspace []
  (let [wsp (r.workspace/current-workspace)]
    (merge wsp (defs/get-workspace wsp))))

(comment
  (current-workspace)
  )

(defn workspace-name [wsp]
  (or (:clawe/name wsp)
      (:workspace/name wsp)
      (:org/name wsp)
      (:awesome/name wsp)))

(defn workspace-repo [wsp]
  (or (:workspace/directory wsp)
      (:org.prop/directory wsp)))

(defn apply-git-status [wsp]
  (let [dir (workspace-repo wsp)]
    (if (r.git/repo? dir)
      (merge wsp (r.git/status dir))
      wsp)))

(defn all-workspaces []
  (->>
    (concat
      (r.workspace/all-workspaces)
      (defs/list-workspaces))
    (group-by workspace-name)
    (remove (comp nil? first))
    (map second)
    (map #(apply merge %))))

(comment
  (all-workspaces)
  )

(defn for-name [name]
  (some->>
    (all-workspaces)
    (filter (comp #{name} workspace-name))
    first))

(comment
  (for-name "ralphie")
  )

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
               :name          (item/awesome-name spc)
               :awesome_index (item/awesome-index spc)
               :key           (-> spc :org.prop/key)
               :fa_icon_code  (when-let [code (or
                                                (:workspace/fa-icon-code spc)
                                                (:org.prop/fa-icon-code spc))]
                                (str "\\u{" code "}"))
               :scratchpad    (item/scratchpad? spc)
               :selected      (item/awesome-selected spc)
               :empty         (item/awesome-empty spc)
               :dirty         (:git/dirty? spc)
               :needs_pull    (:git/needs-pull? spc)
               :needs_push    (:git/needs-push? spc)
               :color         (:workspace/color spc)
               :title_pango   (:workspace/title-pango spc)}))
       (map (fn [spc]
              (assoc spc
                     :sort-key (str (if (:scratchpad spc) "z" "a") "-"
                                    (:awesome_index spc)))))
       ;; sort and map-indexed to set new_indexes
       (sort-by :sort-key)
       (map-indexed (fn [i wsp] (assoc wsp :new_index
                                       ;; lua indexes start at 1
                                       (+ i 1))))))

(comment
  (->>
    (r.workspace/all-workspaces)
    (filter :awesome/tag))

  (active-workspaces))

(defn update-workspaces-widget
  ([] (update-workspaces-widget nil))
  ([fname]
   (let [fname (or fname "update_workspaces_widget")]
     (awm/awm-cli
       {:quiet? true}
       (awm/awm-fn fname (active-workspaces))))))

(comment
  (->>
    (active-workspaces)
    (map :awesome-index))
  (awm/awm-fn "update_workspaces_widget" (active-workspaces))

  (update-workspaces-widget))

(defcom update-workspaces-cmd
  {:name    "update-workspaces"
   :one-line-desc
   "updates the workspaces widget to reflect the current workspaces state."
   :description
   ["expects a function-name as an argument,
which is called with a list of workspaces maps."]
   :handler (fn [_ parsed]
              (update-workspaces-widget (-> parsed :arguments first)))})


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

(defn drag-workspace-index-handler
  ([] (drag-workspace-index-handler nil nil))
  ([_config {:keys [arguments]}]
   (let [[dir & _rest] arguments]
     (drag-workspace dir))))

(defcom drag-workspace-index-cmd
  {:name          "drag-workspace-index"
   ;; TODO *keys-pressed* as a dynamic var/macro or partially applied key in your keybinding
   ;; TODO support keybindings right here
   :keybinding    "ctrl-shift-p"
   :one-line-desc "Drags a workspace up or down an index."
   :description   ["Intended to feel like dragging a workspace in a direction."]
   :handler       drag-workspace-index-handler})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; consolidate workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn consolidate-workspaces []
  (->>
    (r.workspace/all-workspaces)
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

(comment
  (consolidate-workspaces))

(defn consolidate-workspaces-handler
  ([] (consolidate-workspaces-handler nil nil))
  ([_config _parsed]
   (consolidate-workspaces)
   (update-workspaces-widget)))

(defcom consolidate-workspaces-cmd
  {:name          "consolidate-workspaces"
   :one-line-desc "Groups active workspaces closer together"
   :description   ["Moves active workspaces to the front of the list."]
   :handler       consolidate-workspaces-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clean up workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clean-workspaces
  "Closes workspaces with 0 clients."
  []
  (notify/notify "Cleaning up workspaces")
  (->>
    (r.workspace/all-workspaces)
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

(comment
  (clean-workspaces))

(defn clean-workspaces-handler
  ([] (clean-workspaces-handler nil nil))
  ([_config _parsed]
   (notify/notify "Cleaning up workspaces - 1")
   (clean-workspaces)
   (update-workspaces-widget)))

(defcom clean-workspaces-cmd
  {:name          "clean-workspaces"
   :one-line-desc "Closes workspaces that have no active clients"
   :handler       clean-workspaces-handler})

(comment)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; New create workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-workspace
  "Creates a new tag, focuses it, and run the workspace's on-create hook."
  [wsp]
  (let [name (workspace-name wsp)]

    ;; create tag if none is found
    (when (not (r.awm/tag-for-name name))
      (r.awm/create-tag! name))

    ;; focus the tag
    (r.awm/focus-tag! name)

    ;; run on-create hook
    (when-let [f (:workspace/on-create wsp)]
      (f wsp))

    ;; notify
    (notify/notify (str "Created new workspace: " name))

    ;; create first client if it's not already there
    ;; NOTE might want this to be opt-in/out
    (when (or
            ;; was empty
            (:awesome/empty wsp)
            ;; or had no tag
            (not (:awesome/tag wsp)))
      (r.scratchpad/create-client wsp))

    ;; return the workspace
    wsp))

(comment
  (->
    "ralphie"
    (for-name)
    (create-workspace)))


(defn wsp->rofi-label [{:as   wsp
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
      (map #(assoc % :rofi/label (wsp->rofi-label %)))
      seq)))

(comment
  (select-workspace))

(defn open-workspace
  ([] (open-workspace nil))
  ([name]
   ;; First, delete empty workspaces
   (clean-workspaces)

   ;; Then select and create a new one
   (if name
     (do
       (notify/notify (str "Workspace name passed, creating: " name))
       (-> name
           for-name
           create-workspace))

     ;; no tag, get from rofi
     (some-> (select-workspace)
             ((fn [wsp]
                (create-workspace wsp)))))

   (update-workspaces-widget)))

(comment
  (open-workspace))

(defcom open-workspace-cmd
  {:name          "open-workspace"
   :one-line-desc "Opens a new workspace via rofi."
   :description   []
   :handler       (fn [_ parsed]
                    (open-workspace (some-> parsed :arguments first)))})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; toggle workspace names
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom toggle-scratchpad-names
  {:name    "toggle-scratchpad-names"
   :handler (fn [_ _]
              (awm/awm-cli "_G.toggle_show_scratchpad_names();")
              (update-workspaces-widget))})

(comment
  (toggle-scratchpad-names nil nil)
  )
