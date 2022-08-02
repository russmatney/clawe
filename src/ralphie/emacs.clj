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
  "Opens a new emacs client in the passed workspace.

  Uses the passed workspace data to direct emacs to the relevant initial file
  and named emacs workspace.
  "
  ([] (open nil))
  ([wsp]
   (let [wsp          (or wsp {})
         wsp-name
         (or (some wsp [:emacs.open/workspace
                        :workspace/title :org/name :clawe.defs/name])
             "ralphie-fallback")
         initial-file (some wsp [:emacs.open/file :emacs/open-file :workspace/initial-file])
         initial-file (determine-initial-file initial-file)
         elisp-hook   (:emacs.open/elisp-hook wsp)
         eval-str     (str
                        "(progn "
                        ;; TODO refactor russ/open-workspace to support initial-file
                        ;; so that we don't open the readme when the workspace is already open
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

(defn open-in-emacs
  "Opens a file in the last-focused existing emacs client.
  Expects an absolute file-path."
  [opts]
  (let [file-path (some opts [:emacs/file-path])
        eval-str  (str
                    "(progn " (when file-path (str " (find-file \"" file-path "\") " " ")) " )")]
    (-> ($ emacsclient --no-wait --eval ~eval-str)
        check)))

(comment
  (def --file "/Users/russ/russmatney/clawe/src/api/emacs.clj")

  (open-in-emacs
    {:emacs/file-path --file})
  )
