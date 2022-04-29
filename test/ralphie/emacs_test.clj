(ns ralphie.emacs-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [ralphie.emacs :as sut]))

(deftest determine-initial-file-test
  (testing "nil-punning"
    (is (= nil (sut/determine-initial-file nil))))

  ;; (testing "when passed a file that exists"
  ;;   (is (= nil (sut/determine-initial-file *file*))))

  )
