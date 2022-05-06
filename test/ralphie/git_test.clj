(ns ralphie.git-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [ralphie.git :as sut]))

(deftest commits-for-dir-test
  (testing "commits parse hashes and short-hashes"
    (let [commits (sut/commits-for-dir {:dir "russmatney/clawe" :n 10})]
      (doall
        (for [commit commits]
          (do
            (is (:git.commit/short-hash commit))
            (is (:git.commit/hash commit)))))))

  (testing "commits parse parent hashes and short-hashes"
    (let [commits (sut/commits-for-dir {:dir "russmatney/clawe" :n 10})]
      (doall
        (for [commit commits]
          (do
            (is (:git.commit/parent-short-hash commit))
            (is (:git.commit/parent-hash commit)))))))

  (testing "commits parse subjects"
    (let [commits (sut/commits-for-dir {:dir "russmatney/clawe" :n 10})]
      (doall
        (for [commit commits]
          (do
            (is (:git.commit/subject commit)))))))

  (testing "commits parse authors"
    (let [commits (sut/commits-for-dir {:dir "russmatney/clawe" :n 10})]
      (doall
        (for [commit commits]
          (do
            (is (:git.commit/author-email commit)))))))

  (testing "commits parse dates"
    (let [commits (sut/commits-for-dir {:dir "russmatney/clawe" :n 10})]
      (doall
        (for [commit commits]
          (do
            (is (:git.commit/author-date commit))))))))
