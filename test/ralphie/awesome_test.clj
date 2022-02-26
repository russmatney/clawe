(ns ralphie.awesome-test
  (:require [ralphie.awesome :as sut]
            [clojure.test :as t]))


(t/deftest fnl-test
  ;; These tests expect to be run against a working awesome runtime
  ;; TODO impl a strategy for mocking or testing against awesome

  (t/testing "fnl unquotes and returns a `view`ed value"
    (let [rets ["val"
                {:some-val "val"}
                ;; [{:map-in "a list"} {:of :objs}]
                ]]
      (doall
        (for [ret rets]
          (do
            (t/is (= ret (sut/fnl (view ~ret))))
            (t/is (= ret (sut/fnl (do
                                    (print ~ret)
                                    (view ~ret)))))
            (t/is (= ret (sut/fnl
                           (print ~ret)
                           (view ~ret))))
            ))))))
