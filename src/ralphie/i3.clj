(ns ralphie.i3
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
   [ralphie.config :as config]
   [ralphie.rofi :as rofi]
   [clojure.java.shell :as sh]
   [clojure.set :as set]
   [defthing.defcom :as defcom :refer [defcom]]
   [babashka.process :as process]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3-msg
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn i3-msg! [msg]
  (let [cmd (str "i3-msg " msg)]
    (println cmd)
    (-> (process/process
          {:cmd cmd :out :string})
        ;; throws when error occurs
        (process/check)
        :out
        (json/parse-string
          (fn [k] (keyword "i3" k))))))

(comment
  (i3-msg! "-t get_tree")
  (string/join " "  (concat "i3-msg" ["-t" "get_tree"]))


  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3-data roots
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tree []
  (-> (i3-msg! "-t get_tree")))

(defn workspaces-simple []
  (-> (i3-msg! "-t get_workspaces")))

(comment
  (tree)
  (workspaces-simple))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; mid-parse utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn monitor-node
  []
  (let [monitor (config/monitor)]
    (some->> (tree)
             :i3/nodes
             (filter #(= (:i3/name %) monitor))
             first)))

(defn content-node [m-node]
  (some->> m-node
           :i3/nodes
           (filter #(= (:i3/name %) "content"))
           first))

(defn flatten-nodes
  "Joins and flattens `:nodes` and `:floating_nodes` in x"
  [x]
  (flatten ((juxt :i3/nodes :i3/floating_nodes) x)))

(defn tree->nodes [tr]
  (tree-seq flatten-nodes flatten-nodes tr))

(defn all-nodes []
  (->> (tree)
       tree->nodes))

(defn workspaces-from-tree []
  (->> (monitor-node)
       content-node
       :i3/nodes))

(comment
  (all-nodes)
  (workspaces-from-tree)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current-workspace
  []
  (some->> (workspaces-simple)
           (filter :i3/focused)
           first))

(comment
  (current-workspace)
  (println "\n\n\nbreak\n\n\n")
  ;; (clojure.pprint/pprint
  ;;   (current-workspace))
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; focused node/apps
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn focused-node
  "Returns a map describing the currently focused app."
  []
  (->> (all-nodes)
       (filter :i3/focused)
       first))

(defn focused-app
  [] (-> (focused-node)
         :i3/window_properties
         :i3/class))

(comment
  (focused-node)
  (focused-app)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspace for name
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspace-for-name
  "Returns a workspace from tree for the passed workspace name.
  TODO: handle multiple monitors
  "
  [wsp-name]
  (some->> (monitor-node)
           content-node
           :i3/nodes
           (filter #(string/includes? (:i3/name %) wsp-name))
           first
           ))

(defn nodes-for-wsp-name
  [name]
  (-> name
      workspace-for-name
      tree->nodes))

(defn app-names-in-wsp
  [name]
  (->> name
       nodes-for-wsp-name
       (map (comp :i3/class :i3/window_properties))))

(defn workspace-open?
  [name]
  (seq (workspace-for-name name)))

(defn apps-open?
  "Only searches within the passed workspace."
  [workspace apps]
  (-> workspace
      app-names-in-wsp
      set
      ((fn [open-apps]
         (set/subset? (set apps) open-apps)))))

(comment
  (workspace-for-name "yodo")
  (app-names-in-wsp "read")
  (apps-open? "read" ["Alacritty"]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3 Workspace Upsert
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn next-wsp-number []
  (let [existing-wsp-nums (->> (workspaces-simple) (map :i3/num) (into #{}))]
    (->>
      (range 10)
      (remove existing-wsp-nums)
      first)))

(defn create-workspace [title]
  (let [num (next-wsp-number)]
    (i3-msg! (str "workspace " num ": " title))))

(defn visit-workspace [number]
  (i3-msg! (str "workspace number " number)))

(defn rename-workspace [name number]
  (i3-msg! (str "rename workspace to " (str number ":" name))))

(defn swap-workspaces [a b]
  (let [name_a (:workspace/title a)
        num_a  (:i3/num a)
        name_b (:workspace/title b)
        num_b  (:i3/num b)]
    ;; TODO finish impling
    (i3-msg! (str "rename workspace '" num_a ": " name_a "' to '" num_b ": " name_a "'; "
                  "rename workspace '" num_b ": " name_b "' to '" num_a ": " name_b "'"))))

(defn delete-workspace [_wsp]
  ;; switch to this workspace, move it's contents elsewhere (scratchpad? prev-wsp?)
  ;; then 'kill' it
  )


(defn upsert
  "TODO Perhaps this logic should be in workspaces?"
  [{:keys [name]}]
  (let [name-to-update   (->> (workspaces-simple)
                              (map :i3/name)
                              (rofi/rofi {:msg "Workspace to update?"}))
        number-to-update (some-> name-to-update (string/split #":") first)]
    (rename-workspace name number-to-update)))

(comment
  (upsert {:name "timeline"})
  (rename-workspace "clawe" 3)
  (visit-workspace "4:4"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; resize window
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def window-size-options
  [{:label  "small-centered"
    :i3-cmd "floating enable, resize set width 50 ppt height 50 ppt, move position center"}
   {:label  "large-centered"
    :i3-cmd "floating enable, resize set width 90 ppt height 90 ppt, move position center"}
   {:label  "tall-centered"
    :i3-cmd "floating enable, resize set width 40 ppt height 80 ppt, move position center"}
   ])

(defcom resize-window
  {:doctor/depends-on ["i3-msg"]}
  (->> window-size-options
       (rofi/rofi {:msg "Choose window layout type"})
       :i3-cmd
       (i3-msg!)))
