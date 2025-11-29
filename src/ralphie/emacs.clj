(ns ralphie.emacs
  (:require
   [ralphie.notify :as notify :refer [notify]]
   [babashka.process :as process :refer [$ check]]
   [clojure.string :as string]
   [babashka.fs :as fs]
   [ralphie.zsh :as zsh]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; emacs server/client fns
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn emacs-server-running? []
  (try
    (-> ($ emacsclient -a false -e 't')
        check :out slurp string/trim (= "t"))
    (catch Exception _e
      false)))

(defn initialize-emacs-server []
  (->
    (process/$ systemctl restart --user emacs)
    (process/check))
  ;; (r.sh/zsh
  ;;   (str "emacsclient --alternate-editor='' --no-wait --create-frame"
  ;;        " -e '(delete-frame)'"))
  )

(defn fire
  "Expects a string, passes it to emacsclient --eval."
  [form]
  (-> ($ emacsclient --eval ~form)
      check))

(defn ensure-workspace
  [{:emacs/keys [workspace-name]}]
  (fire (str
          "(unless (+workspace-exists-p \"" workspace-name "\")
             (+workspace-new \"" workspace-name "\"))")))

(comment
  (ensure-workspace {:emacs/workspace-name "newnewwsp"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Open emacs client for passed workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def fallback-default-files
  ["readme.md"
   "readme.org"
   "deps.edn"
   "shadow-cljs.edn"
   "package.json"
   "bb.edn"])

(defn determine-initial-file
  "Initial-file should be a file in the workspace repo's root.
  If it exists, it is returned.
  If it does not exist, sibling `fallback-default-files` are sought out.
  The first to exist is used.
  "
  [initial-file]
  (when initial-file
    (let [initial-file (if (string/starts-with? initial-file "~")
                         (zsh/expand initial-file)
                         initial-file)]
      (if (and (fs/exists? initial-file) (not (fs/directory? initial-file)))
        initial-file
        (if-let [dir (if (fs/directory? initial-file)
                       initial-file
                       (fs/parent initial-file))]

          (let [lower-case-f->f (->> (fs/list-dir dir)
                                     (map str)
                                     (map (fn [f] [(string/lower-case f) f]))
                                     (into {}))]
            ;; TODO refactor to partial match on fallbacks, or support a fn/includes for that
            (->> fallback-default-files
                 (map #(str (string/lower-case dir) "/" %))
                 (filter lower-case-f->f)
                 first
                 lower-case-f->f))
          (println "deter initial file fail" initial-file))))))

(comment
  (determine-initial-file (zsh/expand "~/russmatney/clawe/some.blah"))
  (determine-initial-file (zsh/expand "~/russmatney/clawe"))
  (fs/directory? (zsh/expand "~/russmatney/clawe"))
  (determine-initial-file (zsh/expand "~/borkdude/babashka/readme.org")))

(defn open
  "An opinionated emacs open command.

  Opens a new emacs client with opts from the passed workspace.

  Uses the passed workspace data to direct emacs to the relevant initial file
  and named emacs workspace."
  {:org.babashka/cli
   {:alias {:wsp  :workspace/title
            :file :emacs.open/file}}}
  ([] (open nil))
  ([wsp]
   (try
     (let [wsp                 (or wsp {})
           ignore-dead-server? (:emacs.open/ignore-dead-server? wsp false)
           wsp-name
           (or (some wsp [:emacs.open/workspace
                          :workspace/title :org/name :clawe.defs/name])
               "ralphie-fallback")
           initial-file        (some wsp [:emacs.open/file :emacs.open/directory
                                          :workspace/initial-file])
           initial-file        (determine-initial-file initial-file)
           elisp-hook          (:emacs.open/elisp-hook wsp)
           eval-str            (str
                                 "(progn "
                                 ;; TODO refactor russ/open-workspace to support initial-file
                                 ;; so that we don't open the readme when the workspace is already open
                                 (when wsp-name
                                   (str " (russ/open-workspace \"" wsp-name "\") "))
                                 (when initial-file
                                   ;; TODO consider a 'daily file' pattern here, even searching to find one
                                   (str " (find-file \"" initial-file "\") " " "))
                                 (when elisp-hook elisp-hook)
                                 " )")]

       (when (and (not (emacs-server-running?)) (not ignore-dead-server?))
         (notify {:notify/subject "Initializing Emacs Server, initializing."
                  :notify/id      "init-emacs-server"})
         (initialize-emacs-server)
         (notify {:notify/subject "Started Emacs Server"
                  :notify/id      "init-emacs-server"}))

       (-> ($ emacsclient --no-wait --create-frame
              -F ~(str "((name . \"" wsp-name "\"))")
              --display=:0
              --eval ~eval-str)
           check))
     ;; TODO proper clawe error log
     (catch Exception e
       (notify/notify "emacs/open error" (str e))
       (println e)))))

(comment
  (open {:emacs.open/workspace "clawe"
         :emacs.open/file      (zsh/expand "~/russmatney/clawe/readme.org")}))

(defn open-in-emacs-str [opts]
  (let [file-path  (some opts [:emacs/file-path :path])
        frame-name (:emacs/frame-name opts)]
    (str
      "(progn "
      (when frame-name (str
                         "\n(let ((named-frame (car
(filtered-frame-list (lambda (frame) (equal (frame-parameter frame 'name) \"" frame-name "\"))))))
                                          (if named-frame
                                            (select-frame named-frame)))"))
      (when file-path (str "\n(find-file \"" file-path "\") " " ")) " )")))

(comment
  (open-in-emacs-str
    {:emacs/file-path  "some-file-path"
     :emacs/frame-name "journal"}))

;; TODO is this redundant with `emacs/open` above? the above creates a new client, maybe this name should change
(defn open-in-emacs
  "Opens a file in the last-focused existing emacs client.
  Expects an absolute file-path."
  [opts]
  (let [eval-str (open-in-emacs-str opts)]
    (-> ($ emacsclient --no-wait --eval ~eval-str)
        check)))

(comment
  (def --file "/Users/russ/russmatney/clawe/src/api/emacs.clj")

  (open-in-emacs
    {:emacs/file-path --file}))
