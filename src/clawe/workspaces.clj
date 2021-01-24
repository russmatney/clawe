(ns clawe.workspaces
  (:require
   [ralph.defcom :refer [defcom]]
   [ralphie.workspace :as r.workspace]
   [ralphie.rofi :as rofi]
   [clawe.defs :as defs]
   [clawe.awesome :as awm]
   [ralphie.awesome :as r.awm]
   [ralphie.item :as item]
   [ralphie.notify :as notify]))

(defn all-workspaces []
  (->>
    (concat
      (r.workspace/all-workspaces)
      (defs/list-workspaces))
    (group-by (some-fn item/awesome-name :workspace/name))
    (remove (comp nil? first))
    (map second)
    (map #(apply merge %))))

(comment
  (all-workspaces)
  )

(defn active-workspaces
  "Pulls workspaces to show in the workspaces-widget."
  []
  (->> (all-workspaces)
       (filter :awesome/tag)
       (map (fn [spc]
              {;; consider flags for is-scratchpad/is-app/is-repo
               :name          (item/awesome-name spc)
               :awesome_index (item/awesome-index spc)
               :key           (-> spc :org.prop/key)
               :fa_icon_code  (when-let [code (:org.prop/fa-icon-code spc)]
                                (str "\\u{" code "}"))
               :scratchpad    (item/scratchpad? spc)
               :selected      (item/awesome-selected spc)
               :empty         (item/awesome-empty spc)
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
;; New create workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn select-workspace
  "Opens a list of workspaces in rofi."
  []
  (rofi/rofi
    {:msg "New Workspace Name?"}
    (->>
      (r.workspace/all-workspaces)
      (map :org/name)
      seq)))

(defn open-workspace-handler
  [_ {:keys [arguments]}]
  (if-let [tag-name (some-> arguments first)]
    (do
      (notify/notify (str "Found workspace, creating: " tag-name))
      (r.awm/create-tag! tag-name))

    ;; no tag, get from rofi
    (some-> (select-workspace)
            ((fn [w-name]
               (when (not (r.awm/tag-for-name w-name))
                 (r.awm/create-tag! w-name))
               w-name))
            r.awm/focus-tag!
            ((fn [name]
               (notify/notify (str "Created new workspace: " name))))))
  (update-workspaces-widget))

(defcom open-workspace
  {:name          "open-workspace"
   :one-line-desc "Opens a new workspace via rofi."
   :description   []
   :handler       open-workspace-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; consolidate workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn consolidate-workspaces []
  (->>
    (r.workspace/all-workspaces)
    (filter :awesome/tag)
    (filter (comp #(> % 0) count :clients :awesome/tag))
    (sort-by (comp :index :awesome/tag))
    (map-indexed
      (fn [new-index {:keys [:awesome/tag]}]
        (let [new-index            (+ 1 new-index) ;; b/c lua 1-based
              {:keys [name index]} tag]
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
    (filter :awesome/tag)
    (filter (comp :empty :awesome/tag))
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
