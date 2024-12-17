(ns ralphie.sway
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
   [babashka.process :as process]))

;; swaymsg

(defn swaymsg! [msg]
  (let [cmd (str "swaymsg " msg)]
    (-> (process/process
          {:cmd cmd :out :string})
        ;; throws when error occurs
        (process/check)
        :out
        (json/parse-string
          (fn [k] (keyword "sway" k))))))

(comment
  (swaymsg! "-t get_tree")
  (swaymsg! "-t get_workspaces")
  (current-workspace))

;; workspace read

(defn workspaces []
  (swaymsg! "-t get_workspaces"))

(defn current-workspace []
  (some->>
    (swaymsg! "-t get_workspaces")
    (filter :sway/focused)
    first))

(defn get-workspace [{:keys [name]}]
  (some->>
    (swaymsg! "-t get_workspaces")
    (filter (comp #(string/includes? % name) :sway/name))
    first))

;; helpers

(defn next-wsp-number []
  (let [existing-wsp-nums (->> (workspaces) (map :sway/num) (into #{}))]
    (->> (range 10)
         (drop 1)
         (remove existing-wsp-nums)
         first)))

(comment
  (next-wsp-number))

(defn ix->wsp
  ([ix] (ix->wsp (workspaces) ix))
  ([wsps ix] (some->> wsps (filter (comp #{ix} :sway/num)) first)))

(defn wsp->workspace-title [wsp]
  (if (:sway/name wsp)
    (string/replace (:sway/name wsp) #"^.*: ?" "")
    (:workspace/title wsp)))

;; workspace write

(defn create-workspace [{:keys [name]}]
  (when name
    (let [num (next-wsp-number)]
      (swaymsg! (str "workspace " num ": " name)))))

(defn focus-workspace [{:keys [num]}]
  (when num
    (swaymsg! (str "workspace number " num))))

(defn swap-workspaces-by-index [ixa ixb]
  (let [wsps   (workspaces)
        a      (ix->wsp wsps ixa)
        name-a (wsp->workspace-title a)
        b      (ix->wsp wsps ixb)
        name-b (wsp->workspace-title b)]
    (swaymsg! (str "\""
                   "rename workspace \\\"" (:sway/name a)  "\\\" to " ixb ":" name-a " ; "
                   (when b
                     (str "rename workspace \\\"" (:sway/name b) "\\\" to " ixa ":" name-b))
                   "\""))))

(comment
  (create-workspace {:name "clawe"})
  (focus-workspace {:num 2})
  (swap-workspaces-by-index 1 2))

;; client read

(defn tree []
  (swaymsg! "-t get_tree"))

(defn flatten-nodes
  [x] (flatten ((juxt :sway/nodes :sway/floating_nodes) x)))

(defn all-nodes
  "Returns all :sway/nodes and :sway/floating_nodes"
  [] (->> (tree) (tree-seq flatten-nodes flatten-nodes)))

(comment
  (tree)
  (all-nodes))

(defn clients []
  (->> (all-nodes)
       (filter (some-fn :sway/app_id :sway/window_properties))))

(defn scratchpad-clients []
  (->> (all-nodes)
       (filter (some-fn :sway/app_id :sway/window_properties))
       ;; no sure this is right
       (filter (comp #{"fresh"} :sway/scratchpad_state))))

(defn wsp->clients [{:keys [] :as wsp}]
  ;; note sure if this gets tiling clients
  (->> wsp
       (tree-seq flatten-nodes flatten-nodes)
       (filter (some-fn :sway/app_id :sway/window_properties))))

(comment
  (-> (current-workspace)
      wsp->clients))

;; client write

(defn focus-client [client]
  (swaymsg! (str " [con_id=" (:sway/id client) "] focus")))

(defn bury-client [client]
  (swaymsg! (str " [con_id=" (:sway/id client) "] floating disable")))

(defn bury-clients [clients]
  (swaymsg!
    (string/join " ; "
                 (->> clients
                      (map (fn [client] (str " [con_id=" (:sway/id client) "] floating disable")))))))

(defn close-client [client]
  (swaymsg! (str " [con_id=" (:sway/id client) "] kill")))

(defn hide-scratchpad [client]
  (swaymsg! (str " [con_id=" (:sway/id client) "] move scratchpad")))

(defn find-or-create-wsp-name [wsp]
  (when wsp
    (if (:sway/name wsp)
      (:sway/name wsp)
      (if-let [title (:workspace/title wsp)]
        (if-let [w (-> {:name title} get-workspace)]
          (:sway/name w)
          (str (next-wsp-number) ": " title))
        (println "No :sway/name or :workspace/title on passed wsp" wsp)))))

(defn move-client-to-workspace [client wsp]
  (when (and (:sway/id client) wsp)
    (when-let [wsp-name (find-or-create-wsp-name wsp)]
      (swaymsg!
        (str
          "\""
          "[con_id=" (:sway/id client) "] "
          "move --no-auto-back-and-forth to workspace " wsp-name
          "\"")))))
