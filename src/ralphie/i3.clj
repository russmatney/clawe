(ns ralphie.i3
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
   [babashka.process :as process]))

(defn wsp->workspace-title [wsp]
  (if (:i3/name wsp)
    (string/replace (:i3/name wsp) #"^.*: ?" "")
    (:workspace/title wsp)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3-msg
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn i3-msg! [msg]
  (let [cmd (str "i3-msg " msg)]
    (-> (process/process
          {:cmd cmd :out :string})
        ;; throws when error occurs
        (process/check)
        :out
        (json/parse-string
          (fn [k] (keyword "i3" k))))))

(defn i3-cmd! [msg]
  (i3-msg! (str "\"" msg "\"")))

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

(defn next-wsp-number []
  (let [existing-wsp-nums (->> (workspaces-fast) (map :i3/num) (into #{}))]
    (->> (range 10)
         (drop 1)
         (remove existing-wsp-nums)
         first)))

(defn find-or-create-wsp-name [wsp]
  (when wsp
    (if (:i3/name wsp) (:i3/name wsp)
        (if-let [title (:workspace/title wsp)]
          (if-let [w (workspace-for-name title)]
            (:i3/name w)
            (str (next-wsp-number) ": " title))
          (println "No :i3/name or :workspace/title on passed wsp" wsp)))))

(defn ix->wsp
  ([ix] (ix->wsp (workspaces-fast) ix))
  ([wsps ix] (some->> wsps (filter (comp #{ix} :i3/num)) first)))

(defn current-workspace
  ([] (current-workspace nil))
  ([opts]
   (if (:include-clients opts)
     (let [wsp (->> (workspaces-fast) (filter :i3/focused) first)]
       (workspace-for-name (:i3/name wsp)))
     (some->> (workspaces-fast) (filter :i3/focused) first))))

(comment
  (workspaces)
  (current-workspace)
  (current-workspace {:include-clients true})
  (workspace-for-name "clawe"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3 Workspace Upsert
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-workspace [title]
  (when title
    (let [num (next-wsp-number)]
      (i3-msg! (str "workspace " num ": " title)))))

(defn focus-workspace [number]
  (when number
    (i3-msg! (str "workspace number " number))))

(defn rename-workspace [name number]
  (i3-msg! (str "rename workspace to " (str number ":" name))))

(comment
  (rename-workspace "clawe" 3)
  (focus-workspace "4:4"))


(defn swap-workspaces-by-index [ixa ixb]
  (let [wsps (workspaces-fast)
        a    (ix->wsp wsps ixa) name_a (wsp->workspace-title a)
        b    (ix->wsp wsps ixb) name_b (wsp->workspace-title b)]
    (i3-cmd! (str "rename workspace \\\"" (:i3/name a)  "\\\" to " ixb ":" name_a " ; "
                  (when b
                    (str "rename workspace \\\"" (:i3/name b) "\\\" to " ixa ":" name_b))))))

(comment
  (swap-workspaces-by-index 4 5))


(defn swap-workspaces [a b]
  (let [name_a (:workspace/title a)
        num_a  (:i3/num a)
        name_b (:workspace/title b)
        num_b  (:i3/num b)]
    ;; TODO finish impling
    (i3-cmd! (str "rename workspace \\\"" num_a ": " name_a "\\\" to \\\"" num_b ": " name_a "\\\" ; "
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

(defn focused-client
  "Returns a map describing the currently focused node."
  []
  (->> (->all-nodes)
       (filter :i3/focused)
       (filter :i3/window_properties)
       first))

(comment
  (focused-client))

(defn focus-client [client]
  (i3-msg! (str " [con_id=" (:i3/id client) "] focus")))

(defn bury-client [client]
  (i3-msg! (str " [con_id=" (:i3/id client) "] floating disable")))

(defn bury-clients [clients]
  (i3-msg!
    (string/join " ; "
                 (->> clients
                      (map (fn [client] (str " [con_id=" (:i3/id client) "] floating disable")))))))

(defn close-client [client]
  (i3-msg! (str " [con_id=" (:i3/id client) "] kill")))

(defn hide-scratchpad [client]
  (i3-msg! (str " [con_id=" (:i3/id client) "] move scratchpad")))

(defn move-client-to-workspace [client wsp]
  (when (and (:i3/id client) wsp)
    (when-let [wsp-name (find-or-create-wsp-name wsp)]
      (i3-cmd!
        (str "[con_id=" (:i3/id client) "] "
             ;; focus the client first
             ;; " focus, "
             ;; move the client to the indicated workspace
             ;; "move window to workspace " (:i3/name wsp)
             "move --no-auto-back-and-forth to workspace " wsp-name
             ;; toggle the current workspace (to trigger a go-back-to-prev wsp)
             ;; ", workspace " (:i3/name wsp)
             )))))
