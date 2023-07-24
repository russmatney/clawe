(ns ralphie.i3
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
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
  (string/join " "  (concat "i3-msg" ["-t" "get_tree"])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; data roots
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn flatten-nodes
  [x] (flatten ((juxt :i3/nodes :i3/floating_nodes) x)))

(defn tree []
  (-> (i3-msg! "-t get_tree")))

(defn ->all-nodes
  "Returns all :i3/nodes and :i3/floating_nodes"
  ([] (->all-nodes (tree)))
  ([t] (tree-seq flatten-nodes flatten-nodes t)))

(defn content-node
  "Most parses/paths stem from here"
  ([] (content-node (tree)))
  ([t]
   (some->> t :i3/nodes
            (remove #(= (:i3/name %) "__i3"))
            ;; NOTE may not be happy across multiple displays
            first
            :i3/nodes
            (filter #(= (:i3/name %) "content"))
            first)))

(comment
  (->all-nodes)
  (workspaces)

  (->> (->all-nodes)
       (filter :i3/focused)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspaces-fast []
  (i3-msg! "-t get_workspaces"))

(defn workspaces
  ([] (workspaces (tree)))
  ([t] (->> (content-node t) :i3/nodes)))

(defn workspace-for-name
  "Returns a workspace from tree for the passed workspace name."
  ([wsp-name] (workspace-for-name wsp-name (tree)))
  ([wsp-name t]
   (some->> (workspaces t)
            ;; TODO stronger string match
            (filter #(string/includes? (:i3/name %) wsp-name))
            first)))

(defn current-workspace []
  (some->> (workspaces-fast) (filter :i3/focused) first))

(comment
  (workspaces)
  (current-workspace)
  (workspace-for-name "clawe"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3 Workspace Upsert
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn next-wsp-number []
  (let [existing-wsp-nums (->> (workspaces-fast) (map :i3/num) (into #{}))]
    (->> (range 10)
         (remove existing-wsp-nums)
         first)))

(defn create-workspace [title]
  (let [num (next-wsp-number)]
    (i3-msg! (str "workspace " num ": " title))))

(defn focus-workspace [number]
  (i3-msg! (str "workspace number " number)))

(defn rename-workspace [name number]
  (i3-msg! (str "rename workspace to " (str number ":" name))))

(comment
  (rename-workspace "clawe" 3)
  (focus-workspace "4:4"))

(defn swap-workspaces [a b]
  (let [name_a (:workspace/title a)
        num_a  (:i3/num a)
        name_b (:workspace/title b)
        num_b  (:i3/num b)]
    ;; TODO finish impling
    (i3-msg! (str "rename workspace \\\"" num_a ": " name_a "\\\" to \\\"" num_b ": " name_a "\\\" ; "
                  "rename workspace \\\"" num_b ": " name_b "\\\" to \\\"" num_a ": " name_b "\\\""))))

(defn delete-workspace [_wsp]
  ;; switch to this workspace, move it's contents elsewhere (scratchpad? prev-wsp?)
  ;; then 'kill' it
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clients/windows/apps
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn all-clients
  ([] (all-clients (tree)))
  ([t] (->> t ->all-nodes (filter :i3/window_properties))))

(comment
  (all-clients))

(defn wsp->clients [wsp]
  (->> wsp ->all-nodes (filter :i3/window_properties)))

(defn clients-for-wsp-name
  ([name] (clients-for-wsp-name name (tree)))
  ([name t]
   (-> name (workspace-for-name t) wsp->clients)))

(comment
  (workspace-for-name "clawe")
  (clients-for-wsp-name "clawe"))

(defn focused-app
  "Returns a map describing the currently focused node."
  []
  (->> (->all-nodes)
       (filter :i3/focused)
       (filter :i3/window_properties)
       first))

(comment
  (focused-app))
