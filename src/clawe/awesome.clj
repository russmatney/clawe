(ns clawe.awesome
  (:require
   [babashka.process :as process :refer [$ check]]
   [clojure.string :as string]
   [ralph.defcom :refer [defcom]]
   [ralphie.sh :as sh]
   [ralphie.notify :as notify]
   [ralphie.awesome :as r.awm]
   [clawe.db.scratchpad :as db.scratchpad]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Lua -> Clojure, awm-cli
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-output
  "Parses the output of awm-cli, with assumptions not worth baking into the root
  command."
  [str]
  (->>
    ;; remove leading `string` label
    (-> str string/trim (string/replace #"^string " ""))

    ;; drop quotes
    (drop 1) reverse
    (drop 1) reverse

    ;; rebuild string
    string/join

    ((fn [s]
       (try
         ;; convert to clojure data structure
         ;; TODO: use edn/read-string?
         (load-string s)
         (catch Exception _e
           (println "Exception while parsing output:" s)
           s))))))

(def lua-preamble
  "Passed ahead of awm-cli commands to provide common vars."
  "-- Preamble
awful = require('awful');
lume = require('lume');
view = require('fennelview');
inspect = require 'inspect';
s = awful.screen.focused();
lain = require 'lain';
util = require 'util';
")

(defn awm-cli
  "Expects `lua-str`, a literal string of lua.
  Adds a preamble that sets common variables and requires common modules."
  ([lua-str] (awm-cli nil lua-str))
  ([{:keys [quiet?]} lua-str]
   (->>
     (str lua-preamble "\n\n-- Passed command:\n" lua-str)
     ((fn [lua-str]
        (when-not quiet?
          (println "Running lua via awesome-client!:\n\n" lua-str))
        lua-str))
     ((fn [lua-str]
        ^{:out :string}
        ($ awesome-client ~lua-str)))
     check
     :out
     parse-output)))

(comment
  (awm-cli "return 'hi';")

  (awm-cli
    (str
      "return view(lume.map(client.get(), "
      "function (t) return {name= t.name} end))"))

  (println "hello")
  (awm-cli "print('hello from clojure')")
  (awm-cli "return view(lume.map(s.tags, function (t) return {name= t.name} end))"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clojure -> Lua, awm-fn
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->snake-case [s]
  (string/replace s "-" "_"))

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
  (->lua-arg {:org/name "my-name"})

  *1
  )

(defn awm-fn [fn & args]
  (str fn "("
       (->> args
            (map ->lua-arg)
            (string/join ", ")
            (apply str))
       ")"))

(comment
  (awm-fn "awful.layout.set" :lain.layout.centerwork)
  (= (awm-fn "awful.layout.set" :lain.layout.centerwork)
     "awful.layout.set(lain.layout.centerwork)")
  (let [args {:some-clojure "map"
              :with         :global.keywords
              :and          [{:nested  1
                              :numbers 2}]}]
    (println (awm-fn "my-fn" args))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Awesome-fnl
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn awm-fnl
  "Compiles and runs the passed string of fennel.
  See comment below for usage.
  "
  ([fnl] (awm-fnl {} fnl))
  ([opts fnl]
   (let [fnl     (-> fnl
                     (string/replace "," ""))
         ;; TODO consider removing line-breaks, commas
         lua-str (str
                   "local fennel = require('fennel'); "
                   "local compiled_lua = fennel.compileString('" fnl "'); "
                   "local run = fennel.loadCode(compiled_lua); "
                   "return run(); ") ]
     (awm-cli opts lua-str))))

(comment
  (awm-fnl '(do
              (print "hello-world!")
              (print "goodbye"))
           )

  (awm-fnl '[(println "hello-world!")])
  (awm-fnl '[;; create a function
             (fn hi [] (print "hello-from-fennel"))

             ;; call that function
             (hi)])

  ;; TODO not sure why this fails to parse
  (awm-fnl
    {:quiet? false}
    '(view {:name     client.focus.name
            :instance client.focus.instance
            }))

  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Assert Doctor
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
  (fennel-check (sh/expand "~/.config/awesome/run-init.fnl"))
  )

(defn fennel-compile [{:keys [path]}]
  (-> ^{:out :string}
      ($ fennel --compile ~path)
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
      (do
        (fennel-compile {:path abs-path})
        ;; TODO add luacheck to awm runtime's path
        ;; (fennel-check abs-path)
        ))

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

(comment
  (check-for-errors))

(defcom awesome-doctor
  {:defcom/name    "awesome-doctor"
   :defcom/handler (fn [_ _] (check-for-errors))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reloading files
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Generic

(defn hotswap-module-names [names]
  (->> names
       (map #(str "lume.hotswap('" % "');"))
       ;; might not be necessary
       ;; reverse ;; move 'bar' hotswap to last
       (string/join "\n")
       (awm-cli {:quiet? true})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Bar and Widgets

(defn rebuild-bar []
  (awm-cli
    {:quiet? true}
    "require('bar'); init_bar();"))

(def widget-filenames
  ;; TODO generate this from the awesome/widgets/* dir
  (concat
    (map (partial str "widgets.")
         ["workspaces"
          "workspace-meta"
          "org-clock"
          "workrave"
          "focus"
          "dirty-repos"])
    ["bar"]))

(defn reload-bar-and-widgets []
  (assert (= (check-for-errors) "No Errors."))
  (->> (concat widget-filenames)
       (hotswap-module-names))
  (rebuild-bar))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; keybindings

;; TODO fix to not always append on all keybindings - maybe by leaving awm completely
(defn reload-keybindings []
  (hotswap-module-names ["bindings"])
  (awm-cli
    {:quiet? true}
    "require('bindings'); set_global_keys();"))

(comment
  (awm-cli "awful.keyboard.remove_global_keybinding();")
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; misc

(defn reload-misc []
  (hotswap-module-names
    ["clawe" "util" "icons"]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clawe bar
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn build-top-bar []
  '{:position "top"
    :screen   s
    :height   (if (util.is_vader) 30 50)
    :bg       beautiful.bg_transparent})

(comment
  (->lua-arg (build-top-bar)))


(defn rebuild-bar-2 []
  (awm-cli
    {:quiet? true}
    (str "require('bar'); "
         (awm-fn "init_bar"
                 {:top-bar (build-top-bar)}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tile all clients
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn bury-all-clients-handler
  ([] (bury-all-clients-handler nil nil))
  ([_config _parsed]
   (awm-fnl '(let [s (awful.screen.focused)]
               (lume.each s.clients
                          (fn [c]
                            (set c.floating false)))))))

(defcom bury-all-clients-cmd
  {:defcom/name    "bury-all-clients"
   :defcom/handler bury-all-clients-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Client functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn client-for-name [nm]
  (some->>
    (r.awm/all-clients)
    (filter (comp
              #(string/includes? % (string/lower-case nm))
              string/lower-case
              :name))
    first))

(comment
  (client-for-name "clawe"))

(defn wrap-over-client
  "Reduces boilerplate for operating over a client.
  Expects to match on the client's window id.
  Expects to be a `c` in context.
  "
  [window-id cmd-str]
  (awm-cli
    {:quiet? true}
    (str
      "for c in awful.client.iterate(function (c) return c.window == "
      window-id
      " end) do\n" cmd-str "\nend; ")))

(defn move-client-to-tag
  "TODO create tag if it doesn't exist?"
  [window-id tag-name]
  (wrap-over-client
    window-id
    (str
      "local t = awful.tag.find_by_name(nil, \"" tag-name "\");\n"
      "if t then\n"
      "c:tags({t})\nend\n")))

(comment
  (awm-fnl '(awful.tag.find_by_name nil "datalevin"))
  (def -c (client-for-name "clawe"))

  (move-client-to-tag (:window -c) "clawe")
  )


(defn mark-buried-clients []
  (let [floating-clients
        (awm-fnl
          '(do (-> (awful.screen.focused)
                   (. :clients)
                   (lume.filter (fn [c] c.floating))
                   (lume.map (fn [c] {:window   c.window
                                      :name     c.name
                                      :class    c.class
                                      :instance c.instance
                                      :pid      c.pid
                                      :role     c.role})) view)))]
    (->> floating-clients
         (map #(db.scratchpad/mark-buried (str (:window %)) %))
         doall)))

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
   (let [{:keys [window]} client
         window           (:client/window client window)
         bury-all?        (:bury-all? opts true)
         float?           (:float? opts true)
         center?          (:center? opts true)]
     (if-not window
       (notify/notify "Set Focused called with no client :window" {:client client
                                                                   :opts   opts})
       (do
         (when bury-all? (mark-buried-clients))

         (awm-cli
           {:quiet? true}
           (str
             (when bury-all?
               (str
                 ;; set all ontops/floating false
                 "for c in awful.client.iterate(function (c) return c.ontop end) do\n"
                 "c.ontop = false; "
                 "c.floating = false; "
                 "end;\n"))

             "for c in awful.client.iterate(function (c) return c.window == "
             window
             " end) do\n"

             (when float?
               (str
                 "c.ontop = true; "
                 "c.floating = true; "))

             (when center?
               (str
                 "local f = awful.placement.centered; "
                 "f(c); "))

             ;; focus it
             "_G.client.focus = c;"
             "end; ")))))))

(defn close-client
  "Closes the passed client.
  Expects client as a map with `:window` or `:client/window`."
  [client]
  (let [{:keys [window]} client
        window           (:client/window client window)]
    (if-not window
      (notify/notify "Set Focused called with no client :window"
                     {:client client})
      (wrap-over-client window "c:kill();"))))
