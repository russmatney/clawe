(ns clawe.client
  (:require
   [babashka.process :as process]
   [clojure.string :as string]

   [ralphie.emacs :as emacs]
   [ralphie.awesome :as awm]
   [ralphie.notify :as notify]
   [ralphie.tmux :as tmux]
   [ralphie.zsh :as zsh]))


(defn ignore-client?
  "These clients should not be buried or restored...
  (or interacted with at all, really, but that's going
  to take some work...)"
  [{:keys [name]}]
  (or
    (string/includes? name "meet.google.com")
    (string/includes? name "tauri/doctor-topbar")
    (string/includes? name "tauri/twitch-chat")))

(defn bury-floating-clients
  "Usually combined with other awm/fnl commands for performance"
  []
  ^{:quiet? false}
  (awm/fnl
    (each [c (awful.client.iterate (fn [c] (. c :ontop)))]
          ;; TODO filter things to bury/not-bury?
          (tset c :ontop false)
          (tset c :floating false))))

(defn focus-client
  "
  Focuses the passed client.
  Expects client as a map with `:window` or `:client/window`.

  Options:
  - :bury-all? - default: true.
    Sets all other clients ontop and floating to false
  - :float? - default: true.
    Set this client ontop and floating to true
  - :center? - default: true.
    Centers this client with awful
  "
  ([client] (focus-client nil client))
  ([opts client]
   (let [window    ((some-fn :window :client/window :awesome.client/window) client)
         bury-all? (:bury-all? opts true)
         float?    (:float? opts true)
         center?   (:center? opts true)]
     (if-not window
       (notify/notify "Set Focused called with no client :window" {:client client
                                                                   :opts   opts})
       (do
         ^{:quiet? false}
         (awm/fnl
           (when ~bury-all?
             (each [c (awful.client.iterate (fn [c] (. c :ontop)))]
                   ;; TODO filter things to bury/not-bury?
                   (tset c :ontop false)
                   (tset c :floating false)))

           (each [c (awful.client.iterate (fn [c] (= (. c :window) ~window)))]

                 (when ~float?
                   (tset c :ontop true)
                   (tset c :floating true))

                 (when ~center?
                   (awful.placement.centered c))

                 ;; TODO set minimum height/width?
                 (tset _G.client :focus c))))))))

(comment
  (def c
    (->>
      (awm/fetch-tags)
      (filter (comp #{"clawe"} :awesome.tag/name))
      first
      :awesome.tag/clients
      first
      ))

  (focus-client {:center? false} c)

  )


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
