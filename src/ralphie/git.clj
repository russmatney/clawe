(ns ralphie.git
  (:require
   [babashka.process :refer [$ check] :as process]
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [defthing.defcom :refer [defcom] :as defcom]
   [ralphie.notify :refer [notify]]
   [ralphie.rofi :as rofi]
   [ralphie.config :as config]
   [ralphie.clipboard :as clipboard]
   [ralphie.browser :as browser]
   [ralphie.re :as re]
   [ralphie.zsh :as zsh]
   [ralphie.bb :as bb]
   [clojure.string :as string]
   [ralphie.tmux :as tmux]
   [clojure.edn :as edn]
   [util :as util]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; local repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def local-repos-root (zsh/expand "~"))

(defn local-repos
  "Returns a list of absolute paths to local git repos"
  []
  (->> (bb/run-proc
         {:error-message (str "RALPHIE ERROR fetching local repos")}
         ^{:dir local-repos-root}
         ($ ls -a))
       ;; TODO run in parallel
       ;; or just memoize?
       (mapcat (fn [home-dir]
                 (-> (str "~/" home-dir "/*/.git")
                     zsh/expand
                     (string/split #" "))))
       ;; remove failed expansions
       (remove (fn [path] (string/includes? path "/*/")))))

(comment
  (count
    (local-repos)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; transforms
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn repo->rofi-x [{:keys [repo-id language description] :as repo}]
  (assoc repo :rofi/label (str repo-id " (recent star) | " language " | " description)))

(defn star->repo [star]
  (let [owner     (get-in star ["owner" "login"])
        repo-name (get star "name")]
    {:owner       owner
     :repo-id     (str owner "/" repo-name)
     :description (get star "description")
     :language    (get star "language")
     :fork?       (get star "fork")
     :star-count  (get star "stargazers_count")
     :watch-count (get star "watchers_count")}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fetch github stars
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO cache and delimit this to 5/10 mins, and support a cache bust with a
;; webhook for new stars
(defn fetch-stars
  "Returns the 30 most recent github stars for the user.
  TODO: support sorting by :updated-at (repo last updated)
  might be better to pull both here, and update a local db with whatever we get,
  probably during the cache bust."
  []
  (->> (config/github-username)
       (#(str "https://api.github.com/users/" % "/starred"))
       slurp
       json/parse-string))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clone
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clone [{:keys [repo-id]}]
  (notify (str "Clone attempt: " repo-id))
  (try
    (->> ($ gh repo clone ~(str "https://github.com/" repo-id) ~(str (config/home-dir) "/" repo-id))
         check
         :out
         slurp
         (notify (str "Successful clone: " repo-id)))
    (catch Exception e
      (notify "Error while cloning" e)
      (println e))))

(comment
  (clone {:repo-id "metosin/eines"})
  (clone {:repo-id "russmatney/ink-mode"}))

(defn rofi-clone-suggestions-fast []
  (concat
    (->> (clipboard/values)
         (map (fn [v]
                (when-let [repo-id (re/url->repo-id v)]
                  {:repo-id        repo-id
                   :rofi/label     (str repo-id " (from clipboard)")
                   :rofi/tag       "clone"
                   :rofi/on-select clone})))
         (filter :repo-id))
    (->> (browser/tabs)
         (map (fn [t]
                (when-let [repo-id (re/url->repo-id (:tab/url t))]
                  {:repo-id        repo-id
                   :rofi/label     (str repo-id " (from open tabs)")
                   :rofi/tag       "clone"
                   :rofi/on-select clone})))
         (filter :repo-id))))

(defn rofi-clone-suggestions []
  (concat
    (rofi-clone-suggestions-fast)
    (->> (fetch-stars)
         ;; TODO pango markup describing source (recent star)
         (map star->repo)
         (map repo->rofi-x)
         (map (fn [s] (merge s {:rofi/on-select clone
                                :rofi/tag       "clone"}))))))

(defn clone-from-suggestions []
  (->> (rofi-clone-suggestions)
       (rofi/rofi {:message        "Select repo to clone"
                   ;; if it ever comes to that
                   :rofi/on-select clone})))

(comment
  (dorun (map println (clone-from-suggestions))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clone cmd, handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom clone-cmd
  "Clone from your Github Stars"
  "When passed a repo-id, copies it into ~/repo-id."
  "Depends on `gh` on the command line."
  "Does not support private repos."
  "If no repo-id is passed, fetches stars from github."
  (fn [_cmd & args]
    (if-let [repo-id (some-> args first)]
      (clone {:repo-id repo-id})
      (clone-from-suggestions))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; repo?
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn repo?
  "Returns true if the passed path is a git repo"
  [repo-path]
  (fs/exists? (str (zsh/expand repo-path) "/.git")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fetch
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch [repo-path]
  (notify (str "Fetching " repo-path))
  (-> {:read-key :err}
      (bb/run-proc
        ^{:dir (zsh/expand repo-path)}
        ($ git "fetch" --verbose))
      (->>
        (apply notify))))

(comment
  (fetch "~/dotfiles")

  (-> ^{:dir (zsh/expand "~/russmatney/dotfiles")}
      ($ git "fetch" --verbose)
      check :err slurp))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; update local repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def local-repo-group-dirs
  [(zsh/expand "~/russmatney")
   (zsh/expand "~/teknql")])

(defn update-local-repos
  "Updates local repo refs using git-summary.

  git-summary repo: https://github.com/MirkoLedda/git-summary
  Expects git-summary to exist on the PATH.

  If run in directory `parent-dir`, git-summary runs a fetch in all children,
  effectively a git fetch in all `parent-dir/*`.

  See `local-repo-group-dirs` for parent-dirs that this command runs in.

  This function uses tmux-fire rather than clojure/shell or bb/process to let
  the running shell/tmux-session handle the auth.
  "
  []
  (notify "Updating local repo refs via git-summary" local-repo-group-dirs)
  (for [dir local-repo-group-dirs]
    (tmux/fire {:tmux.fire/cmd     (str "cd " dir " && git-summary")
                :tmux.fire/session "git-summary"})))

(defcom update-local-repos-cmd
  "Updates local repo refs using git-summary."
  (update-local-repos))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dirty/is-clean?
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare is-clean?)
(defn dirty? [x] (-> x is-clean? not))
(defn is-clean?
  "Returns true if there is no local diff in the passed path.
  Expects a .git directory at <path>/.git"
  [repo-path]
  (-> {:error-message
       (str "RALPHIE ERROR for " repo-path " in git/dirty?")}
      (bb/run-proc
        ^{:dir (zsh/expand repo-path)}
        ($ git status --porcelain))
      empty?))

(comment
  (is-clean? "~/russmatney/ralphie")
  (dirty? "~/russmatney/ralphie")

  (let [repo-path "~/russmatney/ralphie"]
    (-> {:error-message
         (str "RALPHIE ERROR for " repo-path " in git/dirty?")}
        (bb/run-proc
          ^{:dir (zsh/expand repo-path)}
          ($ git status --porcelain))))

  (-> ^{:dir (zsh/expand "~/russmatney/ralphie")}
      ($ git diff HEAD)
      check :out slurp
      empty?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; needs-push?
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn needs-push? [repo-path]
  (-> {:error-message
       (str "RALPHIE ERROR for " repo-path " in git/needs-push?")}
      (bb/run-proc
        ^{:dir (zsh/expand repo-path)}
        ($ git status))
      (->>
        (filter #(re-seq #"Your branch is ahead" %))
        seq)))

(comment
  (needs-push? "~/russmatney/ralphie"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; needs-pull?
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn needs-pull?
  "Returns true if git status reports that we are behind.
  NOTE that no fetch is made in this function, it only parses
  the current git status so the origin reference may be
  out of date. You can use `(git/fetch repo-path)` to update the repo's ref."
  [repo-path]
  (-> {:error-message
       (str "RALPHIE ERROR for " repo-path " in git/needs-push?")}
      (bb/run-proc
        ^{:dir (zsh/expand repo-path)}
        ($ git status))
      (->>
        (filter #(re-seq #"branch is behind" %))
        seq)))

(comment
  (needs-pull? "~/russmatney/dotfiles"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; last fetch
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn last-fetch-timestamp [repo-path]
  (some->
    (bb/run-proc
      {:error-message
       (str "RALPHIE ERROR for " repo-path " in git/last-fetch-timestamp")}
      ^{:dir (zsh/expand repo-path)}
      ($ stat -c %Y .git/FETCH_HEAD))
    first
    Integer/parseInt))

(comment
  (last-fetch-timestamp "~/russmatney/clawe")
  (last-fetch-timestamp (zsh/expand "~/russmatney/lifeofbob")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; status
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn status [repo-path]
  (let [res         (-> {:error-message
                         (str "RALPHIE ERROR for " repo-path " in git/status")}
                        (bb/run-proc
                          ^{:dir (zsh/expand repo-path)}
                          ($ git status))
                        seq)
        dirty?      (->> res (filter #(re-seq #"not staged for commit" %)) seq)
        needs-pull? (->> res (filter #(re-seq #"branch is behind" %)) seq)
        needs-push? (->> res (filter #(re-seq #"branch is ahead" %)) seq)]
    {:git/dirty?      dirty?
     :git/needs-pull? needs-pull?
     :git/needs-push? needs-push?
     ;; :git/last-fetch-timestamp (last-fetch-timestamp repo-path)
     }))

(comment
  (status "~/russmatney/dotfiles")
  (status "~/russmatney/lifeofbob")
  (status "~/todo"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; commits
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def log-format-keys
  {:git.commit/hash              "%H"
   :git.commit/short-hash        "%h"
   ;; :git.commit/tree             "%T"
   ;; :git.commit/abbreviated-tree "%t"
   :git.commit/parent-hash       "%P"
   :git.commit/parent-short-hash "%p"
   ;; :git.commit/refs               "%D"
   ;; :git.commit/encoding           "%e"
   :git.commit/subject           "%s"
   ;; :git.commit/sanitized-subject-line "%f"
   :git.commit/body              "%b"
   :git.commit/full-message      "%B"
   ;; :git.commit/commit-notes           "%N"
   ;; :git.commit/verification-flag      "%G?"
   ;; :git.commit/signer                 "%GS"
   ;; :git.commit/signer-key             "%GK"
   :git.commit/author-name       "%aN"
   :git.commit/author-email      "%aE"
   :git.commit/author-date       "%aD"
   ;; :git.commit/commiter-name          "%cN"
   ;; :git.commit/commiter-email         "%cE"
   ;; :git.commit/commiter-date "%cD"
   })
(def delimiter "^^^^^")
(defn log-format-str []
  (str
    "{"
    (->> log-format-keys
         (map (fn [[k v]] (str k " " delimiter v delimiter)))
         (string/join " "))
    "}"))

(defn commits-for-dir
  "Retuns metadata for `n` commits at the specified `dir`.
  ;; TODO support before/after
  "
  [{:keys [dir n before after] :as opts}]
  (let [dir (if (string/starts-with? dir "/")
              dir (zsh/expand "~/" dir))
        n   (or n 10)]
    (try
      (->
        ^{:out :string :dir dir}
        (process/$
          git log
          -n ~n
          ;; ~(when before (str "--before=" before))
          ;; ~(when after (str "--after=" after))
          ~(str "--pretty=format:" (log-format-str)))
        process/check :out
        ((fn [s] (str "[" s "]")))
        (string/replace delimiter "\"")
        edn/read-string
        (->> (map #(assoc % :git.commit/directory dir))))
      (catch Exception e
        (println "Error fetching commits for dir" opts)
        (println e)
        nil))))

(comment
  (commits-for-dir {:dir "russmatney/clawe" :n 10 :before "2022-05-06" :after "2022-05-02"})
  (count (commits-for-dir {:dir "russmatney/clawe" :n 10 :before "2022-05-06" :after "2022-05-02"}))
  (commits-for-dir {:dir "russmatney/clawe" :n 10})
  (commits-for-dir {:dir "/Users/russ/russmatney/clawe" :n 10})
  (commits-for-dir {:dir "/Users/russ/russmatney/dotfiles" :n 10}))

(defn ->stats-header [[commit author date]]
  {:git.commit/hash         (-> commit (string/split #" ") second)
   :git.commit/author-name  (some-> (re-find #"Author: (.+) <" author) second)
   :git.commit/author-email (some-> (re-find #"<(.+)>" author) second)
   :git.commit/author-date  (some-> (re-find #"Date: (.+)$" date) second string/trim)})

(defn ->stat-line
  "TODO: parse filenames, renamed files, symlinks, etc"
  [stat-line]
  (let [[added removed file-line] (string/split stat-line #"\t")
        is-rename?                (if (re-seq #"\{" file-line) true false)]
    (try
      {:git.stat/lines-added   (read-string added)
       :git.stat/lines-removed (read-string removed)
       :git.stat/raw-file-line file-line
       :git.stat/is-rename?    is-rename?}
      (catch Exception e
        (println "Failed to parse ->stat-line" stat-line)
        (println e)
        nil))))

(comment
  (->stat-line "3\t5\tsrc/doctor/ui/views/screenshots.cljs")
  (->stat-line "1\t1\tsrc/{doctor/ui => hooks}/screenshots.cljc"))

(defn ->stats [stat-lines]
  (try
    (let [parsed-stat-lines (->> stat-lines (map ->stat-line) (remove nil?))]
      {:git.commit/lines-added   (->> parsed-stat-lines (map :git.stat/lines-added) (reduce +))
       :git.commit/lines-removed (->> parsed-stat-lines (map :git.stat/lines-removed) (reduce +))
       :git.commit/files-renamed (->> parsed-stat-lines (filter :git.stat/is-rename?) count)
       :git.commit/stat-lines    parsed-stat-lines})
    (catch Exception e
      (println "Failed to parse ->stats with lines" stat-lines)
      (println e)
      nil)))

(comment
  (->stats '("3\t5\tsrc/doctor/ui/views/screenshots.cljs"
             "2\t2\tsrc/doctor/ui/views/todos.cljs"
             "7\t7\tsrc/doctor/ui/views/topbar.cljs"
             "3\t4\tsrc/doctor/ui/views/wallpapers.cljs"
             "5\t5\tsrc/doctor/ui/views/workspaces.cljs"
             "1\t1\tsrc/{doctor/ui => hooks}/screenshots.cljc"
             "1\t1\tsrc/{doctor/ui => hooks}/todos.cljc"
             "1\t1\tsrc/{doctor/ui => hooks}/wallpapers.cljc"
             "3\t6\tsrc/{doctor/ui => hooks}/workspaces.cljc")))

(defn ->stats-commit
  "Parse `git log --numstat` lines.
  We don't care for commit message here - that is handled in `commits-for-dir`.
  "
  [[header _msg stats]]
  (merge
    (->stats-header header)
    (->stats stats)))

(defn commit-stats-for-dir
  "Retuns metadata for `n` commits at the specified `dir`.
  ;; TODO support before/after
  "
  [{:keys [dir n before after] :as opts}]
  (let [dir (if (string/starts-with? dir "/")
              dir (zsh/expand "~/" dir))
        n   (or n 10)]
    (try
      (->
        ^{:out :string :dir dir}
        (process/$ git log -n ~n --numstat)
        process/check :out
        string/split-lines
        util/partition-by-newlines
        (->> (partition 3) (map ->stats-commit)))
      (catch Exception e
        (println "Error fetching commits for dir" opts)
        (println e)
        nil))))


(comment
  (nth
    (commit-stats-for-dir {:dir "russmatney/clawe" :n 10})
    7))
