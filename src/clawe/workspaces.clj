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
   [clawe.workspace :as workspace]
   [clawe.wm :as wm]))

(def home-dir (zsh/expand "~"))

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
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- wsp-sort-key [wsp]
  (str (if (#{home-dir} (:workspace/directory wsp)) "z" "a") "-"
       (format "%03d" (or (:workspace/index wsp) 0))))

(defn- workspaces-to-swap-indexes
  "Creates a sorted list of workspaces with an additional key: :new-index.
  This is in support of the `update-workspace-indexes` func below."
  []
  (->> (workspace/all-active)
       (map (fn [wsp] (assoc wsp :sort-key (wsp-sort-key wsp))))
       ;; sort and map-indexed to set new_indexes
       (sort-by :sort-key)
       (map-indexed (fn [i wsp]
                      ;; lua indexes start at 1, and osx's first wsp as 1...
                      ;; this is probably right for most wm indexes (to match the keyboard)
                      (assoc wsp :new-index (+ i 1))))
       (remove #(= (:new-index %) (:workspace/index %)))))

(defn update-workspace-indexes
  []
  (loop [wsps (workspaces-to-swap-indexes)]
    (let [wsp (some-> wsps first)]
      (when wsp
        (let [{:keys [new-index]} wsp
              index               (-> wsp :workspace/title
                                      wm/fetch-workspace :workspace/index)]
          (when (not= new-index index)
            (wm/swap-workspaces-by-index index new-index))
          ;; could be optimized....
          (recur (workspaces-to-swap-indexes)))))))

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
      (do
        (yabai/ensure-labeled-space
          {:space-label       title
           :overwrite-labeled true})
        (yabai/focus-space {:space-label title}))

      (do
        ;; create tag if none is found
        (awm/ensure-tag title)
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
