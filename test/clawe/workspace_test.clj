(ns clawe.workspace-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clawe.workspace :as subject]
   [ralphie.zsh :as zsh]
   [malli.core :as m]))

(deftest current-workspace-test
  (testing "has a title"
    (is (not (nil? (-> (subject/current-workspace) :workspace/title)))))
  (testing "has a directory"
    (is (not (nil? (-> (subject/current-workspace) :workspace/directory)))))

  (testing "validates against `schema`"
    (is (m/validate subject/schema (subject/current-workspace))))

  (testing "sets a fallback title and directory"
    (let [fallback
          {:workspace/title     "home"
           :workspace/directory (zsh/expand "~")}]
      (is (= fallback (subject/current-workspace))))))
