(ns clawe.client-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clawe.client :as client]
   [clawe.wm :as wm]))

(deftest unambiguous-matching-test
  (testing "unambiguous matching"
    (let [clients (wm/active-clients)]
      (doall
        (for [c clients]
          (let [fetched (wm/fetch-client c)]
            (is (= fetched c))))))))

(deftest match-test
  (testing "basic match"
    (is (client/match?
          {:client/window-title "journal" :client/app-name "Emacs"}
          {:client/window-title "journal" :client/app-name "Emacs"})))

  (testing "loose title match"
    (let [a {:client/window-title "journal - 65x48 w/e osx does" :client/app-name "Emacs"}
          b {:client/window-title "journal" :client/app-name "Emacs"}]
      (is (not (client/match? b a)))
      (is (not (client/match? a b)))
      (is (client/match? {:match/soft-title true} b a))
      (is (client/match? {:match/soft-title true} a b))))

  (testing "no app-names, false"
    (is (not (client/match?
               {:client/window-title "journal"}
               {:client/window-title "journal"}))))

  (testing "using other app-names"
    (let [a {:client/window-title "some bs title" :client/app-name "weird app name"
             :client/app-names    ["Slack"]}
          b {:client/window-title "i'm dynamic!" :client/app-name "Woo!"
             :client/app-names    ["Slack"]}]
      (is (not (client/match? a b)))
      (is (client/match? {:match/skip-title true} a b)))))
