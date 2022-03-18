(ns ralphie.util
  (:require
   [babashka.process :refer [$ check]]))

(defn get-cp
  "Builds a classpath in a directory."
  [dir]
  (-> ^{:dir dir}
      ($ clojure -Spath)
      check :out slurp))

(defn arity
  "Seems to work on the JVM, but not yet via babashka

  from: https://stackoverflow.com/a/47861069/860787

  Returns the maximum arity of:
    - anonymous functions like `#()` and `(fn [])`.
    - defined functions like `map` or `+`.
    - macros, by passing a var like `#'->`.

  Returns `:variadic` if the function/macro is variadic."
  [f]
  (let [func      (if (var? f) @f f)
        methods   (->> func class .getDeclaredMethods
                       (map #(vector (.getName %)
                                     (count (.getParameterTypes %)))))
        var-args? (some #(-> % first #{"getRequiredArity"})
                        methods)]
    (if var-args?
      :variadic
      (let [max-arity (->> methods
                           (filter (comp #{"invoke"} first))
                           (sort-by second)
                           last
                           second)]
        (if (and (var? f) (-> f meta :macro))
          (- max-arity 2) ;; substract implicit &form and &env arguments
          max-arity)))))

(comment
  (= 0 (arity #(println "hi")))
  (arity #(+ % 32))
  (arity #(+ %1 32)))
