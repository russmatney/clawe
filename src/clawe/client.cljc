(ns clawe.client
  (:require
   [clojure.string :as string]
   [clojure.set :as set]
   [malli.transform :as mt]
   [malli.core :as m]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn is-bar-app? [client]
  (or
    (-> client :client/key #{"topbar"})
    (-> client :client/window-title #{"tauri-doctor-topbar"})))

(defn is-scratchpad? [client]
  (or
    (-> client :hide/type #{:hide/scratchpad})
    ;; TODO switch to a non-yabai specific flag here
    (-> client :yabai.window/scratchpad #{""} not)))

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

;; consider enforcing this schema against clawe.edn via clj-kondo
(def schema
  [:map
   ;; TODO perhaps not optional?
   [:client/id {:optional true} :string]

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
  #?(:clj
     (try
       (when (or app-name app-names)
         (some->> (concat [app-name] app-names)
                  (remove nil?)
                  (map string/lower-case)
                  (into #{})))
       (catch Exception e
         (println app-name app-names e)
         nil))
     :cljs
     (when (or app-name app-names)
       (some->> (concat [app-name] app-names)
                (remove nil?)
                (map string/lower-case)
                (into #{})))))

(defn match?
  "Returns true if the clients are a match.

  Uses `:client/app-name`, `:client/app-names`, `:client/window-title`

  Supports `:match/skip-title`, `:match/soft-title`, `:match/use-workspace-title`,
  `:match/ignore-names`.

  If `:match/ignore-names` matches the `:client/app-name` OR `:client/window-title`
  of either a or b, this function returns `false`.

  `:match/*` opts can be attached to clients (in fact, they are attached
  by `wm/merge-with-client-defs`), but they are only read from `opts`.
  This avoids order-matters trouble with this function. Instead, supply
  the client with the desired match opts as the first arg to the 3-arity
  version of this function.
  "
  ;; `:client/key` comparison might be cheap/easy
  ([a b] (match? nil a b))
  ([opts a b]
   (let [a-app-names (->app-names a)
         b-app-names (->app-names b)

         a-window-title (some-> a :client/window-title string/lower-case)
         b-window-title (some-> b :client/window-title string/lower-case)

         ignore-names (some-> opts :match/ignore-names set)]
     (and
       (not (or
              (seq (set/intersection ignore-names a-app-names))
              (seq (set/intersection ignore-names b-app-names))
              (->> ignore-names
                   (filter #(= % a-window-title))
                   seq)
              (->> ignore-names
                   (filter #(= % b-window-title))
                   seq)))

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
                (or
                  (string/includes? a-window-title b-window-title)
                  (string/includes? b-window-title a-window-title))

                :else
                ;; fallback to a direct comparison
                (= a-window-title b-window-title)))

         ;; support matching against the current workspace title, if provided
         (and (:current-workspace-title opts)
              (or
                (and
                  b-window-title
                  (:match/use-workspace-title a)
                  (let [a-title (:current-workspace-title opts)]
                    (if (:match/soft-title opts)
                      (or
                        (string/includes? a-title b-window-title)
                        (string/includes? b-window-title a-title))
                      (= a-title b-window-title))))
                (and
                  a-window-title
                  (:match/use-workspace-title b)
                  (let [b-title (:current-workspace-title opts)]
                    (if (:match/soft-title opts)
                      (or
                        (string/includes? b-title a-window-title)
                        (string/includes? a-window-title b-title))
                      (= b-title a-window-title)))))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; client->workspace-title
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn client->workspace-title [client]
  (or (:client/workspace-title client)
      (let [wt (:client/window-title client)]
        (cond
          (re-seq #" " wt)
          ;; term before first space
          (->> (string/split wt #" ") first)

          :else wt))))
