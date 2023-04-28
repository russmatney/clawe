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

(defn repo-todo-paths [repo-ids]
  (->> repo-ids
       (map #(str "~/" % "/{readme,todo,todos}.org"))
       (mapcat zsh/expand-many)
       (filter fs/exists?)))

(comment
  (repo-todo-paths #{"russmatney/clawe" "teknql/fabb" "russmatney/dino" "doesnot/exist"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; local repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def local-repos-root (fs/home))

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

(defn dir-is-repo?
  "Returns true if the passed path is a git repo"
  [repo-path]
  (fs/exists? (str (zsh/expand repo-path) "/.git")))

(comment
  (dir-is-repo? "hi"))

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
  {:commit/hash              "%H"
   :commit/short-hash        "%h"
   ;; :commit/tree             "%T"
   ;; :commit/abbreviated-tree "%t"
   :commit/parent-hash       "%P"
   :commit/parent-short-hash "%p"
   ;; :commit/refs               "%D"
   ;; :commit/encoding           "%e"
   :commit/subject           "%s"
   ;; :commit/sanitized-subject-line "%f"
   :commit/body              "%b"
   :commit/full-message      "%B"
   ;; :commit/commit-notes           "%N"
   ;; :commit/verification-flag      "%G?"
   ;; :commit/signer                 "%GS"
   ;; :commit/signer-key             "%GK"
   :commit/author-name       "%aN"
   :commit/author-email      "%aE"
   :commit/author-date       "%aD"
   ;; :commit/commiter-name          "%cN"
   ;; :commit/commiter-email         "%cE"
   ;; :commit/commiter-date "%cD"
   })

(def delimiter "^^^^^")
(defn log-format-str []
  (str
    "{"
    (->> log-format-keys
         (map (fn [[k v]] (str k " " delimiter v delimiter)))
         (string/join " "))
    "}"))

(defn commits
  "Retuns metadata for `n` commits at the specified `dir`.
  ;; TODO support before/after
  ;; TODO rename, probably just `commits`
  "
  [{:keys [dir path n
           _before _after
           oldest-first]
    :as   opts}]
  (when-not (or dir path)
    ;; TODO can we use timbre in bb?
    (println "WARN ralphie.git/commits needs dir or path" dir path))
  (let [dir  (or (and dir (fs/expand-home dir))
                 ;; do we need to traverse to find nearest git parent?
                 (when path (-> path fs/expand-home fs/parent str)))
        path (or path ".")
        n    (or n 10)

        cmd
        (str
          "git log"
          (when-not oldest-first (str " -n "))
          (when-not oldest-first (str " " n))
          ;; ~(when before (str "--before=" before))
          ;; ~(when after (str "--after=" after))
          " --pretty=format:'" (log-format-str) "'"
          (when path path))]
    (try
      ;; consider url-encoding the content to avoid delimiting
      (->
        (process/process cmd
                         {:out :string
                          ;; TODO this ought to just-work (bb/process handling a unixpath)
                          ;; maybe use fs/expand-home in bb/process ?
                          :dir (str dir)})
        process/check :out
        ((fn [s] (str "[" s "]")))
        ;; pre-precess double quotes (maybe just move to single?)
        (string/replace "\"" "'")
        (string/replace delimiter "\"")
        #_((fn [s] (println s) s))
        edn/read-string
        (cond->>
            dir (map #(assoc % :commit/directory dir))
            path         (map #(assoc % :commit/path path))
            oldest-first reverse
            oldest-first (take n)))
      (catch Exception e
        (println "Error fetching commits for dir" dir opts)
        (println e)
        nil))))

(comment
  (commits {:dir "russmatney/clawe" :n 10 :before "2022-05-06" :after "2022-05-02"})
  (count (commits {:dir "russmatney/clawe" :n 10 :before "2022-05-06" :after "2022-05-02"}))
  (commits {:dir "russmatney/clawe" :n 10})
  (commits {:dir "/Users/russ/russmatney/clawe" :n 10})
  (commits {:dir (zsh/expand "~/russmatney/dotfiles") :n 30})

  (commits {:n 3 :path (zsh/expand "~/todo/journal.org")})
  (->>
    (commits {:n    3 :oldest-first true
              :dir  "~/todo"
              :path (zsh/expand "~/todo/garden/clawe.org")})
    (map (fn [commit]
           (str (:commit/author-date commit) "     \t" (:commit/full-message commit)))))

  (commits {:dir          "~/russmatney/clawe" :n 10
            :oldest-first true}))

(defn ->stats-header [[commit author date]]
  {:commit/hash         (-> commit (string/split #" ") second)
   :commit/author-name  (when author (some-> (re-find #"Author: (.+) <" author) second))
   :commit/author-email (when author (some-> (re-find #"<(.+)>" author) second))
   :commit/author-date  (when date (some-> (re-find #"Date: (.+)$" date) second string/trim))})

(defn safe-read-string
  [str message raw]
  (try
    (read-string str)
    (catch Exception e
      (println message raw)
      (println e)
      nil)))

(defn ->stat-line
  "TODO: parse filenames, renamed files, symlinks, etc"
  ([stat-line] (->stat-line nil stat-line))
  ([dir stat-line]
   (let [[added removed file-line] (string/split stat-line #"\t")
         no-added-diff             (#{"-"} added)
         no-removed-diff           (#{"-"} removed)

         added   (when-not no-added-diff
                   (safe-read-string
                     added (str "Failed to read added str in ->stat-line " dir)
                     stat-line))
         removed (when-not no-removed-diff
                   (safe-read-string
                     removed (str "Failed to read removed str in ->stat-line " dir)
                     stat-line))

         is-rename (if (re-seq #"\{" file-line) true false)]
     (cond->
         {:git.stat/raw-file-line file-line
          :git.stat/is-rename    is-rename
          :git.stat/no-diff (boolean (or no-added-diff no-removed-diff))}

       added   (assoc :git.stat/lines-added added)
       removed (assoc :git.stat/lines-removed removed)))))

(comment
  (->stat-line "3\t5\tsrc/doctor/ui/views/screenshots.cljs")
  (->stat-line "1\t1\tsrc/{doctor/ui => hooks}/screenshots.cljc")
  (->stat-line "-\t-\tassets/robot_sheet.png"))

(defn ->stats
  ([stat-lines] (->stats nil stat-lines))
  ([dir stat-lines]
   (try
     (let [parsed-stat-lines (->> stat-lines (map (partial ->stat-line dir)) (remove nil?))]
       {:commit/lines-added   (->> parsed-stat-lines (map :git.stat/lines-added) (remove nil?) (reduce +))
        :commit/lines-removed (->> parsed-stat-lines (map :git.stat/lines-removed) (remove nil?) (reduce +))
        :commit/files-renamed (->> parsed-stat-lines (filter :git.stat/is-rename) count)
        :commit/stat-lines    parsed-stat-lines})
     (catch Exception e
       (println "Failed to parse ->stats with lines" dir stat-lines)
       (println e)
       nil))))

(comment
  (->stats '("3\t5\tsrc/doctor/ui/views/screenshots.cljs"
             "2\t2\tsrc/doctor/ui/views/todos.cljs"
             "7\t7\tsrc/doctor/ui/views/topbar.cljs"
             "3\t4\tsrc/doctor/ui/views/wallpapers.cljs"
             "5\t5\tsrc/doctor/ui/views/workspaces.cljs"
             "1\t1\tsrc/{doctor/ui => hooks}/screenshots.cljc"
             "1\t1\tsrc/{doctor/ui => hooks}/todos.cljc"
             "1\t1\tsrc/{doctor/ui => hooks}/wallpapers.cljc"
             "3\t6\tsrc/{doctor/ui => hooks}/workspaces.cljc"))

  (->stats '("-\t-\tassets/robot.aseprite"
             "-\t-\tassets/robot_sheet.png"
             "42\t8\tlevels/Arcade.tscn"
             "25\t1\tlevels/Park.tscn"
             "7\t5\tmobs/Mobot.tscn"
             "4\t5\tplayer/Player.gd"
             "48\t35\tplayer/Player.tscn"
             "1\t1\tproject.godot")))

(defn ->stats-commit
  "Parse `git log --numstat` lines.
  We don't care for commit message here - that is handled in `commits`.
  "
  ([x] (->stats-commit nil x))
  ([dir [header _msg stats]]
   (merge
     (->stats-header header)
     (->stats dir stats))))

(defn commit-stats
  "Retuns metadata for `n` commits at the specified `dir`.
  ;; TODO support before/after
  "
  ;; TODO Dry up logic vs commits
  [{:keys [dir n _before _after] :as opts}]
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
        (->> (partition 3) (map (partial ->stats-commit dir))))
      (catch Exception e
        (println "Error fetching commit stats for dir" dir opts)
        (println e)
        nil))))


(comment
  (nth
    (commit-stats {:dir          "russmatney/clawe" :n 10
                   :oldest-first true})
    7)
  (nth
    (commit-stats {:dir "russmatney/beatemup-two" :n 10})
    7))
