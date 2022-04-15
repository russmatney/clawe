#!/usr/bin/env bb
;; https://book.babashka.org/#_running_tests

(require '[clojure.test :as t]
         '[babashka.process :as p]
         '[babashka.classpath :as cp]
         )

(cp/add-classpath "src:test")

(def is-mac?
  (let [ostype
        (-> ^{:out :string}
            (p/$ "zsh -c 'echo -n $OSTYPE'")
            p/check :out)]
    (re-seq #"^darwin" ostype)))

;; TODO collect these automagically
(def test-nses
  (->>
    [(when-not is-mac? 'ralphie.awesome-test)
     'defthing.core-test
     'defthing.defcom-test]
    (remove nil?)))

(doall
  (for [t test-nses]
    (require t)))

(def test-results
  (apply t/run-tests test-nses))

(def failures-and-errors
  (let [{:keys [:fail :error]} test-results]
    (+ fail error)))

(System/exit failures-and-errors)
