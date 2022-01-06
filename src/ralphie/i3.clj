(ns ralphie.i3
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
   [ralphie.config :as config]
   [ralphie.rofi :as rofi]
   [clojure.java.shell :as sh]
   [clojure.set :as set]
   [defthing.defcom :as defcom :refer [defcom]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3-msg
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO refactor when in i3
(defn i3-msg! [& args]
  (apply sh/sh "i3-msg" args))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3-data roots
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tree []
  (-> (i3-msg! "-t" "get_tree")
      :out
      (json/parse-string true)))

(defn workspaces-simple []
  (-> (i3-msg! "-t" "get_workspaces")
      :out
      (json/parse-string true)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; mid-parse utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn monitor-node
  []
  (let [monitor (config/monitor)]
    (some->> (tree)
             :nodes
             (filter #(= (:name %) monitor))
             first)))

(defn content-node [m-node]
  (some->> m-node
           :nodes
           (filter #(= (:name %) "content"))
           first))

(defn flatten-nodes
  "Joins and flattens `:nodes` and `:floating_nodes` in x"
  [x]
  (flatten ((juxt :nodes :floating_nodes) x)))

(defn tree->nodes [tr]
  (tree-seq flatten-nodes flatten-nodes tr))

(defn all-nodes []
  (->> (tree)
       tree->nodes))

(defn workspaces-from-tree []
  (->> (monitor-node)
       content-node
       :nodes))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current-workspace
  "TODO rewrite to use get_tree"
  []
  (some->> (workspaces-simple)
           (filter :focused)
           first
           :name
           ((fn [name]
              (some->> (workspaces-from-tree)
                       (filter (fn [node]
                                 (= (:name node) name)))
                       first)))))

(comment
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
       (filter :focused)
       first))

(defn focused-app
  [] (-> (focused-node)
         :window_properties
         :class))

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
           :nodes
           (filter #(string/includes? (:name %) wsp-name))
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
       (map (comp :class :window_properties))))

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

(defn visit-workspace [number]
  (i3-msg! "workspace" "number" number))

(defn rename-workspace [name number]
  []
  (i3-msg! "rename" "workspace" "to" (str number ":" name)))

(defn upsert
  "TODO Perhaps this logic should be in workspaces?"
  [{:keys [name]}]
  (let [name-to-update   (->> (workspaces-simple)
                              (map :name)
                              (rofi/rofi {:msg "Workspace to update?"}))
        number-to-update (some-> name-to-update (string/split #":") first)]
    (rename-workspace name number-to-update)))

(comment
  (upsert {:name "timeline"})
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspaces.org items -> i3 config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defn workspace->lines
;;   [i {:keys [name props]}]
;;   (let [{:keys [number hotkey pinned-apps]} props
;;         number                              (or number (+ 1 i))
;;         hotkey                              (or hotkey number)]
;;     (concat
;;       [(str "set $wn" number " \"" number ": " name "\"")
;;        (str "bindsym $mod+" hotkey " workspace number $wn" number)
;;        (str "bindsym $mod+Shift+" hotkey
;;             " move container to workspace number $wn" number)
;;        ;; TODO pull monitor from config
;;        (str "workspace $wn" number " output HDMI-0 eDP-1")]
;;       (for [app pinned-apps]
;;         (str "for_window [class=\"" app "\"] move"
;;              "--no-auto-back-and-forth"
;;              "to workspace number $wn" number)))))

;; (defn write-i3-ralphie
;;   "Parses misc org data into an i3 config. Writes to the passed file."
;;   [file]
;;   (let [workspaces (org/fname->items "workspaces.org")]
;;     (->> workspaces
;;          (take 10)
;;          (map-indexed workspace->lines)
;;          (remove nil?)
;;          (apply concat)
;;          (string/join "\n")
;;          (spit file))))

;; (comment
;;   (write-i3-ralphie (expand "~/temp-i3-conf")))

;; (defn rebuild-i3-config!
;;   "The i3 config is partially based on data in <org-dir>/workspaces.org.
;;   This function converts that data to <i3-config-dir>/config.ralphie and
;;   concatenates it with <i3-config-dir>/config.base.

;;   Could be rewritten to not write the config.ralphie.
;;   Left as is to help debugging/expose what this command is doing.
;;   "
;;   []
;;   (let [i3-config-file (str (config/i3-dir) "/config")
;;         base-file      (str (config/i3-dir) "/config.base")
;;         ralphie-file   (str (config/i3-dir) "/config.ralphie")]
;;     (write-i3-ralphie ralphie-file)
;;     (let [base (slurp base-file)
;;           ext  (slurp ralphie-file)]
;;       (->> [base ext]
;;            (string/join "\n")
;;            (spit i3-config-file)))))

;; (defn restart-i3! [] (i3-msg! "restart"))

;; (defn rebuild-and-restart!
;;   "Converts passed workspaces into an i3 config.
;;   The contents is written to i3/config.ralphie,
;;   which is then concattenated with i3.config.base."
;;   [_config _parsed]
;;   ;; TODO try-catch the rebuild, always restart
;;   (rebuild-i3-config!)
;;   (restart-i3!))

;; (defcom rebuild-and-restart-i3
;;   {:name          "restart-i3"
;;    :one-line-desc "Restarts i3 in place"
;;    :description   ["Pulls workspace config from workspaces.org."
;;                    "Writes a new i3/config."
;;                    "Restarts i3."]
;;    :handler       rebuild-and-restart!})

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
   ;; TODO handle position on multiple monitors
   ;; {:label  "right-side"
   ;;  :i3-cmd "floating enable, resize set width 45 ppt height 90 ppt, move position center"}
   ;; {:label  "left-side"
   ;;  :i3-cmd "floating enable, resize set width 40 ppt height 80 ppt, move position center"}
   ])

(defcom resize-window
  {:doctor/depends-on ["i3-msg"]}
  (->> window-size-options
       (rofi/rofi {:msg "Choose window layout type"})
       :i3-cmd
       (i3-msg!)))
