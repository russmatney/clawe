(ns defthing.defcom
  (:require
   [defthing.core :as defthing]
   [clojure.tools.cli :refer [parse-opts]]
   ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defcom macro
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro defcom
  "Creates a command via defthing-like syntax.

  The last argument to a defcom is the command itself, and can be a named
  function, an anonymous function, or a clojure form that will be treated as the
  function body.

  The first arg passed is the built defcom (itself), and the rest are any
  command line args.

  Commands are registered for a few integrations by default,
  including ralph.rofi support.

  Every command gets a :name and :doc via defthing."
  ;; NOTE all defcoms are evaled at every clawe/ralphie invocation - this may
  ;; benefit from some optimization/caching/laziness, if performance feels slow
  [command-name & xorfs]
  (let [f (last xorfs)
        command-fn
        ;; ensures the command is a function, and has the right arity
        (cond
          ;; named function, wrap if only [] arity
          (symbol? f)
          `(do
             (let [arglists# (:arglists (meta (var ~f)))]
               (cond
                 ;; named function with only zero arity
                 (and (-> arglists# count #{1}) (-> arglists# first count zero?))
                 ~(list 'fn '[& _rest] (list f))

                 ;; ignore the command, pass the args
                 ;; named function with only one arity
                 (and (-> arglists# count #{1}) (-> arglists# first count #{1}))
                 ~(list 'fn '[cmd & rst] (list f 'rst))

                 ;; named function with only two arity
                 (and (-> arglists# count #{1}) (-> arglists# first count #{2}))
                 ~(list 'fn '[cmd & rst] (list f 'cmd 'rst))

                 :else
                 ~f)))

          ;; anonymous function, use it as-is
          (some-> f first (#{'fn 'fn*})) f

          ;; just a body, wrap as a variadic fn
          :else (list 'fn '[& _rest] f))
        xorfs (->> xorfs reverse (drop 1) reverse)]
    (apply defthing/defthing :defcom/command command-name
           (conj xorfs {:defcom/fn command-fn}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; exec
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn exec
  "Executes a passed defcom, passing the command in as the first argument."
  [cmd & args]
  ;; TODO support firing defkbd's
  (apply (:defcom/fn cmd) cmd args))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; list commands
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-commands []
  (defthing/list-things :defcom/command))

(defn find-command [pred]
  (defthing/get-thing :defcom/command pred))

(defn find-command-by-name [n]
  (defthing/get-thing :defcom/command (fn [{:keys [name]}] (= n name))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; built-in commands
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom help-command
  "My help command."
  (fn [_cmd & _args]
    (println "Found commands: " (->> (list-commands)
                                     (map #(str " | " (:name %)))
                                     (apply str)))))

(defcom name-collisions
  "Checks for defcom-style commands with name collisions."
  (fn [& _]
    (->> (list-commands)
         (group-by :name)
         (filter (comp #(> % 1) count second))
         (map first)
         ((fn [names]
            (if (seq names)
              (println "Duplicate defcom names: " names)
              (println "No dupes detected")))))))

(defcom doctor
  "Checks if specified `{:defcom/depends-on [\"executables\"]}` exist, via `which`.

TODO: Could be expanded/refactored into a more general defthing/doctor command."
  (fn [& _]
    (let [deps
          (->> (list-commands)
               (mapcat :doctor/depends-on)
               (into #{})
               ;; TODO finish this impl
               ;; (filter
               ;;   (fn [executable]
               ;;     (-> (proc/$ which ~executable)
               ;;         proc/check :out :string)))
               ;; ((fn [deps-missing]
               ;;    (if (seq deps-missing)
               ;;      (println "Missing expected executables: " deps-missing)
               ;;      (println "No dupes detected"))))
               )]
      (println "dependencies: " deps)
      deps)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn run [& args]
  ;; intended to be suitable as a -main function or bb task
  (let [{:keys [arguments]} (parse-opts args [])
        command-name        (some-> arguments first)
        command             (when command-name (find-command-by-name command-name))]
    (when-not command-name
      (println "No command name received. args: " arguments))
    (when (and command-name (not command))
      (println "No command found for command-name: "
               command-name " from args: " arguments))
    (if command
      (do
        ;; TODO honor some configured logger
        ;; TODO consider integrating performance metrics
        (println "[defcom] exec: " command-name)
        (println "[defcom] doc: " (:doc command))
        (exec command (rest arguments)))
      ;; signal that no command was found
      ;; consumers may decide to print a help message
      :not-found)))
