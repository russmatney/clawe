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
   [babashka.process :as process :refer [check]]
   [backtick]
   [clojure.pprint]
   [clojure.string :as string]
   [defthing.defcom :refer [defcom] :as defcom]
   [ralphie.notify :as notify]
   [ralphie.sh :as sh]))

;;  TODO rename `awm-cli` to `awm-lua`, and write a shared `awm-cli` base. or just
;;  support `awm-cli` and check for a leading `(`.

;; TODO support a dynamic awm/*preamble* context for `awm-cli` functions.

(comment
  (ralphie.awesome/fnl
    (-> (client.get) (lume.map (fn [t] {:name (. t :name)})) view))

  (ralphie.awesome/awm-fnl
    '(-> (client.get) (lume.map (fn [t] {:name t.name})) view))

  (ralphie.awesome/awm-lua
    (str
      "return view(lume.map(client.get(), "
      "function (t) return {name= t.name} end))")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; awm-lua preamble
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def lua-preamble
  "Passed ahead of awm-lua commands to provide commonly used globals."
  ;; TODO couldn't these be globals, declared once?
  ;; maybe it's not expensive to re-require these?
  "-- Preamble
awful = require('awful');
lume = require('lume');
view = require('fennelview');
inspect = require 'inspect';
s = awful.screen.focused();
lain = require 'lain';
util = require 'util';
")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Eval lua in awesome-context
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- parse-output
  "Parses the output of awm-lua, with assumptions not worth baking into the root
  command.

  Handles parsing `view` (fennelview) output back into clojure structures.
  If parsing fails (e.g. b/c a table or some unsupported structure is returned),
  an exception is printed and the raw string is returned.
  "
  [str]
  (let [trimmed (string/trim str)
        to-load (cond
                  ;; remove leading `string` label
                  (re-seq #"^string" trimmed)
                  (->>
                    (string/replace trimmed #"^string " "")

                    ;; drop quotes
                    (drop 1) reverse
                    (drop 1) reverse

                    ;; rebuild string
                    string/join)

                  (re-seq #"^boolean" trimmed)
                  (string/replace trimmed #"^boolean " "")

                  (re-seq #"^double" trimmed)
                  (string/replace trimmed #"^double " ""))]
    (try
      ;; convert to clojure data structure
      ;; (edn/read-string to-load)
      (load-string to-load)
      (catch Exception _e
        (println "Exception while parsing output:" trimmed to-load)
        ;; (println "e" e)
        to-load))
    ))

(defn awm-cli
  "Expects `lua-str`, a literal string of lua.
  Adds a preamble that sets common variables and requires common modules."
  ([lua-str] (awm-cli nil lua-str))
  ([opts lua-str]
   (when-not notify/is-mac? ;; TODO better detection/handling for awm running
     (let [quiet? (:quiet? opts true)]
       (->>
         (str lua-preamble "\n\n-- Passed command:\n" lua-str)
         ((fn [lua-str]
            (when-not quiet?
              (println "Running lua via awesome-client!:\n\n" lua-str))
            lua-str))
         ((fn [lua-str]
            ^{:out :string}
            (process/$ awesome-client ~lua-str)))
         check
         :out
         parse-output)))))

(def awm-lua awm-cli)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; converts a clojure map to a lua table
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->lua-key [s]
  (-> s
      (string/replace "-" "_")
      (string/replace "?" "")))

(defn ->lua-arg
  "Converts the passed arg to a string representing lua syntax.

  Strings are wrapped in quotes, keywords are not. This is to allow passing
  references to global vars, such as `lain.layout.centerwork`."
  [arg]
  (cond
    (nil? arg)
    "nil"

    (boolean? arg)
    (str arg)

    (string? arg)
    (str "\""
         ;; TODO escape this string
         (string/replace arg "\"" "\\\"")
         "\"")

    (keyword? arg)
    (apply str (rest (str arg)))

    (int? arg)
    arg

    (map? arg)
    (->> arg
         (map (fn [[k v]]
                (str "\n" (->lua-key (name k)) " = " (->lua-arg v))))
         (string/join ", ")
         (#(str "{" % "} \n")))

    (coll? arg)
    (->> arg
         (map (fn [x]
                (->lua-arg x)))
         (string/join ",")
         (#(str "{" % "} \n")))))

(comment
  (string? "hello")
  (->lua-arg "hello")
  (->lua-arg :lain.layout.centerwork)
  (->lua-arg {:level 1 :status :status/done})
  (->lua-arg {:fix-keyword 1})
  ;; drop question marks
  (->lua-arg {:clean? nil})
  (->lua-arg {:clean? false})
  (->lua-arg {:screen "s" :tag "yodo"}
             )
  (->lua-arg {:org/name "my-name"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; eval Fennel in awm context
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn awm-fnl
  "Compiles and runs the passed string of fennel.

  Exs.

  (awm-fnl '(do
              (print \"hello-world!\")
              (print \"goodbye\")))
  (awm-fnl '[(view {:some-data \"hello-world!\"})])
  (awm-fnl '[;; create a function
             (fn hi [] (print \"hello-from-fennel\"))

             ;; call that function
             (hi)])

  See: `ralphie.awesome/fnl` for simpler fennel + interpolation/unquoting
  "
  ([fennel] (awm-fnl {} fennel))
  ([opts fennel]
   (let [quiet? (:quiet? opts true)]
     (when-not quiet? (println "\nfennel code:" fennel "\n"))
     (let [fennel  (-> fennel (string/replace "," ""))
           lua-str (str
                     "local fennel = require('fennel'); \n"
                     "local compiled_lua = fennel.compileString('" fennel "'); \n"
                     "local run = fennel.loadCode(compiled_lua); "
                     "return run(); ") ]
       (awm-lua opts lua-str)))))

(comment
  (awm-fnl '(do
              (print "hello-world!")
              (print "goodbye")))

  (awm-fnl '[(println "hello-world!")])
  (awm-fnl '[;; create a function
             (fn hi [] (print "hello-from-fennel"))

             ;; call that function
             (hi)])

  (awm-fnl
    '(view {:name     client.focus.name
            :instance client.focus.instance})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fnl macro
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro fnl
  "The main public interface to awesomeWM.

  Expects one or more fennel forms as arguments, which are eventually
  stringified and passed to `awm-fnl`.

  Uses `backtick/template` to prevent namespace-qualification of the forms while
  still supporting `~` for unquoting, i.e. interpolating values in fennel code.

  Exs.

  (let [val \"some-val\"]
    (fnl
      ;; require naughty
      (local naughty (require :naughty))

      ;; fire a notification via awesome
      (naughty.notify
        {:title \"Test notif\"
         :text  (.. \"some sub head: \" ~val)}) ;; `~` unquotes `val`

      ;; return the current focused client's name
      _G.client.focus.name))

  Options can be passed via metadata:

  ^{:quiet? false} (fnl (print \"hello\"))

  ;; TODO support this at runtime, not just macro time
  If the first form is a `do` expression, the rest of the forms will be included
  in the same `do`. This supports setting some state for later fnl expressions
  via lua/fennel's `local`, which is more or less a `let` for setting vars in
  the current scope.
  "
  [& fnl-forms]
  (let [opts       (meta &form)
        opts       (assoc opts :quiet? (:quiet? opts true))
        first-form (first fnl-forms)
        rest-forms (rest fnl-forms)

        ;; syntax eval first-form early to see if it's a do block? not sure that's possible

        first-form-starts-with-do (#{'do} (first first-form))
        fnl-forms                 (if first-form-starts-with-do
                                    (concat
                                      ;; drop the do, we'll use the one in the template below
                                      (rest first-form) rest-forms)
                                    fnl-forms)]
    `(awm-fnl ~opts (backtick/template (do ~@fnl-forms)))))

(comment
  (let [val "some-val"]
    (fnl
      ;; require naughty
      (local naughty (require :naughty))

      ;; fire a notification via awesome
      (naughty.notify
        {:title "Test notif"
         :text  (.. "some sub head: " ~val)})

      ;; return the current focused client's name
      _G.client.focus.name)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; bags of data from awesome
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO rework these with malli


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; AwesomeWM data fetchers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn screen []
  (-> (fnl
        (view
          {:geometry (. s :geometry)
           :tags     (lume.map
                       s.tags
                       (fn [t]
                         ;; could fetch more tag details here...
                         {:name  (. t :name)
                          :index (. t :index)}))}))
      ->namespaced-screen))

(comment
  (screen))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; tag fetchers

(defn ->namespaced-client
  "Recieves a raw-awm `client`, and moves data to namespaced keywords."
  [client]
  {:awesome/client          (dissoc client :name :urgent :instance :type :pid :class :ontop :master :window :focused
                                    :tag-names)
   :awesome.client/name     (:name client)
   :awesome.client/class    (:class client)
   :awesome.client/instance (:instance client)
   :awesome.client/window   (:window client)
   :awesome.client/pid      (:pid client)

   :awesome.client/tag-names (:tag-names client)
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
  ([] (fetch-tags {}))
  ([_opts]
   ;; TODO consider filtering on passed tag names, current tag
   (try
     (->>
       (fnl
         (local focused-window (if client.focus client.focus.window nil))
         (local m-client (awful.client.getmaster))
         (local m-window (if m-client m-client.window nil))

         (->
           (root.tags)
           (lume.map
             (fn [t]
               {:name     t.name
                :selected t.selected
                :index    t.index
                :urgent   t.urgent
                :layout   (-?> t (. :layout) (. :name))
                :clients
                (->
                  (t:clients)
                  (lume.map
                    (fn [c]
                      {:name      (. c :name)
                       :ontop     c.ontop
                       :window    c.window
                       :urgent    c.urgent
                       :type      c.type
                       :class     c.class
                       :instance  c.instance
                       :pid       c.pid
                       :role      c.role
                       :tag-names (-> (c:tags) (lume.map (fn [x] (. x :name))))
                       :master    (= m-window c.window)
                       :focused   (= focused-window c.window)})))}))
           view))
       (map ->namespaced-tag))
     (catch Exception _e
       (println "awm/fetch-tags error")
       nil))))

(comment
  (->>
    (fetch-tags)
    first
    :awesome.tag/clients
    first
    )

  (fnl
    (->
      (root.tags)
      (lume.map
        (fn [t]
          {:clients
           (->
             (t:clients)
             (lume.map
               (fn [c]
                 {:tag-names (-> (c:tags) (lume.map (fn [x] (. x :name))))})))}))
      view))

  )


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

;; TODO benchmark these - the fennel version may have more overhead, or it could be negligible
(defn current-tag-name []
  (awm-lua "return s.selected_tag.name"))
(defn current-tag-name-2 []
  ;; interesting that i couldn't just `s.selected-tag.name`
  (fnl (. s.selected_tag :name)))

(comment
  ;; TODO benchmark
  (current-tag-name)
  (current-tag-name-2))

(defn current-tag-names []
  (try
    (fnl (->
           s.selected_tags
           (lume.map (fn [t] (. t :name)))
           view))
    (catch Exception _e
      nil)))

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
      (-> (client.get)
          (lume.map (fn [c]
                      {:name      (. c :name)
                       :geometry  (c:geometry)
                       :window    c.window
                       :type      c.type
                       :class     c.class
                       :instance  c.instance
                       :pid       c.pid
                       :role      c.role
                       :tags      (lume.map (c:tags) (fn [t] {:name (. t :name)}))
                       :first_tag c.first_tag.name}))
          view))
    (map ->namespaced-client)))

(comment
  (->> (all-clients)
       (filter (comp #(= % "tauri/doctor-topbar") :awesome.client/name))))

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

(defn create-tag! [tag-name]
  (fnl (awful.tag.add ~tag-name {:layout awful.layout.suit.tile})))

(defn ensure-tag [tag-name]
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

(defn delete-tag! [tag-name]
  (fnl
    #_{:clj-kondo/ignore [:unused-binding]}
    (let [tag (awful.tag.find_by_name nil ~tag-name)]
      (tag:delete))))

(comment
  (delete-tag! "slack")
  (delete-tag! "dotfiles")
  (focus-tag! "slack")
  )

(defn delete-current-tag! []
  (fnl (s.selected_tag:delete)))

(defcom awesome-delete-current-tag
  "Deletes the current focused tag."
  delete-current-tag!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; common awm client functions

(defcom set-above-and-ontop
  (do
    (notify/notify "Setting above and ontop")
    (fnl
      (tset _G.client.focus :ontop true)
      (tset _G.client.focus :above true))))

(defn bury-all-clients []
  (fnl
    (let [s (awful.screen.focused)]
      (lume.each (. s :clients)
                 (fn [c]
                   (tset c :floating false))))))

(defcom bury-all-clients-cmd bury-all-clients)

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
  (awm-fnl '(awful.tag.find_by_name nil "datalevin"))
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
      check))

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
;; Reloading in the running awm instance
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn hotswap-module-names
  "Hotswap lua modules by name."
  [names]
  (->> names
       (map #(str "lume.hotswap('" % "');"))
       ;; might not be necessary
       ;; reverse ;; move 'bar' hotswap to last
       (string/join "\n")
       (awm-lua {:quiet? true})))

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
