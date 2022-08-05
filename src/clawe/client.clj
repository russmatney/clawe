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

;; TODO enforce schema against clawe.edn via clj-kondo
(def schema
  [:map
   [:client/app-name :string]
   [:client/window-title :string]
   [:client/focused [:maybe :boolean]]

   ;; unique for a clawe.edn config: supports spawn-client, find-client, apply rules workflows
   [:client/key {:optional true} :string]
   [:client/app-names {:optional true} [:sequential :string]]

   ;; string used to assign a workspace title when :hide/scratchpadding a client
   [:client/workspace-title {:optional true} :string]

   ;; if true, the title won't be used when matching this client against others
   [:match/skip-title {:optional true} :boolean]
   ;; if true, this client will be more forgiving when matching against others
   [:match/soft-title {:optional true} :boolean]

   ;; if true, client/match will use the current workspace title for the client window title comparison
   ;; this supports toggling an app that belongs to the workspace (terminal, emacs)
   [:match/use-workspace-title {:optional true} :boolean]

   ;; determines the hide behavior when toggling
   [:hide/type {:optional true} :keyword]])

(defn strip [c]
  (m/decode schema c (mt/strip-extra-keys-transformer)))

(comment
  (m/validate
    schema
    {:client/window-title "hi"
     :client/app-name     "blah"
     :client/focused      nil
     :gibber              :jabber})

  (strip
    {:client/window-title "hi"
     :gibber              :jabber
     :hide/type           :hyde/close}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; match
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->app-names [{:client/keys [app-name app-names]}]
  (->> (concat [app-name] app-names)
       (remove nil?)
       (map string/lower-case)
       (into #{})))

(defn match?
  "Returns true if the clients are a match.

  Uses `:client/app-name`, `:client/app-names`, `:client/window-title`

  Supports `:match/skip-title` and `:match/soft-title`

  `:match/*` opts can be attached to clients (in fact, they are attached
  by `wm/merge-with-client-defs`), but they are only read from `opts`.
  This avoids order-matters trouble with this function. Instead, supply
  the client with the desired match opts as the first arg to the 3-arity
  version of this function.
  "
  ;; TODO support :match/use-workspace-title and :current-workspace-title as a passed opt
  ([a b] (match? nil a b))
  ([opts a b]
   (let [a-app-names (->app-names a)
         b-app-names (->app-names b)

         a-window-title (-> a :client/window-title)
         b-window-title (-> b :client/window-title)]
     (and
       (and (seq a-app-names)
            (seq b-app-names)
            (seq (set/intersection a-app-names b-app-names)))

       (or
         ;; skip title check completely
         (:match/skip-title opts)

         (and a-window-title
              b-window-title
              (cond

                ;; support optional soft title match
                (:match/soft-title opts)
                (when (or
                        (string/includes? a-window-title b-window-title)
                        (string/includes? b-window-title a-window-title))
                  (println "successful soft-title match" a-window-title b-window-title)
                  true)

                :else
                ;; fallback to a direct comparison
                (= a-window-title b-window-title)))

         ;; support matching against the current workspace title, if provided
         (and (:current-workspace-title opts)
              (or
                (and (:match/use-workspace-title a)
                     (= (:current-workspace-title opts)
                        b-window-title))
                (and (:match/use-workspace-title b)
                     (= (:current-workspace-title opts)
                        a-window-title)))))

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
