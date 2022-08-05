(ns ralphie.awesome.fnl-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [ralphie.awesome.fnl :as sut]
   [clojure.string :as string]))

;; These tests expect to be run against a working awesome runtime
;; TODO impl a strategy for mocking or testing against awesome in ci

(deftest fnl-test
  (testing "fnl unquotes and returns a `view`ed value"
    (let [rets ["val"
                {:some-val "val"}
                ;; [{:map-in "a list"} {:of :objs}]
                ]]
      (doall
        (for [ret rets]
          (do
            (is (= ret (sut/fnl (view ~ret))))
            (is (= ret (sut/fnl (do
                                  (print ~ret)
                                  (view ~ret)))))
            (is (= ret (sut/fnl
                         (print ~ret)
                         (view ~ret)))))))))

  (testing "pushes do-blocks together to share fennel/lua locals"
    (is (= "val" (sut/fnl (do
                            (local my-val "val")
                            (local my-other-val "val"))
                          (view my-val)))))

  ;; TODO how to support this `do` feature on eval, not just at macro time?
  (testing "supports preambles for setting locals"
    (let [preamble '(do
                      (local some-val "some-val")
                      (local some-other-val "other-val"))]
      (is (= "some-val-other-val"
             ^{:quiet? false}
             (sut/fnl ~(concat preamble
                               (backtick/template (view (.. some-val "-" some-other-val)))))))))

  (testing "fnl supports :quiet? opt via metadata."
    ;; TODO this test fails if a println is left in dependent functions.......
    (let [output (with-out-str
                   ^{:quiet? true}
                   (sut/fnl (view {:val "hi"})))]
      (is (= output "")))
    (let [output (with-out-str
                   ^{:quiet? false}
                   (sut/fnl (view {:val "hi"})))]
      (is (string/includes? output "fennel code"))
      (is (string/includes? output "awesome-client")))))
