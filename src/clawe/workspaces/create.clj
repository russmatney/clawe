(ns clawe.workspaces.create
  (:require
   [ralphie.emacs :as emacs]
   [ralphie.notify :as notify]

   [babashka.process :refer [process check]]
   [clojure.string :as string]))

(defn create-client
  "Creates clients for a given workspace

  TODO refactor to remove or otherwise use a :create.client/exec api
  "
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

    (case first-client
      :create/emacs (emacs/open {:emacs.open/workspace (:workspace/title wsp)
                                 :emacs.open/file      init-file})
      :create/exec  (-> exec
                        (string/split #" ")
                        process
                        check)

      :create/none
      ;; NOTE maybe detect a readme in directories as well
      ;; (notify/notify "New workspace has no default client."
      ;;                "Try setting :initial-file or :exec")
      )))

(comment
  (create-client
    {:workspace/initial-file "/home/russ/russmatney/ralphie/readme.org"
     :workspace/title "my-wsp"}))
