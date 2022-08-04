(ns clawe.client-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clawe.client :as client]
   [clawe.wm :as wm]))

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

(deftest matching-client-defs-test
  (testing "no matching app-names, should not match"
    (is (not (client/match?
               {:client/window-title "my-client" :client/app-name "some-app"}
               {:client/app-names ["my" "app" "names"] :client/key "my-client-key"}))))

  (testing "matching app-names, should match"
    (is (client/match?
          {:client/window-title "my-client" :client/app-name "some-app"}
          {:client/app-names ["some-app" "app" "names"] :client/key "my-client-key"})))

  (testing "matching app-names, but not titles, should not match"
    (is (not (client/match?
               {:client/window-title "my-client" :client/app-name "some-app"}
               {:client/window-title "my-client-with-a-diff-title"
                :client/app-names    ["some-app" "app" "names"] :client/key "my-client-key"}))))

  (testing "matching app-names, not titles, but :match/soft-title matches, should match"
    (is (client/match?
          {:match/soft-title true}
          {:client/window-title "my-client" :client/app-name "some-app"}
          {:client/window-title "my-client-with-a-diff-title"
           :client/app-names    ["some-app" "app" "names"] :client/key "my-client-key"})))

  (testing "matching app-names, not titles, :match/soft-title passed, but no match"
    (is (not (client/match?
               {:match/soft-title true}
               {:client/window-title "my-client" :client/app-name "some-app"}
               {:client/window-title "some-very-diff-title"
                :client/app-names    ["some-app" "app" "names"] :client/key "my-client-key"}))))

  (testing "matching app-names, not titles, :match/skip-title passed, should match"
    (is (client/match?
          {:match/skip-title true}
          {:client/window-title "my-client" :client/app-name "some-app"}
          {:client/window-title "some-very-diff-title"
           :client/app-names    ["some-app" "app" "names"] :client/key "my-client-key"}))))