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
          (is (:git.commit/author-email commit))))))

  (testing "commits parse dates"
    (let [commits (sut/commits-for-dir {:dir "russmatney/clawe" :n 10})]
      (doall
        (for [commit commits]
          (is (:git.commit/author-date commit)))))))

(deftest ->stats-header-test
  (is (= (sut/->stats-header '("commit 7c293095d1b17f821944d28fc4e3fa38f33dcf1a"
                               "Author: Russell Matney <russell.matney@gmail.com>"
                               "Date:   Fri May 6 13:36:24 2022 -0400"))
         #:git.commit{:hash         "7c293095d1b17f821944d28fc4e3fa38f33dcf1a"
                      :author-name  "Russell Matney"
                      :author-email "russell.matney@gmail.com"
                      :author-date  "Fri May 6 13:36:24 2022 -0400"})))

;; some raw diff stats:

;; 17      20      src/ralphie/git.clj
;; 7       4       src/ralphie/sh.clj
;; 42      0       test/ralphie/git_test.clj

;; commit c4db012ed2b5b1397ec5611f891616d74c5cf976
;; Author: Russell Matney <russell.matney@gmail.com>
;; Commit: Russell Matney <russell.matney@gmail.com>

;; fix: ensure [?e :type :clawe/workspaces] on get-db-workspace

;; 2       1       src/defthing/defworkspace.clj

;; commit bff02ad57e6f79e7cf3ff5a5b502eab759ac4131
;; Author: Russell Matney <russell.matney@gmail.com>
;; Commit: Russell Matney <russell.matney@gmail.com>

;; refactor: move doctor ui.namespaces to hooks

;; 3       5       src/doctor/ui/views/screenshots.cljs
;; 2       2       src/doctor/ui/views/todos.cljs
;; 7       7       src/doctor/ui/views/topbar.cljs
;; 3       4       src/doctor/ui/views/wallpapers.cljs
;; 5       5       src/doctor/ui/views/workspaces.cljs
;; 1       1       src/{doctor/ui => hooks}/screenshots.cljc
;; 1       1       src/{doctor/ui => hooks}/todos.cljc
;; 1       1       src/{doctor/ui => hooks}/wallpapers.cljc
;; 3       6       src/{doctor/ui => hooks}/workspaces.cljc
