(ns clawe.workspaces.create
  (:require
   [ralphie.emacs :as emacs]
   [ralphie.notify :as notify]

   [babashka.process :refer [process check]]
   [clojure.string :as string]))

(defn create-client
  "Creates clients for a given workspace"
  [wsp]
  (let [exec         (some wsp [:workspace/exec])
        init-file    (some wsp [:workspace/initial-file])
        first-client (cond
                       exec      :create/exec
                       init-file :create/emacs
                       :else     (do
                                   (notify/notify
                                     "Could not determine first client for wsp" wsp)
                                   :create/none))]
    (println first-client init-file)
    (notify/notify first-client init-file)

    (case first-client
      :create/emacs (emacs/open wsp)
      :create/exec  (do
                      (notify/notify "Starting new client" exec)
                      (-> exec
                          (string/split #" ")
                          process
                          check)
                      (notify/notify "New client started" exec))
      :create/none
      ;; NOTE maybe detect a readme in directories as well
      (notify/notify "New workspace has no default client."
                     "Try setting :initial-file or :exec"))))

(comment
  (create-client
    {:workspace/initial-file "/home/russ/Dropbox/todo/projects.org"
     :workspace/title "my-wsp"}))
