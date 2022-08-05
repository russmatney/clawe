(ns ralphie.awesome.fnl
  (:require
   [babashka.process :as process]
   [backtick]
   [clojure.string :as string]
   [ralphie.notify :as notify]))

(comment
  (ralphie.awesome.fnl/fnl
    (-> (client.get) (lume.map (fn [t] {:name (. t :name)})) view))

  (ralphie.awesome.fnl/awm-fnl
    '(-> (client.get) (lume.map (fn [t] {:name t.name})) view))

  (ralphie.awesome.fnl/awm-lua
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
                  (string/replace trimmed #"^double " "")

                  (= "nil" trimmed)
                  nil)]
    (when to-load
      (try
        ;; convert to clojure data structure
        ;; (edn/read-string to-load)
        (load-string to-load)
        (catch Exception _e
          (println "awesome.fnl exception while parsing output:" trimmed to-load)
          ;; if we fail to load-string, return the string version
          to-load)))))

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
         process/check
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
