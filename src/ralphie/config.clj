(ns ralphie.config
  (:require [ralphie.sh :refer [expand]]))

;; TODO support some kind of configuration

(defn home-dir [] (expand "~"))

(defn project-dir [] (expand "~/russmatney/ralphie"))
(defn src-dir [] (expand "~/russmatney/ralphie/src"))

(defn local-bin-dir [] (expand "~/.local/bin"))
(defn ralphie-bin-path [] (expand "~/.local/bin/ralphie"))
(defn awesome-config-org-path [] (expand "~/russmatney/dotfiles/config.org"))

(defn github-username [] "russmatney")

(defn monitor []
  (or (System/getenv "MONITOR")
      "HDMI-0"
      "eDP-1"))

(defn yodo-dir [] (expand "~/russmatney/yodo"))
(defn org-dir [] (expand "~/todo"))
(defn repos-file [] (str (org-dir) "/repos.org"))
(defn projects-file [] (str (org-dir) "/projects.org"))
(defn inbox-file [] (str (org-dir) "/inbox.org"))
(defn goals-file [] (str (org-dir) "/goals.org"))
(defn today-file [] (str (org-dir) "/today.org"))
(defn journal-file [] (str (org-dir) "/journal.org"))
(defn prompts-file [] (str (org-dir) "/prompts.org"))
(defn pomodoros-file [] (str (org-dir) "/pomos.org"))
(defn screenshots-file [] (str (org-dir) "/screenshots.org"))
(defn workspaces-file [] (str (org-dir) "/workspaces.org"))
(defn reads-file [] (str (org-dir) "/reads.org"))
(defn watches-file [] (str (org-dir) "/watches.org"))

(defn todo-paths []
  [(repos-file)
   (projects-file)
   (goals-file)
   (today-file)
   (inbox-file)])

(defn item-paths []
  [(repos-file)
   (inbox-file)
   (projects-file)
   (goals-file)
   (today-file)
   (journal-file)
   (prompts-file)
   (screenshots-file)])

(defn notes-dir [] (expand "~/Dropbox/notes"))
(defn blog-notes-dir []
  (expand "~/russmatney/blog-gatsby/content/posts/notes"))

(defn i3-dir [] (expand "~/.config/i3"))
