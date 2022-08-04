(ns clawe.client
  (:require
   [babashka.process :as process]
   [clojure.string :as string]

   [ralphie.emacs :as emacs]
   [ralphie.notify :as notify]
   [ralphie.tmux :as tmux]
   [ralphie.zsh :as zsh]
   [clojure.set :as set]
   [malli.transform :as mt]
   [malli.core :as m]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; schema
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def schema
  [:map
   [:client/app-name :string]
   [:client/window-title :string]
   [:client/focused [:maybe :boolean]]

   ;; unique for a clawe.edn config: supports spawn-client, find-client, apply rules workflows
   [:client/key {:optional true} :string]
   [:client/app-names [:sequential :string]]])

(defn strip [c]
  (m/decode schema c (mt/strip-extra-keys-transformer)) )

(comment
  (m/decode
    schema
    {:client/window-title "hi"
     :gibber              :jabber}
    (mt/strip-extra-keys-transformer))

  (strip
    {:client/window-title "hi"
     :gibber              :jabber}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; match
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->app-names [{:client/keys [app-name app-names]}]
  (->> (concat [app-name] app-names)
       (remove nil?)
       (into #{})))

(defn match?
  "Returns true if the clients are a match.

  Uses `:client/app-name`, `:client/app-names`, `:client/window-title`"
  ([a b] (match? nil a b))
  ([opts a b]
   (let [a-app-names (->app-names a)
         b-app-names (->app-names b)

         a-window-title (-> a :client/window-title)
         b-window-title (-> b :client/window-title)]
     (and
       (and (seq a-app-names)
            (seq b-app-names)
            (set/intersection a-app-names b-app-names))

       (or
         (:match/skip-title opts)
         (not a-window-title)
         (not b-window-title)
         (and a-window-title
              b-window-title
              (if (:match/soft-title opts)
                (or
                  (string/includes? a-window-title b-window-title)
                  (string/includes? b-window-title a-window-title))
                (= a-window-title b-window-title))))

       ;; TODO `:client/key` comparison
       ))))

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
