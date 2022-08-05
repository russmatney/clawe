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

  Matches via `client/match?`, passing the def twice to use any `:match/` opts.

  Supports prefetched `:prefetched-clients` to avoid the `wm/` call."
  ([client-def] (find-client nil client-def))
  ([opts client-def]
   (let [all-clients (:prefetched-clients opts (wm/active-clients opts))
         current (:current-workspace opts (wm/current-workspace))]
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
        :client/create
        :create/cmd
        ((fn [f] ((resolve f) def)))))

  (->>
    (wm/active-clients)
    (map client/strip)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; determine toggle action
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn determine-toggle-action [client-key]
  ;; TODO prefetch and pass opts into funcs
  (let [def (clawe.config/client-def client-key)]
    (if-not def
      [:no-def client-key]

      (let [client (find-client def)]
        (cond
          (not client) [:create-client def]

          (client-in-current-workspace? client)
          (if (:client/focused client)
            [:hide-client client]
            [:focus-client client])

          :else [:show-client client])))))

;; TODO rofi for choosing+executing one of these events with any client in the config

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; execute-toggle-action
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn execute-toggle-action [tog-action args]
  (let [[action client-or-def] tog-action]
    (case action
      :no-def
      (do
        (println "WARN: [toggle] no def found for :client/key in" args)
        (notify/notify "[no def]" (:client/key args "No --key")))

      :create-client
      (do
        (println "create" (client/strip client-or-def))
        (notify/notify "[:create-client]" (:client/key args "No --key"))
        (client.create/create-client client-or-def))

      :hide-client
      (do
        ;; move to an ensured workspace? close? minimize? hide?
        (println "hide" (client/strip client-or-def))
        (notify/notify "[:hide-client]")
        (wm/hide-client client-or-def))

      :focus-client
      (do
        (println "focus" (client/strip client-or-def))
        (notify/notify "[:focus-client]")
        ;; this is sometimes 'send-focus' other times 'jumbotron'
        (wm/focus-client {:float-and-center (:focus/float-and-center client-or-def true)}
                         client-or-def))

      :show-client ;; i.e. move-to-current-wsp + focus
      (do
        (println "show" (client/strip client-or-def))
        (notify/notify "[:show-client]")
        ;; TODO consider 'wm/show-client' or :show/ opts
        ;; or a more :hyde/jekyll or toggle->focus function
        (wm/move-client-to-workspace
          ;; TODO how to cache this extra lookup... for free?
          ;; wm could hold a cache? or just support a 'current'
          ;; opt for this function - the wm might have a shortcut
          ;; .... or just call it in a let and pass it all the way through
          client-or-def (wm/current-workspace))
        (wm/focus-client {:float-and-center (:focus/float-and-center client-or-def true)}
                         client-or-def))

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
  (-> args
      :client/key
      determine-toggle-action
      (execute-toggle-action args))
  (clawe.doctor/update-topbar))

(comment
  (toggle {:client/key "journal"}))
