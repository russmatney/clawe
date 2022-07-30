(ns defthing.defworkspace-test
  (:require
   [clojure.test :refer [deftest is testing] :as t]
   [defthing.defworkspace :as sut]
   [ralphie.zsh :as zsh]
   [systemic.core :as sys]
   [defthing.config :as defthing.config]))

(def home-dir (zsh/expand "~"))

(t/use-fixtures
  :once
  (fn [f]
    (sys/with-system
      [defthing.config/*config* {:db-path "~/russmatney/dbs/testdb"}]
      (f))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defworkspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest defworkspace-test
  (testing ":name"
    (sut/defworkspace test-wsp-defaults)
    (is (= "test-wsp-defaults" (-> test-wsp-defaults :name)))
    (is (= "test-wsp-defaults" (-> test-wsp-defaults :workspace/title))))

  (testing ":workspace/directory"
    (testing "defaults to ~"
      (sut/defworkspace test-directory-defaults)
      (is (= home-dir (-> test-directory-defaults :workspace/directory))))

    (testing "sets an absolute path"
      (sut/defworkspace test-directory-sets-abs-path
        {:workspace/directory "some/path"})
      (is (= (str home-dir "/some/path") (-> test-directory-sets-abs-path :workspace/directory))))

    ;; TODO support unquoting in args
    ;; (testing "supports unquote?"
    ;;   (let [path "some/other-path"]
    ;;     (sut/defworkspace test-directory-supports-unquote
    ;;       {:workspace/directory ~path})
    ;;     (is (= (str home-dir "/" path) (-> test-directory-supports-unquote :workspace/directory)))))
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; test-workspace storage
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- assert-test-workspace [wsp test-val]
  (is (= "test-workspace" (-> wsp :name)))
  (is (= test-val (-> wsp :test-val))))

(defn- assert-db-workspace-fetches
  [workspace test-val]
  (testing "get-db-workspace"
    (-> (sut/get-db-workspace workspace)
        (assert-test-workspace test-val))

    (testing "with a string"
      (-> (sut/get-db-workspace (:name workspace))
          (assert-test-workspace test-val))))

  (testing "latest-db-workspaces"
    (let [match (->> (sut/latest-db-workspaces)
                     (filter (comp #{"test-workspace"} :name))
                     first)]
      (assert-test-workspace match test-val))))

(defn- assert-in-memory-workspace-fetches
  [workspace test-val]
  (testing "get-workspace"
    (-> (sut/get-workspace workspace)
        (assert-test-workspace test-val))

    (testing "with a string"
      (-> (sut/get-workspace (:name workspace))
          (assert-test-workspace test-val))))

  (testing "list-workspaces"
    (let [match (->> (sut/list-workspaces)
                     (filter (comp #{"test-workspace"} :name))
                     first)]
      (assert-test-workspace match test-val))))

;; (deftest defworkspaces-memory-and-storage-test
;;   (let [my-random-uuid
;;         ;; TODO support uuid or move to some other thing here
;;         (random-uuid)]
;;     (testing "defworkspace macro"
;;       (sut/defworkspace test-workspace {:test-val my-random-uui

;;       (testing "in-memory fetches"
;;         (assert-in-memory-workspace-fetches
;;           test-workspace my-random-uuid))

;;       ;; syncing `all` (no params) here is problemmatic for testing,
;;       ;; b/c it'll overwrite the :test-val with whatever's in the db
;;       (testing "sync-workspaces-to-db"
;;         (let [res (sut/sync-workspaces-to-db test-workspace)]
;;           (is res)
;;           ;; some val, at least
;;           (is (-> res :datoms-transacted))))

;;       (testing "db fetches"
;;         (assert-db-workspace-fetches test-workspace my-random-uuid))

;;       (testing "supports overwrites via the db (single map)"
;;         (let [new-val "some-new-val"]
;;           (sut/sync-workspaces-to-db
;;             (assoc test-workspace :test-val new-val))

;;           (testing "db fetches"
;;             (assert-db-workspace-fetches test-workspace new-val)))

;;         (testing "supports overwrites via the db (list of maps)"
;;           (let [new-val "some-other-val"]
;;             (sut/sync-workspaces-to-db
;;               [(assoc test-workspace :test-val new-val)])

;;             (testing "db fetches"
;;               (assert-db-workspace-fetches test-workspace new-val))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; repo workspace install and storage
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- assert-repo-path [wsp path title]
  (let [dir (str home-dir "/" path)]
    (is wsp)
    (is (= path (-> wsp :repo/short-path)))
    (is (= title (-> wsp :workspace/title)))
    (is (= dir (-> wsp :workspace/directory)))
    (is (= dir (-> wsp :git/repo)))))

(deftest install-repo-workspaces-test
  (let [repo-paths
        {"some-test"          "russmatney/some-test"
         "something-test"     "/home/russ/teknql/something-test"
         "something-test-two" "/home/russ/teknql/something-test-two"}]
    (let [res (sut/install-repo-workspaces (vals repo-paths))]
      (is res)
      ;; some datoms, at least
      (is (-> res :datoms-transacted)))

    (testing "installed wsps are listed from the db"
      (let [wsps     (sut/latest-db-workspaces)
            find-wsp (fn [n]
                       (->> wsps
                            (filter (comp #{n} :name))
                            first))]
        (for [[n path] repo-paths]
          (let [wsp (find-wsp n)]
            (assert-repo-path wsp path n)))))))
