(ns clawe.client
  (:require
   [babashka.process :as process]
   [clojure.string :as string]

   [ralphie.emacs :as emacs]
   [ralphie.notify :as notify]
   [ralphie.tmux :as tmux]
   [ralphie.zsh :as zsh]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; schema
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def schema
  [:map
   [:client/app-name :string]
   [:client/window-title :string]
   [:client/focused [:maybe :boolean]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; create client
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-client
  "Creates clients for a given workspace

  TODO refactor to remove or otherwise use a :create.client/exec api
  "
  [{:workspace/keys [exec directory initial-file readme] :as wsp}]
  (let [first-client (cond
                       exec         :create/exec
                       readme       :create/emacs
                       initial-file :create/emacs
                       :else        (do
                                      (notify/notify
                                        "Could not determine first client for wsp" wsp)
                                      :create/none))]

    (case first-client
      :create/emacs
      (emacs/open {:emacs.open/workspace (:workspace/title wsp)
                   :emacs.open/file
                   (let [f (or initial-file readme)]
                     (if (string/starts-with? f "/") f
                         (str directory "/" f)))})
      :create/exec
      (cond
        (and (map? exec) (:tmux.fire/cmd exec))
        (tmux/fire exec)

        (string? exec) (-> exec
                           (string/split #" ")
                           process/process
                           process/check)

        :else
        (notify/notify "Unhandled workspace/exec:" exec))

      :create/none
      ;; NOTE maybe detect a readme in directories as well
      ;; (notify/notify "New workspace has no default client."
      ;;                "Try setting :initial-file or :exec")
      )))

(comment
  (create-client
    {:workspace/initial-file (zsh/expand "~/russmatney/ralphie/readme.org")
     :workspace/title        "my-wsp"}))
