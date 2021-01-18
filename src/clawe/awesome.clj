(ns clawe.awesome
  (:require
   [babashka.process :refer [$ check]]
   [clojure.string :as string]
   [ralph.defcom :refer [defcom]]
   [ralphie.sh :as sh]
   [ralphie.notify :as notify]))

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
    (str "\"" arg "\"")

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
;; Assert Doctor
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fennel-compile [abs-path]
  (-> ^{:out :string}
      ($ fennel --compile ~abs-path)
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
      (fennel-compile abs-path))
    nil
    (catch Exception e e)))

(defn check-for-errors
  "Checks for syntax errors across your awesome config.
  Intended to prevent restarts that would otherwise crash.

  TODO maybe we just try to load the config here via `lua` or `fennel`
  - possibly it could be impled to not re-run in run-init
  (if it's already alive)"
  []
  (notify/notify "Checking AWM Config" "Syntax and Other BS")
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
                  (notify/notify "Found issue:" error)
                  (println (str file "\n" (str error) "\n\n")))))

      (do
        (notify/notify "Clean Awesome config!" (str "Checked " ct " files"))
        "No Errors."))))

(comment
  (check-for-errors))

(defcom doctor
  {:name    "doctor"
   :handler (fn [_ _]
              (check-for-errors))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reload files
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn hotswap-module-names [names]
  (->> names
       (map #(str "lume.hotswap('" % "');"))
       ;; might not be necessary
       ;; reverse ;; move 'bar' hotswap to last
       (string/join "\n")
       (awm-cli {:quiet? true})))

(def widget-filenames
  ;; TODO generate this from the awesome/widgets/* dir
  (concat
    (map (partial str "widgets.")
         ["workspaces"
          "org-pomodoro"
          "org-clock"
          "focus"
          "pomodoro"
          "dirty-repos"])
    ["bar"]))

(defn rebuild-bar []
  (awm-cli
    {:quiet? true}
    "require('bar'); init_bar();"))

(defn reload-misc []
  (hotswap-module-names
    ["clawe" "util" "icons"]))

(defn reload-widgets []
  (assert (= (check-for-errors) "No Errors."))
  (->> (concat widget-filenames)
       (hotswap-module-names))
  (rebuild-bar))

(defn reload-keybindings []
  (hotswap-module-names ["bindings"])
  (awm-cli
    {:quiet? true}
    "require('bindings'); set_global_keys();"))

(defcom reload-widgets-cmd
  {:name    "reload-widgets"
   :handler (fn [_ _]
              ;; write fancy logger with time-since-exec-start
              (println "\treloading keybindings")
              (reload-keybindings)
              (println "\treloading misc")
              (reload-misc)
              (println "\treloading widgets!")
              (reload-widgets))})

(comment)
