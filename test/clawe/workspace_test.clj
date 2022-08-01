(ns clawe.workspace-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clawe.workspace :as workspace]
   [ralphie.zsh :as zsh]
   [malli.core :as m]
   [systemic.core :as sys]
   [clawe.config :as clawe.config]))

(defrecord NoWorkspacesWM []
  workspace/ClaweWM
  (current-workspaces [_this] [])
  (current-workspaces [_this _opts] []))

(def test-wsp-title "my-wsp")
(defrecord OneWorkspaceWM []
  workspace/ClaweWM
  (current-workspaces [_this] [{:workspace/title test-wsp-title}])
  (current-workspaces [_this _opts] [{:workspace/title test-wsp-title}]))

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
      (sys/with-system [workspace/*wm* (NoWorkspacesWM.)]
        (is (= fallback (workspace/current))))))

  (testing "mixes and expands data from config/workspace-def"
    (let [dir          "~/russmatney/blah"
          expected-dir (zsh/expand dir)]
      (sys/with-system
        [workspace/*wm* (OneWorkspaceWM.)
         clawe.config/*config* {:workspace/defs
                                {test-wsp-title {:workspace/directory "~/russmatney/blah"}}}]
        (is (= test-wsp-title
               (-> (workspace/current) :workspace/title)))
        (is (= expected-dir
               (-> (workspace/current) :workspace/directory)))))))
