(ns clawe.client.create-test
  (:require
   [clojure.test :refer [deftest is use-fixtures testing]]
   [clawe.client.create :as create]
   [clawe.config :as clawe.config]
   [clawe.wm.protocol :as wm.protocol]
   [clawe.wm :as wm]
   [systemic.core :as sys]))


(use-fixtures
  :once
  (fn [f]
    (f)
    ;; reload the config to have mercy on the repl-user
    ;; shouldn't be required b/c we're using with-system...
    (clawe.config/reload-config)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; basic def passed
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn reset-atom [{:keys [atom val]}]
  (reset! atom val))

(deftest create-client-basic-tests
  (testing "runs a passed def, include passing create/opts"
    (let [x (atom nil)]
      (is (nil? @x))
      (create/create-client {:client/create
                             {:create/cmd `reset-atom
                              :atom       x
                              :val        "some-val"}})
      (is (= "some-val" @x)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; lookup from config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest create-client-pull-defs-tests
  (testing "pulls defs by string from the config"
    (let [x (atom nil)]
      (sys/with-system
        [clawe.config/*config*
         (atom {:client/defs
                {"my-client"
                 {:client/create
                  {:create/cmd `reset-atom
                   :atom       x
                   :val        "some-val"}}}})]
        (is (nil? @x))
        (create/create-client "my-client")
        (is (= "some-val" @x)))))

  (testing "pulls defs by :client/key from the config"
    (let [x (atom nil)]
      (sys/with-system
        [clawe.config/*config*
         (atom {:client/defs
                {"my-client"
                 {:client/create
                  {:create/cmd `reset-atom
                   :atom       x
                   :val        "some-val"}}}})]
        (is (nil? @x))
        (create/create-client {:client/key "my-client"})
        (is (= "some-val" @x))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; zero-arity symbol
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce y (atom nil))
(defn my-custom-reset-atom []
  (reset! y "my-val"))

(deftest create-client-symbol-form-tests
  (testing "runs a passed 0-arity function"
    (reset! y nil)
    (is (nil? @y))
    (create/create-client {:client/create `my-custom-reset-atom})
    (is (= "my-val" @y))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; converts :create/use-workspace/title to the current-workspace's title
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn reset-atom-with-val [{:keys [atom val]}]
  (reset! atom val))

(defrecord TestWM [workspace]
  wm.protocol/ClaweWM
  (-current-workspaces [_this _opts] workspace))

(deftest create-client-use-workspace-title-test
  (testing "any val named `:create/use-workspace-title` has the current wsp title swapped in"
    (let [x (atom nil)]
      (is (nil? @x))
      (sys/with-system
        [wm/*wm* (TestWM. [{:workspace/title "wsp-title"}])]
        (create/create-client {:client/create
                               {:create/cmd `reset-atom-with-val
                                :atom       x
                                :val        :create/use-workspace-title}})
        (is (= "wsp-title" @x))))))

(deftest create-client-use-workspace-directory-test
  (testing "any val named `:create/use-workspace-directory` has the current wsp directory swapped in"
    (let [x (atom nil)]
      (is (nil? @x))
      (sys/with-system
        [wm/*wm* (TestWM. [{:workspace/directory "wsp-dir"}])]
        (create/create-client {:client/create
                               {:create/cmd `reset-atom-with-val
                                :atom       x
                                :val        :create/use-workspace-directory}})
        (is (= "wsp-dir" @x))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; exec
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest create-client-exec-test
  (testing ":exec/cmd can be used to run arbitrary processes"

    ;; not much, just making sure we don't crash
    (is (= "hi\n" (create/create-client {:client/create
                                         {:create/cmd `clawe.client.create/exec
                                          :exec/cmd   "echo hi"}})))

    (is (thrown? Exception
                 (create/create-client {:client/create
                                        {:create/cmd `clawe.client.create/exec
                                         :exec/cmd   "gibberjabber"}}))))
  (testing "exec is specified when :client/create is just a string"
    (is (= "hi\n" (create/create-client {:client/create "echo hi"})))
    (is (thrown? Exception (create/create-client {:client/create "gibberjabber"})))))
