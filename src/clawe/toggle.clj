(ns clawe.toggle
  (:require
   [ralphie.notify :as notify]
   [clawe.config :as clawe.config]
   [clawe.client :as client]
   [clawe.client.create :as client.create]
   [clawe.doctor :as clawe.doctor]
   [clawe.wm :as wm]
   [clawe.workspace :as workspace]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; find-client
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO this might be the same as wm/fetch-client... perhaps that should be written
;; to support passed :prefetched-clients
(defn find-client
  "Returns a client matching the passed `client-def`.

  The current workspace is fetched to support per-workspace client matching.

  Matches via `client/match?`, passing the def twice to use any `:match/` opts.

  Supports prefetched `:prefetched-clients` to avoid the `wm/` call."
  ([client-def] (find-client nil client-def))
  ([opts client-def]
   (let [client-def  (cond
                       (map? client-def)    client-def
                       (string? client-def) (clawe.config/client-def client-def))
         all-clients (or (:prefetched-clients opts) (wm/active-clients opts))
         current     (or (:current-workspace opts) (wm/current-workspace
                                                     {:prefetched-clients all-clients}))]
     (some->> all-clients
              (filter
                (partial client/match?
                         ;; pass def as opts
                         (assoc client-def :current-workspace-title (:workspace/title current))
                         client-def))
              first
              ;; merge the client-def here, to include match details
              ;; for use-workspace-title client matches
              ;; (could update wm/merge-client-defs to handle a context-workspace instead)
              (merge client-def)))))

(comment
  (clawe.config/reload-config)
  (find-client "messages")
  (clawe.config/client-def "messages")
  (->>
    (wm/active-clients)
    (filter
      (partial client/match?
               (clawe.config/client-def "messages")))

    #_(filter (comp #{"Messages"} :client/app-name))
    )

  (->
    (clawe.config/client-def "terminal")
    find-client
    client/strip))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; in current workspace?
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn client-in-current-workspace?
  ([client] (client-in-current-workspace? nil client))
  ([opts client]
   (when client
     (let [wsp (or (:current-workspace opts) (wm/current-workspace opts))]
       (some->> [wsp] ;; may reach for checking multiple wsps later on
                (map (fn [wsp] (workspace/find-matching-client wsp client)))
                first)))))

(comment
  (clawe.config/reload-config)

  (let [def (clawe.config/client-def "journal")]
    (-> def
        :client/create
        :create/cmd
        ((fn [f] ((resolve f) def)))))

  (->>
    (wm/active-clients)
    (map client/strip)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; determine toggle action
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn determine-toggle-action
  ([client-key] (determine-toggle-action client-key nil))
  ([client-key opts]
   (let [def (clawe.config/client-def client-key)]
     (if-not def
       [:no-def client-key]

       (let [client (find-client opts def)]
         (cond
           (not client) [:create-client def]

           (client-in-current-workspace? opts client)
           (if (:client/focused client)
             [:hide-client client]
             [:focus-client client])

           :else [:show-client client]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; execute-toggle-action
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn execute-toggle-action [tog-action opts]
  (let [[action client-or-def] tog-action]
    (println "Executing" action)
    (case action
      :no-def
      (do
        (println "WARN: [toggle] no def found for :client/key in" opts)
        (notify/notify "[no def]" (:client/key opts "No --key")))

      :create-client
      (do
        (println "create" (client/strip client-or-def))
        (notify/notify "[:create-client]" (:client/key opts "No --key"))
        (client.create/create-client client-or-def opts))

      :hide-client
      (do
        (println "hide" (client/strip client-or-def))
        (notify/notify "[:hide-client]")
        (wm/hide-client client-or-def))

      :focus-client
      (do
        (println "focus" (client/strip client-or-def))
        (notify/notify "[:focus-client]")
        ;; this is sometimes 'send-focus', other times 'jumbotron'
        (wm/focus-client {:float-and-center (:focus/float-and-center client-or-def true)}
                         client-or-def))

      :show-client
      (do
        (println "show" (client/strip client-or-def))
        (notify/notify "[:show-client]")
        (wm/show-client opts client-or-def))

      (println "No matching action for:" action))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; toggle
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn toggle
  {:org.babashka/cli
   ;; could support overrides for :hide/*, :create/*, :focus/* options
   {:alias {:key :client/key}}}
  [args]
  (notify/notify "[toggle]" (:client/key args "No --key"))
  (let [all-clients       (wm/active-clients)
        current-workspace (wm/current-workspace
                            {:prefetched-clients all-clients})
        opts              {:prefetched-clients all-clients
                           :current-workspace  current-workspace}]
    (-> args
        :client/key
        (determine-toggle-action opts)
        (execute-toggle-action (merge args opts))))

  (clawe.doctor/update-topbar))

(comment
  (toggle {:client/key "journal"}))
