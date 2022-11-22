(ns ^:awesomewm ralphie.awesome.fnl-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [ralphie.awesome.fnl :as awm.fnl]
   [clojure.string :as string]))

;; These tests expect to be run against a working awesome runtime
;; TODO impl a strategy for mocking or testing against awesome in ci


(deftest awm-lua-parse-output-test
  (testing "parses numbers"
    (is (= 4.5 (awm.fnl/awm-lua "return 4.5")))
    (is (= 4 (awm.fnl/awm-lua "return 4"))))

  (testing "parses strings"
    (is (= "hi" (awm.fnl/awm-lua "return 'hi'"))))

  (testing "parses bools"
    (is (= true (awm.fnl/awm-lua "return true")))
    (is (= false (awm.fnl/awm-lua "return false"))) )

  (testing "parses maps"
    (is (= {:name "hi"} (awm.fnl/awm-lua "return view({name='hi'})"))))

  (testing "parses lists"
    (is (= [{:name "hi"} {:name "bye"}]
           (awm.fnl/awm-lua "return view({{name='hi'}, {name='bye'}})"))))

  (testing "returns tables"
    (is (string/includes? (awm.fnl/awm-lua "return {}") "table:"))))



(deftest fnl-test
  (testing "fnl unquotes and returns a `view`ed value"
    (let [rets ["val"
                {:some-val "val"}
                ;; [{:map-in "a list"} {:of :objs}]
                ]]
      (doall
        (for [ret rets]
          (do
            (is (= ret (awm.fnl/fnl (view ~ret))))
            (is (= ret (awm.fnl/fnl (do
                                      (print ~ret)
                                      (view ~ret)))))
            (is (= ret (awm.fnl/fnl
                         (print ~ret)
                         (view ~ret)))))))))

  (testing "pushes do-blocks together to share fennel/lua locals"
    (is (= "val" (awm.fnl/fnl (do
                                (local my-val "val")
                                (local my-other-val "val"))
                              (view my-val)))))

  ;; TODO how to support this `do` feature on eval, not just at macro time?
  (testing "supports preambles for setting locals"
    (let [preamble '(do
                      (local some-val "some-val")
                      (local some-other-val "other-val"))]
      ;; this throws an error when parsing, but returns the val fine
      (is (= "some-val-other-val"
             (awm.fnl/fnl ~(concat preamble
                                   (backtick/template (view (.. some-val "-" some-other-val)))))))))

  (testing "fnl supports :quiet? opt via metadata."
    (let [output (with-out-str (awm.fnl/fnl (view {:val "hi"})))]
      (is (= output "")))
    (let [output (with-out-str
                   ^{:quiet? false}
                   (awm.fnl/fnl (view {:val "hi"})))]
      (is (string/includes? output "fennel code"))
      (is (string/includes? output "awesome-client")))))
