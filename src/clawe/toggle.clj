(ns clawe.toggle
  (:require
   [ralphie.notify :as notify]
   [clawe.doctor :as clawe.doctor]
   [clawe.wm :as wm]
   [clawe.client :as client]
   [clawe.workspace :as workspace]
   [clawe.config :as clawe.config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; open client
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn open-client [def]
  ;; how to integrate a quick --key cli into this?
  ;; support as a key? feels like a workaround, ought to read bb-cli
  #_{:org.babashka/cli {:alias {:key :client/key}}}
  (let [def (if (string? def) (clawe.config/client-def def) def)]
    (when-let [open-opts (-> def :client/open)]
      (cond
        (symbol? open-opts)
        (let [n (namespace open-opts)]
          (if-let [f (if n
                       (do
                         (require (symbol n))
                         (ns-resolve (symbol n) open-opts))
                       (resolve open-opts))]
            (f)
            (do
              (println "Could not resolve :client/open" open-opts)
              (notify/notify "Could not resolve :client/open"))))
        (map? open-opts)
        (if-let [cmd (-> open-opts :open/cmd)]
          ((resolve cmd) open-opts)
          (notify/notify ":client/open map form requires :open/cmd"))))))

(comment
  (->
    (clawe.config/client-def "journal")
    open-client))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; find-client
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO this might be the same as wm/fetch-client... presuming that is written
;; to support passed :clients or :prefetched-clients
(defn find-client
  "Returns a client matching the passed `client-def`.

  Matches via `client/match?`, passing the def twice to use any `:match/` opts.

  Supports prefetched `:prefetched-clients` to avoid the `wm/` call."
  ([client-def] (find-client nil client-def))
  ([opts client-def]
   (let [all-clients (:prefetched-clients opts (wm/active-clients opts))]
     (some->> all-clients
              (filter
                ;; pass def as opts
                (partial client/match? client-def client-def))
              first))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; in current workspace?
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn client-in-current-workspace?
  ([client] (client-in-current-workspace? nil client))
  ([opts client]
   (when client
     (let [wsps (:current-workspaces opts (wm/current-workspaces
                                            ;; pass :prefetched-clients through
                                            (assoc opts :include-clients true)))]
       (some->> wsps
                (map (fn [wsp] (workspace/find-matching-client wsp client)))
                first)))))

(comment
  (clawe.config/reload-config)

  (let [def (clawe.config/client-def "journal")]
    (-> def
        :client/open
        :open/cmd
        ((fn [f] ((resolve f) def)))))

  (->>
    (wm/active-clients)
    (map client/strip)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; toggle client def
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn determine-toggle-action [client-key]
  (println "deter w/ client-key" client-key)
  ;; TODO prefetch and pass opts into funcs
  (let [def (clawe.config/client-def client-key)]
    (if-not def
      [:no-def client-key]

      (let [client (find-client def)]
        (cond
          (not client) [:open-client def]

          (client-in-current-workspace? client)
          (if (:client/focused client)
            [:hide-client client]
            [:focus-client client])

          :else [:show-client client])))))

;; TODO rofi for choosing one of these events with any client in the config

(defn toggle
  {:org.babashka/cli
   {:alias {:key :client/key
            ;; TODO might include an override for :hide/type, :open/* options
            }}}
  [args]
  (notify/notify "[toggle]" (:client/key args "No --key"))
  (let [[action
         ;; TODO better var name than val
         ;; client-or-def ?
         ;; this is why reframe did maps for everything
         ;; gotta do keys when it's ambigous
         val] (determine-toggle-action (:client/key args))]
    (println "action:" action)
    (case action
      :no-def
      (do
        (println "WARN: [toggle] no def found for client-key" args)
        (notify/notify "[no def]" (:client/key args "No --key")))

      ;; emacs/tmux/generic opts/description
      ;; open better than create? some other naming? compare to show?
      :open-client
      (do
        (println "open" (client/strip val))
        (notify/notify "[:open-client]" (:client/key args "No --key"))
        (open-client val))

      :hide-client
      (do
        ;; move to an ensured workspace? close? minimize? hide?
        (println "hide" (client/strip val))
        (notify/notify "[:hide-client]")
        (wm/hide-client val))

      :focus-client
      (do
        (println "focus" (client/strip val))
        (notify/notify "[:focus-client]")
        ;; this is sometimes 'send-focus' other times 'jumbotron'
        (wm/focus-client {:float-and-center
                          ;; won't apply to emacs/term... fine for now
                          true} val))

      :show-client
      (do
        (println "show" (client/strip val))
        (notify/notify "[:show-client]")
        ;; TODO consider 'wm/show-client' or :show/ opts
        ;; or a more :hyde/jekyll or toggle->focus function
        (wm/move-client-to-workspace
          ;; TODO how to cache this extra lookup... for free?
          ;; wm could hold a cache? or just support a 'current'
          ;; opt for this function - the wm might have a shortcut
          ;; .... or just call it in a let and pass it all the way through
          val (wm/current-workspace))
        (wm/focus-client {:float-and-center
                          ;; won't apply to emacs/term... fine for now
                          true} val) )

      (println "No matching action for:" action))
    (clawe.doctor/update-topbar)))

(comment
  (toggle {:client/key "journal"}))
