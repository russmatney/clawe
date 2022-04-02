(ns ralphie.emacs
  (:require
   [ralphie.notify :refer [notify]]
   [babashka.process :as process :refer [$ check]]
   [defthing.defcom :refer [defcom] :as defcom]
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

(defn eval-form
  "Expects a string."
  [form]
  (-> ($ emacsclient --eval ~form)
      check))

(def fire eval-form)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Open emacs client for passed workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def fallback-default-files
  ["readme.md"
   "readme.org"
   "deps.edn"
   "shadow-cljs.edn"
   "package.json"])

(defn determine-initial-file
  "Initial-file should be a file in the workspace repo's root.
  If it exists, it is returned.
  If it does not exist, sibling `fallback-default-files` are sought out.
  The first to exist is used.
  "
  [initial-file]
  (when initial-file
    (if (fs/exists? initial-file)
      initial-file
      (let [dir (fs/parent initial-file)

            lower-case-f->f (->> (fs/list-dir dir)
                                 (map str)
                                 (map (fn [f] [(string/lower-case f) f]))
                                 (into {}))]
        (->> fallback-default-files
             (map #(str (string/lower-case dir) "/" %))
             (filter lower-case-f->f)
             first
             lower-case-f->f)))))

(comment
  (determine-initial-file (zsh/expand "~/russmatney/clawe/some.blah"))
  (determine-initial-file (zsh/expand "~/borkdude/babashka/readme.org"))
  )

(defn open
  "Opens a new emacs client in the passed workspace.

  Uses the passed workspace data to direct emacs to the relevant initial file
  and named emacs workspace.
  "
  ([] (open nil))
  ([wsp]
   (let [wsp          (or wsp {})
         wsp-name
         (or (some wsp [:emacs.open/workspace :emacs/workspace-name
                        :workspace/title :org/name :clawe.defs/name])
             "ralphie-fallback")
         initial-file (some wsp [:emacs.open/file :emacs/open-file
                                 :workspace/initial-file :org.prop/initial-file])
         initial-file (determine-initial-file initial-file)
         elisp-hook   (:emacs.open/elisp-hook wsp)
         eval-str     (str
                        "(progn "
                        (when wsp-name
                          (str " (russ/open-workspace \"" wsp-name "\") "))
                        (when initial-file
                          (str " (find-file \"" initial-file "\") " " "))
                        (when elisp-hook elisp-hook)
                        " )")]

     (when-not (emacs-server-running?)
       (notify {:notify/subject          "Initializing Emacs Server, initializing."
                :notify/replaces-process "init-emacs-server"})
       (initialize-emacs-server)
       (notify {:notify/subject          "Started Emacs Server"
                :notify/replaces-process "init-emacs-server"}))

     (-> ($ emacsclient --no-wait --create-frame
            -F ~(str "((name . \"" wsp-name "\"))")
            --display=:0
            --eval ~eval-str)
         check))))

(comment
  (open {:emacs.open/workspace "clawe"
         :emacs.open/file      (zsh/expand "~/russmatney/clawe/readme.org")}))

(defcom open-emacs
  "Opens emacs in the current workspace"
  (fn [_cmd & [n & _]]
    (if n
      (open {:emacs/workspace-name n})
      (open))))

(comment
  (defcom/exec open-emacs))
