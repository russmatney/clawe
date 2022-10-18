(ns ralphie.awesome
  "This namespace provides useful interactions with AwesomeWM.

  It depends heavily on a working `awesome-client` command for firing commands
  or gathering data from awesome. Alot of this is made simple (or more complex?)
  by using fennel as the intermediary, because fennel datastructures can be
  directly consumed by clojure's `load-string` function.

  Most of the functions build on the `awm-fnl` or `awm-lua` functions
  which expect fennel or lua as a string (or a quoted list to be passed to `str`).
  Both pass the expression to `awesome-client` via babashka.process and return
  the output as a clojure data structure (i.e. a map or list of values), to the
  extent that it can be parsed.

  The `fnl` macro is a helper for writing fennel directly, and supports unquoting
  for interpolating a value.

  Eg.

  (ralphie.awesome/fnl (-> (client.get) (lume.map (fn [t] {:name t.name}))))
  => [{:name \"Mozilla Firefox\"} {:name \"ralphie\"} {:name \"Junior Boys - Dull To Pause\"} {:name \"tauri/doctor-topbar\"}]

  (ralphie.awesome/awm-cli
    (str
      \"return view(lume.map(client.get(), \"
      \"function (t) return {name= t.name} end))\"))
  => [{:name \"Mozilla Firefox\"} {:name \"ralphie\"} {:name \"Junior Boys - Dull To Pause\"} {:name \"tauri/doctor-topbar\"}]

  These functions include a 'preamble' that requires a handful of convenient globals,
  such as `lume` (a lua functional lib), `client`, `awful`, `view` (aka `fennelview`,
  which prettyprints to clojure-`load-string`-readable structures). See `lua-preamble`.
  "
  (:require
   [babashka.process :as process]
   [clojure.string :as string]
   [malli.provider :as mp]
   [defthing.defcom :refer [defcom] :as defcom]
   [ralphie.awesome.fnl :as awm.fnl :refer [fnl]]
   [ralphie.notify :as notify]
   [ralphie.sh :as sh]))

;; This is hard-coded to the awesome keybinding writer! be careful!
(def awm-fnl awm.fnl/awm-fnl) ;; awmfnlawmfnlawmfnl
(def awm-cli awm.fnl/awm-cli) ;; awmfnlawmfnlawmfnl

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; malli schema
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def client-schema
  [:map
   [:awesome.client {:optional true} [:map]]
   [:awesome.client/window :int]])

(def tag-schema
  [:map
   [:awesome/tag {:optional true} [:map]]
   [:awesome.tag/index :int]
   [:awesome.tag/name :string]
   [:awesome.tag/layout :string]
   [:awesome.tag/selected [:maybe :boolean]]
   [:awesome.tag/urgent [:maybe :boolean]]
   [:awesome.tag/empty :boolean]
   [:awesome.tag/clients {:optional true} [:sequential client-schema]]])

(def screen-schema
  [:map
   [:awesome/screen [:map]]
   [:awesome.screen/tags {:optional true} [:sequential tag-schema]]
   [:awesome.screen/geometry [:map-of keyword? int?]]])


(declare ->namespaced-tag)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; screen fetcher

(defn ->namespaced-screen
  "Recieves a raw-awm-screen `screen`, and moves all data to a namespaced keyword.

  `:tags` are passed to `->namespaced-tag`, `:clients` to `->namespaced-client`."
  [screen]
  (let [tags (->> screen :tags (map ->namespaced-tag))]
    {:awesome/screen          (dissoc screen :tags :geometry)
     :awesome.screen/tags     tags
     :awesome.screen/geometry (:geometry screen)}))

(defn screen
  ([] (screen nil))
  ([_opts]
   (-> (fnl
         (view
           {:geometry (. s :geometry)
            :tags     (lume.map
                        s.tags
                        (fn [t]
                          ;; could fetch more tag details here...
                          {:name   (. t :name)
                           :index  (. t :index)
                           :layout (-?> t (. :layout) (. :name))}))}))
       ->namespaced-screen)))

(comment
  (screen)
  (mp/provide (list (screen))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; tag fetchers

(defn ->namespaced-client
  "Recieves a raw-awm `client`, and moves data to namespaced keywords."
  [client]
  {:awesome/client          (dissoc client :name :urgent :instance :type :pid :class :ontop :master :window :focused
                                    :tag-names :first-tag :geometry)
   :awesome.client/name     (:name client)
   :awesome.client/class    (:class client)
   :awesome.client/instance (:instance client)
   :awesome.client/window   (:window client)
   :awesome.client/pid      (:pid client)

   :awesome.client/tag-names (:tag-names client)
   :awesome.client/tag       (:first-tag client)
   :awesome.client/type      (:type client)

   :awesome.client/focused  (:focused client)
   :awesome.client/urgent   (:urgent client)
   :awesome.client/ontop    (:ontop client)
   :awesome.client/master   (:master client)
   :awesome.screen/geometry (:geometry client)})


;; TODO replace this with a malli docoder
(defn ->namespaced-tag
  "Recieves a raw-awm-tag `tag`, and moves all data to a namespaced keyword.

  If the tag has `:clients`, they are passed to `->namespaced-client`.
  "
  [tag]
  (let [clients (->> tag :clients (map ->namespaced-client))
        empty   (zero? (count clients))]
    {:awesome/tag         (dissoc tag :clients :name :index :selected :urgent :layout)
     :awesome.tag/index   (:index tag)
     :awesome.tag/name    (:name tag)
     :awesome.tag/clients clients

     :awesome.tag/layout (:layout tag)

     :awesome.tag/selected (:selected tag)
     :awesome.tag/urgent   (:urgent tag)
     :awesome.tag/empty    empty}))


(defn fetch-tags
  "Returns all awm tags as clojure maps, namespaced with :awesome.tag and :awesome.client keys."
  ([] (fetch-tags nil))
  ([opts]
   ;; TODO support `:tag-names #{}` as a list
   (try
     (->>
       (fnl
         (local focused-window (if client.focus client.focus.window nil))
         (local m-client (awful.client.getmaster))
         (local m-window (if m-client m-client.window nil))

         (->
           ~(if (:only-current opts)
              '[s.selected_tag]
              '(root.tags))
           (lume.map
             (fn [t]
               {:name     t.name
                :selected t.selected
                :index    t.index
                :urgent   t.urgent
                :layout   (-?> t (. :layout) (. :name))
                :clients
                (->
                  ~(if (:include-clients opts) '(t:clients) '[])
                  (lume.map
                    (fn [c]
                      {:name      (. c :name)
                       :geometry  (c:geometry)
                       :ontop     c.ontop
                       :window    c.window
                       :urgent    c.urgent
                       :type      c.type
                       :class     c.class
                       :instance  c.instance
                       :pid       c.pid
                       :role      c.role
                       :tag-names (-> (c:tags) (lume.map (fn [x] (. x :name))))
                       :first-tag c.first_tag.name
                       :master    (= m-window c.window)
                       :focused   (= focused-window c.window)})))}))
           view))
       (map ->namespaced-tag))
     (catch Exception _e
       (println "awm/fetch-tags error")
       nil))))

(comment
  (some->> (fetch-tags) first :awesome.tag/clients)

  (some->> (fetch-tags {:include-clients true}) first :awesome.tag/clients first)
  (some->> (fetch-tags {:only-current true}) first :awesome.tag/clients)
  (some->> (fetch-tags {:only-current    true
                        :include-clients true}) first :awesome.tag/clients))


(defn tag-for-name
  "Returns a namespaced tag matching the passed name.
  Supports a passed `fetch-tags`, which is helpful when running for multiple tags
  at once - in that case, fetch-tags should be calculated and passed to each call.

  May benefit from a smarter base case, if serialization/fetch-tags is expensive.
  "
  ([name] (tag-for-name name (fetch-tags)))
  ([name fetch-tags]
   (some->>
     fetch-tags
     (filter (comp #{name} :awesome.tag/name))
     first)))

(comment
  (tag-for-name "clawe"))


(defn tag-exists? [tag-name]
  (fnl (-> (root.tags)
           (lume.filter (fn [t] (= (. t :name) ~tag-name)))
           lume.count
           (> 0))))

(comment
  (tag-exists? "clawe")
  (tag-exists? "joker"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; client fetchers

(defn all-clients
  "Returns all currently running clients."
  []
  (->>
    (fnl
      (local focused-window (if client.focus client.focus.window nil))
      (local m-client (awful.client.getmaster))
      (local m-window (if m-client m-client.window nil))
      (-> (client.get)
          (lume.map (fn [c]
                      {:class     c.class
                       :first-tag c.first_tag.name
                       :focused   (= focused-window c.window)
                       :geometry  (c:geometry)
                       :instance  c.instance
                       :master    (= m-window c.window)
                       :name      (. c :name)
                       :ontop     c.ontop
                       :pid       c.pid
                       :role      c.role
                       :tag-names (lume.map (c:tags) (fn [t] {:name (. t :name)}))
                       :type      c.type
                       :urgent    c.urgent
                       :window    c.window}))
          view))
    (map ->namespaced-client)))

(defn clients-for-class [nm]
  (some->>
    (all-clients)
    (filter (comp
              #(string/includes? % (string/lower-case nm))
              string/lower-case
              :awesome.client/class))))

(defn client-for-class [nm]
  (some->> nm clients-for-class first))

(defn clients-for-name [nm]
  (some->>
    (all-clients)
    (filter (comp
              #(string/includes? % (string/lower-case nm))
              string/lower-case
              :awesome.client/name))))

(defn client-for-name [nm]
  (some->> nm clients-for-name first))

(comment (client-for-name "clawe"))

(defn client-for-id [window-id]
  (some->>
    (all-clients)
    (filter (comp #{window-id} :awesome.client/window))
    first))

(comment (client-for-id (read-string "58720263")))

(defn client-on-tag? [client tag-name]
  ;; TODO benchmark - is this cheaper via awm?
  ;; in most cases we probably already have the data via fetch-tags
  ;; fetch-tags could build a quick lookup index for this
  ;; or a client could be used to use an awm func for this (like `(c:tags)`)
  ;; - that's the benchmark to compare: awm via clojure overhead vs clojure data manip

  ;; the reality might be that we delete this func in a scratchpad-impl refactor
  (let [tag-names (->> client
                       :awesome.client/window
                       client-for-id
                       :awesome.client/tags
                       (map :awesome.tag/name)
                       (into #{}))]
    (boolean (tag-names tag-name))))

(comment
  (->>
    ;; (client-for-class "spotify")
    (client-for-id 50331655)
    :awesome.client/window
    client-for-id
    :awesome.client/tags
    (map :awesome.tag/name)
    (into #{})
    )
  (client-for-class "spotify")
  (-> (client-for-id 50331655)
      (client-on-tag? "spotify")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; common awm tag functions

(defn ensure-tag
  "Creates a tag with the passed name if it does not exist."
  [tag-name]
  (fnl
    (if (awful.tag.find_by_name (awful.screen.focused) ~tag-name)
      nil
      (awful.tag.add ~tag-name {:layout awful.layout.suit.tile}))))

(defn focus-tag! [tag-name]
  (fnl
    #_{:clj-kondo/ignore [:unused-binding]}
    (let [tag (awful.tag.find_by_name nil ~tag-name)]
      (tag:view_only))))

(defn toggle-tag [tag-name]
  (fnl (awful.tag.viewtoggle (awful.tag.find_by_name s ~tag-name))))

(comment
  (toggle-tag "clawe"))

(defn delete-tag! [tag-name]
  (fnl
    #_{:clj-kondo/ignore [:unused-binding]}
    (let [tag (awful.tag.find_by_name nil ~tag-name)]
      (tag:delete))))

(comment
  (delete-tag! "slack")
  (delete-tag! "dotfiles")
  (focus-tag! "slack"))

(defn delete-tag-by-index!
  "Deletes a tag at the passed index."
  [index]
  (fnl
    #_{:clj-kondo/ignore [:unused-binding]}
    (let [screen (awful.screen.focused)
          tags   screen.tags
          tag    (. tags ~index)]
      (tag:delete))))

(defn delete-current-tag! []
  (fnl (s.selected_tag:delete)))

(defcom awesome-delete-current-tag
  "Deletes the current focused tag."
  delete-current-tag!)

#_{:clj-kondo/ignore [:unused-binding]}
(defn swap-tags-by-index [idx-1 idx-2]
  (fnl
    #_{:clj-kondo/ignore [:unused-binding]}
    (let [screen (awful.screen.focused)
          tags   screen.tags
          tag    (. tags ~idx-1)
          tag2   (. tags ~idx-2)]
      (tag:swap tag2))))

(defn drag-workspace [up-or-down]
  (let [increment (case up-or-down
                    :dir/up   1
                    :dir/down -1)]
    (fnl
      #_{:clj-kondo/ignore [:unused-binding]}
      (let [screen        (awful.screen.focused)
            tags          screen.tags
            current-index s.selected_tag.index
            new-idx       (+ current-index ~increment)
            new-tag       (. tags new-idx)]
        (when new-tag
          (s.selected_tag:swap new-tag))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; common awm client functions

(defcom set-above-and-ontop
  (do
    (notify/notify "Setting above and ontop")
    (fnl
      (tset _G.client.focus :ontop true)
      (tset _G.client.focus :above true))))

(defn bury-all-clients
  "Buries :ontop clients."
  []
  ^{:quiet? false}
  (fnl
    (each [c (awful.client.iterate (fn [c] (. c :ontop)))]
          ;; TODO support filter
          (tset c :ontop false)
          (tset c :above false)
          (tset c :floating false))))

(defn bury-client
  "Buries :ontop clients."
  [window]
  ^{:quiet? false}
  (fnl
    (each [c (awful.client.iterate (fn [c] (= window (. c :window))))]
          ;; TODO support filter
          (tset c :ontop false)
          (tset c :above false)
          (tset c :floating false))))

(defn focus-client
  "
  Focuses the client with the passed window-id.

  Options:
  - :bury-all? - default: true.
    Sets all other clients ontop and floating to false
  - :float? - default: true.
    Set this client ontop and floating to true
  - :center? - default: true.
    Centers this client with awful
  "
  ([window-id] (focus-client nil window-id))
  ([opts window-id]
   (let [;; TODO consider bury-all alternatives, and pip/twitch-chat use-cases
         bury-all? (:bury-all? opts false)
         float?    (:float? opts true)
         center?   (:center? opts true)]
     (when window-id
       (fnl
         (when ~bury-all?
           (each [c (awful.client.iterate (fn [c]
                                            (. c :ontop)))]
                 ;; TODO filter things to bury/not-bury?
                 (tset c :floating false)))

         (each [c (awful.client.iterate (fn [c] (not (= (. c :ontop) ~window-id))))]
               (tset c :ontop false)
               (tset c :above false))

         (each [c (awful.client.iterate (fn [c] (= (. c :window) ~window-id)))]

               (when ~float?
                 (tset c :ontop true)
                 (tset c :above true)
                 (tset c :floating true))

               (when ~center?
                 (awful.placement.centered c))

               ;; TODO set minimum height/width?
               ;; default dimensions?
               (tset _G.client :focus c)))))))

(comment
  (def c
    (->>
      (fetch-tags)
      (filter (comp #{"clawe"} :awesome.tag/name))
      first
      :awesome.tag/clients
      first
      ))

  (focus-client {:center? false} (:awesome.client/window c)))

(defn move-client-to-tag
  "TODO create tag if it doesn't exist?"
  [window-id tag-name]
  (fnl
    (let [t (awful.tag.find_by_name nil ~tag-name)]
      (if t
        (each [c (awful.client.iterate (fn [c] (= (. c :window) ~window-id)))]
              (c:tags [t]))
        nil))))

(comment
  (awm.fnl/awm-fnl '(awful.tag.find_by_name nil "clawe"))
  (def -c (client-for-name "journal"))
  (move-client-to-tag (:awesome.client/window -c) "clawe"))

(defn close-client
  "Closes the passed client.
  Expects client as a map with `:window` or `:client/window`."
  [client]
  (let [window ((some-fn :window :client/window :awesome.client/window) client)]
    (println "close-client with window" window)
    (if-not window
      (notify/notify "Close client called with no client :window"
                     {:client client})

      (do
        (println "closing window" window)
        (fnl
          (each [c (awful.client.iterate (fn [c] (= (. c :window) ~window)))]
                (c:kill)))))))

(defcom set-layout
  "Sets the awesome layout"
  (fnl (awful.layout.set awful.layout.suit.tile)))

;; The rest of this file should probably live elsewhere
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; AwesomeWM Config management (doctor?)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fennel-check [abs-path]
  (let [dir (sh/expand "~/.config/awesome")
        res (->
              (process/process ["fennel" "--compile" abs-path] {:dir dir})
              (process/process ["luacheck" "-"] {:dir dir})
              ;; :out
              ;; slurp
              )
        res @res]
    (when-not (zero? (:exit res))
      (throw (Exception. (str "Fennel-check failed for path: " abs-path))))))

(comment
  (fennel-check (sh/expand "~/.config/awesome/run-init.fnl")))

(defn fennel-compile [{:keys [path]}]
  (-> ^{:out :string}
      (process/$ fennel --compile ~path)
      process/check))

(defn expand-files [str]
  (-> (sh/expand str)
      (string/split #" ")))

(defn ->compiler-error
  "Returns nil if the passed absolute path fails any checks."
  [abs-path]
  (try
    (cond
      (re-seq #".fnl$" abs-path)
      (fennel-compile {:path abs-path})
      ;; TODO add luacheck to awm runtime's path
      ;; (do
      ;;   (fennel-compile {:path abs-path})
      ;;   (fennel-check abs-path))
      )

    ;; TODO handle regular luacheck for .lua files

    nil
    (catch Exception e e)))

(defn check-for-errors
  "Checks for syntax errors across your awesome config.
  Intended to prevent restarts that would otherwise crash.

  TODO maybe we just try to load the config here via `lua` or `fennel`
  - possibly it could be impled to not re-run in run-init
  (if it's already alive)"
  []
  (let [notif-proc "awm-error-check"]
    (notify/notify "Checking AWM Config" "Syntax and Other BS"
                   {:replaces-process notif-proc})
    (let [config-files (concat
                         (expand-files "~/.config/awesome/*")
                         (expand-files "~/.config/awesome/**/*"))
          ct           (count config-files)
          errant-files (->> config-files
                            (map #(-> {:error (->compiler-error %)
                                       :file  %}))
                            (filter :error))]
      (if (seq errant-files)
        (->> errant-files
             (map (fn [{:keys [file error]}]
                    (notify/notify "Found issue:" error
                                   {:replaces-process notif-proc})
                    (println (str file "\n" (str error) "\n\n")))))

        (do
          (notify/notify "Clean Awesome config!" (str "Checked " ct " files")
                         {:replaces-process notif-proc})
          "No Errors.")))))

(comment (check-for-errors))

(defcom awesome-doctor check-for-errors)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Awesome-$ (shell command)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn shell
  "Intended to allow for a cheap shelling out from awesome.
  Was quickly replaced by sxhkd usage.
  A performance comparsion of the two might be interesting.

  One detail - awm key-repeats much faster than sxhkd, which is an issue -
  i've spammed awesomewm to death multiple times by holding keybindings...
  "
  ;; TODO async or not? maybe `shell!` is async?
  [arg] (fnl (awful.spawn.easy_async ~arg (fn []))))

(comment
  (let [arg "notify-send hi"]
    ^{:quiet? false}
    (fnl (awful.spawn.easy_async ~arg)))
  (shell "notify-send hi")
  (shell "pactl set-sink-volume @DEFAULT_SINK@ +5%")
  (shell "pactl set-sink-volume @DEFAULT_SINK@ -5%"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Rofi
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn rofi-kill-opts []
  (->>
    (fetch-tags)
    (map (fn [tag]
           (let [tag-name (:awesome.tag/name tag)]
             (concat
               [{:rofi/label     (str "Kill awesome tag: " tag-name)
                 ;; TODO tags with clients block deletion - support deleting a tag's clients
                 :rofi/on-select (fn [_] (delete-tag! tag-name))}]
               (->> (:awesome.tag/clients tag)
                    (map (fn [client]
                           (let [client-name (str (:awesome.client/name client)
                                                  " - "
                                                  (:awesome.client/class client)
                                                  " - "
                                                  (:awesome.client/instance client))]
                             {:rofi/label     (str "Kill client: " client-name)
                              :rofi/on-select (fn [_] (close-client client))})))
                    (into []))))))
    flatten))

;; TODO ignore the clj-kondo public-fn error properly
(comment (rofi-kill-opts))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sandbox
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;; calling global functions impled in awesome/run-init.fnl
  (fnl (update-topbar))
  (fnl (reload-doctor))

  ;; set some global
  (fnl (global my-global "some-val"))
  ;; later eval that global
  (fnl (view my-global))

  ;; topbar update
  (fnl (awful.spawn.easy_async "curl http://localhost:3334/topbar/update" nil))


  ;; handling awesome garbage
  (fnl (log_garbage))
  (fnl (handle_garbage))
  )
