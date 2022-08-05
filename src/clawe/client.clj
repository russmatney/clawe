(ns clawe.client
  (:require
   [clojure.string :as string]
   [clojure.set :as set]
   [malli.transform :as mt]
   [malli.core :as m]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; schema
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def client-create-schema
  [:or
   :symbol
   :string
   [:map [:create/cmd :symbol
          ]]])

(comment
  (m/validate
    client-create-schema
    println)

  (m/validate
    client-create-schema
    {:create/cmd println}))

;; TODO enforce schema against clawe.edn via clj-kondo
(def schema
  [:map
   [:client/app-name {:optional true} :string]
   [:client/window-title {:optional true} :string]
   [:client/focused {:optional true} [:maybe :boolean]]

   ;; unique for a clawe.edn config: supports spawn-client, find-client, apply rules workflows
   [:client/key {:optional true} :string]
   [:client/app-names {:optional true} [:sequential :string]]

   ;; string used to assign a workspace title when :hide/scratchpadding a client
   [:client/workspace-title {:optional true} :string]

   [:client/create {:optional true} client-create-schema]

   ;; if true, the title won't be used when matching this client against others
   [:match/skip-title {:optional true} :boolean]
   ;; if true, this client will be more forgiving when matching against others
   [:match/soft-title {:optional true} :boolean]

   ;; if true, client/match will use the current workspace title for the client window title comparison
   ;; this supports toggling an app that belongs to the workspace (terminal, emacs)
   [:match/use-workspace-title {:optional true} :boolean]

   ;; determines the hide behavior when toggling
   [:hide/type {:optional true} :keyword]

   ;; whether to float-and-center when focusing. defaults to true!
   [:focus/float-and-center {:optional true :default true} :boolean]])

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

       ;; TODO `:client/key` comparison might be cheap/easy
       ))))
