(ns clawe.workspace-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clawe.workspace :as workspace]
   [clawe.wm :as wm]
   [clawe.wm.protocol :as wm.protocol]
   [ralphie.zsh :as zsh]
   [malli.core :as m]
   [systemic.core :as sys]
   [clawe.config :as clawe.config]))

(defrecord NoWorkspacesWM []
  wm.protocol/ClaweWM
  (-current-workspaces [_this _opts] []))

(def test-wsp-title "my-wsp")
(defrecord OneWorkspaceWM []
  wm.protocol/ClaweWM
  (-current-workspaces [_this _opts] [{:workspace/title test-wsp-title}])
  (-active-workspaces [_this _opts] [{:workspace/title test-wsp-title}]))


(deftest current-workspace-test
  (testing "has a title"
    (is (not (nil? (-> (workspace/current) :workspace/title)))))
  (testing "has a directory"
    (is (not (nil? (-> (workspace/current) :workspace/directory)))))

  (testing "validates against `schema`"
    (is (m/validate workspace/schema (workspace/current))))

  (testing "sets a fallback title and directory"
    (let [fallback {:workspace/title     "home"
                    :workspace/directory (zsh/expand "~")}]
      (sys/with-system [wm/*wm* (NoWorkspacesWM.)]
        (is (= fallback (workspace/current))))))

  (testing "mixes and expands data from config/workspace-def"
    (let [dir          "~/russmatney/blah"
          expected-dir (zsh/expand dir)]
      (sys/with-system
        [wm/*wm* (OneWorkspaceWM.)
         clawe.config/*config* {:workspace/defs
                                {test-wsp-title {:workspace/directory "~/russmatney/blah"}}}]
        (is (= test-wsp-title
               (-> (workspace/current) :workspace/title)))
        (is (= expected-dir
               (-> (workspace/current) :workspace/directory)))))))

(deftest all-defs-test
  (testing "re-uses the map key as the title."
    (let [dir          "~/russmatney/blah"
          expected-dir (zsh/expand dir)]
      (sys/with-system
        [clawe.config/*config*
         {:workspace/defs
          {test-wsp-title {:workspace/directory "~/russmatney/blah"}}}]
        (is (= test-wsp-title
               (-> (workspace/all-defs) first :workspace/title)))
        (is (= expected-dir
               (-> (workspace/all-defs) first :workspace/directory)))))))

(deftest all-active-test
  (testing "merges config def data"
    (let [dir          "~/russmatney/blah"
          expected-dir (zsh/expand dir)]
      (sys/with-system
        [wm/*wm* (OneWorkspaceWM.)
         clawe.config/*config*
         {:workspace/defs
          {test-wsp-title {:workspace/directory "~/russmatney/blah"}}}]
        (is (= test-wsp-title
               (-> (workspace/all-active) first :workspace/title)))
        (is (= expected-dir
               (-> (workspace/all-active) first :workspace/directory)))))))
