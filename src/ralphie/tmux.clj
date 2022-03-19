(ns ralphie.tmux
  (:require
   [babashka.process :as process :refer [$ check]]
   [ralphie.rofi :as rofi]
   [ralphie.notify :refer [notify]]
   [defthing.defcom :refer [defcom] :as defcom]
   [ralphie.sh :as r.sh]
   [ralphie.config :as config]
   [clojure.string :as string]
   [ralphie.awesome :as awm]
   [clojure.edn :as edn]
   [ralphie.notify :as notify]))


;; TODO sync sessions/window/panes with defs/workspaces - similar to awm tags and emacs workspaces

(def tmux-format-keys
  "These are pulled from the tmux man-page, FORMATS section."
  {
   :tmux/pane-active           "?pane_active"
   :tmux/pane-current-command  "pane_current_command"
   :tmux/pane-current-path     "pane_current_path"     ;; Current path if available
   :tmux/pane-dead             "?pane_dead"            ;; 1 if pane is dead
   :tmux/pane-dead-status      "pane_dead_status"      ;; Exit status of process in dead pane
   :tmux/pane-id               "pane_id"               ;; Unique pane ID
   :tmux/pane-in-mode          "?pane_in_mode"         ;; 1 if pane is in a mode
   :tmux/pane-index            "pane-index"
   :tmux/pane-path             "pane_path"             ;; Path of pane (can be set by application)
   :tmux/pane-pid              "pane_pid"              ;; PID of first process in pane
   :tmux/pane-synchronized     "?pane_synchronized"    ;; 1 if pane is synchronized
   :tmux/pane-start-command    "pane_start_command"    ;; Command pane started with
   :tmux/pane-tabs             "pane_tabs"             ;; Pane tab positions
   :tmux/pane-title            "pane_title"            ;; Title of pane (can be set by application)
   :tmux/pane-tty              "pane_tty"              ;; Pseudo terminal of pane
   :tmux/session-activity      "session_activity"      ;; Time of session last activity
   :tmux/session-created       "session_created"       ;; Time session created
   :tmux/session-id            "session_id"            ;; Unique session ID
   :tmux/session-name          "session_name"          ;; Name of session
   :tmux/session-path          "session_path"          ;; Working directory of session
   :tmux/session-stack         "session_stack"         ;; Window indexes in most recent order
   :tmux/session-windows       "session_windows"       ;; Number of windows in session
   :tmux/active-window-index   "active_window_index"   ;; Index of active window in session
   :tmux/window-active         "?window_active"        ;; 1 if window active
   :tmux/window-activity       "window_activity"       ;; Time of window last activity
   :tmux/window-activity-flag  "?window_activity_flag" ;; 1 if window has activity
   :tmux/window-bell-flag      "?window_bell_flag"     ;; 1 if window has bell
   :tmux/window-id             "window_id"             ;; Unique window ID
   :tmux/window-index          "window_index"          ;; Index of window
   :tmux/window-last-flag      "?window_last_flag"     ;; 1 if window is the last used
   :tmux/window-layout         "window_layout"         ;; Window layout description, ignoring zoomed window panes
   :tmux/window-name           "window_name"
   :tmux/window-panes          "window_panes"          ;; Number of panes in window
   :tmux/window-silence-flag   "?window_silence_flag"  ;; 1 if window has silence alert
   :tmux/window-stack-index    "window_stack_index"    ;; Index in session most recent stack
   :tmux/window-start-flag     "?window_start_flag"    ;; 1 if window has the lowest index
   :tmux/window-visible-layout "window_visible_layout" ;; Window layout description, respecting zoomed window panes
   })

;; TODO unit tests
(defn tmux-format-str
  "Returns a format string that can be `edn/read-string`-ed into clj objs.

  Defaults to including all supported format keys (all data),
  but a set or map of keys can be passed to limit the returned data.
  "
  ([] (tmux-format-str nil))
  ([selected-formats]
   (let [selected-formats (or selected-formats tmux-format-keys)
         format-syms-and-keys
         (->> tmux-format-keys
              (filter (comp selected-formats first))
              (map (fn [[key fmt-key]]
                     (str key " \"#{" fmt-key "}\"")
                     (str key " " (if (re-seq #"^\?" fmt-key)
                                    ;; `?` prefix for now implies we want a boolean
                                    ;; may one day want something for ints/indexes
                                    ;; this lets edn/read-string read a bool
                                    (str "#{" fmt-key ",true,false}")
                                    (str "\"#{" fmt-key "}\"")))))
              (string/join " "))]
     (str "{" format-syms-and-keys "}"))))


(defn list-panes
  ([] (list-panes nil))
  ([{:tmux/keys [formats target]}]
   (->
     (process/sh
       (->> ["tmux" "list-panes"
             (when-not target "-a") ;; list all panes unless a target is passed
             "-F"
             (tmux-format-str formats)
             (when target "-t")
             (when target target)]
            (remove nil?)))
     check :out string/split-lines (->> (map edn/read-string))) ))

(comment
  (tmux-format-str nil)
  (str "hi " (tmux-format-str nil))
  (list-panes)
  (list-panes {:tmux/formats #{:tmux/pane-current-command
                               :tmux/pane-active}})
  (list-panes {:tmux/formats #{:tmux/pane-current-command
                               :tmux/pane-index}
               :tmux/target  "clawe:."})
  )


(defn has-session? [name]
  (-> @($ tmux has-session -t ~name)
      :exit
      (= 0)))

(defn kill-session [name]
  (notify/notify "Killing session" name)
  (-> ^{:out :string :err :string}
      ($ tmux kill-session -t ~name)
      check
      :out))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; new session
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn new-session [{:tmux/keys [session-name window-name directory] :as opts}]
  (notify "Attempt to create new tmux session" opts)
  (when session-name
    (->
      ^{:out :string}
      ($ tmux new-session -d
         -c ~(if directory directory "~")
         -s ~session-name
         -n ~(or window-name session-name))
      check
      :out)
    (notify "Created new tmux session" opts)))

(comment
  (has-session? {:name "ralphie"}))

(defn ensure-session
  "Creates a tmux session in the background."
  [{:tmux/keys [session-name] :as opts}]
  ;; TODO :tmux/ prefix these opts
  ;; TODO can probably do this with one tmux command
  (when-not (has-session? session-name)
    (new-session opts)))

(comment
  (ensure-session {:tmux/session-name "mysess"
                   :tmux/directory    "~/russmatney/clawe"})

  (kill-session "mysess")
  (kill-session "mynewsession")

  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fire command
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-available-pane
  "Returns a pane running zsh. Expects either a list of panes or a target.

  Attempts to select an active pane from the list. If the active pane is 'busy',
  another pane `zsh` running zsh will be selected (if available)."
  ([{:tmux/keys [panes target]}]
   (let [panes       (or panes
                         (list-panes
                           {:tmux/formats #{:tmux/pane-current-command
                                            :tmux/pane-index}
                            :tmux/target  target}))
         active-pane (some->> panes (filter :tmux/pane-active) first)]
     (if (-> active-pane :tmux/pane-current-command #{"zsh"})
       ;; return active pane if it is available
       active-pane

       ;; otherwise, return first available pane
       (some->> panes
                (filter (comp #{"zsh"} :tmux/pane-current-command))
                first)))))

(comment
  (get-available-pane "clawe:.")
  )

(defn fire
  "Aka tmux send-keys, but with handling for selecting/interrupting panes.

  Useful because:
  - the output is accessible in a tmux session, which might be desired
  - the workspace's session is usually in the expected directory already
  - commands can be fired from multiple places (emacs, a keybinding), and 'land'
  in the same place.

  The session defaults to the current workspace name.
  The window and pane are the 'active', i.e. the last one interacted with.
  If the pane is busy (i.e. `:tmux/pane-current-command` is not \"zsh\"),
  a new pane will be created and the command will run in there.
  If `:tmux/interrupt?` is true, the active pane will be sent `C-c` first
  to kill whatever is running, and that pane will be used.
  "
  ([opts-or-str]
   (cond
     (map? opts-or-str)
     (fire nil opts-or-str)

     (string? opts-or-str)
     (fire opts-or-str nil)))
  ([cmd-str opts]
   (let [{:tmux/keys [fire session-name
                      window-name window-index
                      pane-name interrupt?]} (or opts {})

         cmd-str      (or cmd-str fire)
         ;; fallback to a session in the current tag/workspace
         session-name (or session-name (awm/current-tag-name))
         window-name  (or window-name window-index)

         ;; leave the window/pane empty to get the last-active ones
         initial-target (str session-name ":" window-name "." pane-name)]

     (when-not (has-session? initial-target)
       (ensure-session {:tmux/session-name session-name}))

     (let [panes          (list-panes
                            {:tmux/formats #{:tmux/pane-current-command
                                             :tmux/pane-index}
                             :tmux/target  initial-target})
           available-pane (when (seq panes)
                            (get-available-pane {:tmux/target initial-target
                                                 :tmux/panes  panes}))

           ;; get a non-busy target pane
           non-busy-target (cond
                             (empty? panes)
                             (do
                               ;; this should never happen - missing panes means a new session should have been created
                               (notify/notify "No panes :/" "This should never happen!")
                               initial-target)

                             available-pane
                             (str session-name ":" window-name "." (:tmux/pane-index available-pane))

                             interrupt?
                             (do
                               ;; interrupt whatever's running
                               (notify/notify "Tmux/fire interrupting pane!")
                               (-> ($ tmux send-keys "-t" ~initial-target C-c) check)
                               initial-target)

                             :else
                             (do
                               ;; split the window and get that pane's index
                               (notify/notify "Creating new pane")
                               (-> ($ tmux split-window "-t" ~initial-target) check)
                               ;; is this a race-case? may need to wait if zsh is slow?
                               ;; could also loop-recur with a delay?
                               ;; seems like the keys send and fire as soon as zsh is ready
                               (let [available-pane (get-available-pane {:tmux/target initial-target})]
                                 (str session-name ":" window-name "." (:tmux/pane-index available-pane)))))]
       (if-not cmd-str
         (notify "invalid tmux/fire! called" cmd-str opts)
         (try
           (do
             (notify "tmux/fire!" {:tmux/session-name session-name
                                   :tmux/cmd-str      cmd-str
                                   :tmux/target       non-busy-target})

             (->
               ^{:out :string}
               ($ tmux send-keys
                  -t ~non-busy-target
                  ;; -R ;; resets the terminal state
                  ~cmd-str C-m)
               check))
           (catch Exception e
             (println "tmux/fire Exception" e)
             (throw e))))))))

(comment
  ;; should fire in the current workspace's tmux session
  ;;   if the active pane is busy, a new one should be created and fired in there
  ;;   if interrupt?, the busy pane should be C-c and re-used
  ;;   if the specified session does not exist, it should be created and the command run in there

  (fire "echo sup")
  (fire "bb log-awesome")
  (fire "echo sup" {:tmux/interrupt? true})
  (fire "bb log-awesome" {:tmux/interrupt? true})
  (fire "notify-send.py supject supdy")

  ;; should create and run in non-existent session
  (fire "notify-send.py supject supdy" {:tmux/session-name "mysess"})
  (kill-session "mysess")

  (ensure-session {:tmux/session-name "mysess"
                   :tmux/directory    "~/russmatney/clawe"}))

(defcom fire-cmd
  "Fires a command in the nearest tmux shell."
  (fn [_cmd & args]
    (let [cmd (if (seq args)
                (first args)
                (rofi/rofi {:msg "Command to fire"} (rofi/zsh-history)))]
      (fire cmd))))

(defcom fire-cmd-with-interrupt
  "Fires a command in the nearest tmux shell, interrupting the active pane
if it is busy."
  (fn [_cmd & args]
    (let [cmd (if (seq args)
                (first args)
                (rofi/rofi {:msg "Command to fire"} (rofi/zsh-history)))]
      (fire {:tmux/interrupt? true
             :tmux/fire       cmd}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; New window
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn open-session
  "Creates a session in a new alacritty window."
  ([] (open-session {:tmux/name "ralphie-fallback" :tmux/directory "~"}))
  ([{:tmux/keys [name session-name directory]}]
   (let [directory    (if directory
                        (r.sh/expand directory)
                        (config/home-dir))
         session-name (or session-name name)
         ;; window-name  (or window-name name)
         ]

     ;; NOTE `check`ing or derefing this won't release until
     ;; the alacritty window is closed. Not sure if there's a better
     ;; way to disown the process without skipping error handling
     (-> ($ alacritty --title ~session-name -e tmux "new-session" -A
            ~(when directory "-c") ~(when directory directory)
            -s ~session-name)
         check))))

(comment
  (open-session {:tmux/name "name"})
  (open-session {:tmux/name "name" :tmux/directory "~/russmatney"}))
