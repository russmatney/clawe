(ns ralphie.awesome-test
  (:require
   [clojure.test :as t]
   [ralphie.awesome :as sut]
   [clojure.string :as string]))


(t/deftest fnl-test
  ;; These tests expect to be run against a working awesome runtime
  ;; TODO impl a strategy for mocking or testing against awesome in ci

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
                           (view ~ret)))))))))

  (t/testing "pushes do-blocks together to share fennel/lua locals"
    (t/is (= "val" (sut/fnl (do
                              (local my-val "val")
                              (local my-other-val "val"))
                            (view my-val)))))

  ;; TODO how to support this `do` feature on eval, not just at macro time?
  ;; (t/testing "supports preambles for setting locals"
  ;;   (let [preamble '(do
  ;;                     (local some-val "some-val")
  ;;                     (local some-other-val "other-val"))]
  ;;     (t/is (= "val"
  ;;              ^{:quiet? false}
  ;;              (sut/fnl ~preamble
  ;;                       (view (.. some-val some-other-val)))))))

  (t/testing "fnl supports :quiet? opt via metadata."
    (let [output (with-out-str
                   ^{:quiet? true}
                   (sut/fnl (view {:val "hi"})))]
      (t/is (= output "")))
    (let [output (with-out-str
                   ^{:quiet? false}
                   (sut/fnl (view {:val "hi"})))]
      (t/is (string/includes? output "fennel code"))
      (t/is (string/includes? output "awesome-client")))))
