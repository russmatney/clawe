#!/usr/bin/env bb
;; https://book.babashka.org/#_running_tests

(require '[clojure.test :as t]
         '[babashka.classpath :as cp])

(cp/add-classpath "src:test")

;; TODO collect these automagically
(def test-nses ['ralphie.awesome-test
                'defthing.core-test
                'defthing.defcom-test])

(doall
  (for [t test-nses]
    (require t)))

(def test-results
  (apply t/run-tests test-nses))

(def failures-and-errors
  (let [{:keys [:fail :error]} test-results]
    (+ fail error)))

(System/exit failures-and-errors)
