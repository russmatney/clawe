(ns clawe.awesome
  (:require
   [babashka.process :refer [$ check]]
   [clojure.string :as string]
   [ralph.defcom :refer [defcom]]
   ))

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
lain = require 'lain';")

(defn awm-cli
  "Expects `lua-str`, a literal string of lua.
  Adds a preamble that sets common variables and requires common modules."
  [lua-str]
  (->>
    (str lua-preamble "\n\n-- Passed command:\n" lua-str)
    ((fn [lua-str]
       (println "Running lua via awesome-client!:\n\n" lua-str)
       lua-str))
    ((fn [lua-str]
       ^{:out :string}
       ($ awesome-client ~lua-str)))
    check
    :out
    parse-output))

(comment
  (awm-cli "return 'hi';")

  (awm-cli
    (str
      "return view(lume.map(client.get(), "
      "function (t) return {name= t.name} end))"))

  (println "hello")
  (awm-cli "print('hello')")
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
  (->lua-arg {:screen "s" :tag "yodo"}))

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
;; Reload Widgets
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def widget-filenames
  ;; TODO generate this from the awesome/widgets/* dir
  #{"workspaces"
    "org-pomodoro"
    "focus"
    "pomodoro"
    "dirty-repos"})

(defn hotswap-widget-modules []
  (->> widget-filenames
       (map (partial str "widgets."))
       (map #(str "lume.hotswap('" % "');"))
       (cons "lume.hotswap('bar');")
       reverse ;; move 'bar' hotswap to last
       (string/join "\n")
       (awm-cli)))

(defn init-screen []
  (awm-cli "require('bar'); init_screen();"))

(defn reload-widgets []
  (hotswap-widget-modules)
  (init-screen))

(comment
  (reload-widgets))

(defcom reload-widgets-cmd
  {:name    "reload-widgets"
   :handler (fn [_ _] (reload-widgets))})