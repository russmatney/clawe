(ns defthing.defcom-test
  (:require [defthing.defcom :refer [defcom] :as defcom]
            [clojure.test :refer [deftest is testing]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; General
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest example-test
  (testing "general use, expected to be most common form"
    (defcom example-cmd
      (fn [_cmd & args]
        (let [arg (some-> args first)]
          (if arg (+ 2 arg) :no-arg))))

    ;; can be executed with and without args
    (is (= 4 (defcom/exec example-cmd 2)))
    (is (= :no-arg (defcom/exec example-cmd)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handling args
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest cmd-args-tests
  (defcom cmd-all-args (fn [& _rest] :no-args))
  (is (= :no-args (defcom/exec cmd-all-args)))

  (defcom cmd-one-arg (fn [_cmd] :one-arg))
  (is (= :one-arg (defcom/exec cmd-one-arg)))

  (defcom cmd-just-body (identity :just-body))
  (is (= :just-body (defcom/exec cmd-just-body))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Named functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest named-fn-tests
  (testing "named-fn without args"
    (defn named-fn [] :named-fn)
    (defcom cmd-named-fn named-fn)

    ;; can be executed
    (is (= :named-fn (defcom/exec cmd-named-fn)))

    ;; args are passed automatically, so this needs to work
    (is (= :named-fn (defcom/exec cmd-named-fn "blah"))))


  (testing "named-fn one arg"
    (defn named-fn-with-arg [args] (apply str :blah args))
    (defcom cmd-named-one-arg named-fn-with-arg)
    (is (= ":blah" (defcom/exec cmd-named-one-arg)))
    (is (= ":blahblah" (defcom/exec cmd-named-one-arg "blah")))

    (defcom cmd-anon-fn-shorthand-wrapper #(named-fn-with-arg (rest %&)))
    (is (= ":blah" (defcom/exec cmd-anon-fn-shorthand-wrapper)))
    (is (= ":blahhiargs" (defcom/exec cmd-anon-fn-shorthand-wrapper "hiargs"))))

  (testing "named-fn two args"
    (defn named-fn-with-two-args [_cmd args] (apply str :blah- args))
    (defcom cmd-named-two-args named-fn-with-two-args)
    (is (= ":blah-" (defcom/exec cmd-named-two-args)))
    (is (= ":blah-hiargs" (defcom/exec cmd-named-two-args "hiargs")))))
