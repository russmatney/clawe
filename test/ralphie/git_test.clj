(ns ^:integration ralphie.git-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [ralphie.git :as sut]))

(deftest commits-test
  (testing "commits parse hashes and short-hashes"
    (let [commits (sut/commits {:dir "russmatney/clawe" :n 10})]
      (doall
        (for [commit commits]
          (do
            (is (:commit/short-hash commit))
            (is (:commit/hash commit)))))))

  (testing "commits parse parent hashes and short-hashes"
    (let [commits (sut/commits {:dir "russmatney/clawe" :n 10})]
      (doall
        (for [commit commits]
          (do
            (is (:commit/parent-short-hash commit))
            (is (:commit/parent-hash commit)))))))

  (testing "commits parse subjects"
    (let [commits (sut/commits {:dir "russmatney/clawe" :n 10})]
      (doall
        (for [commit commits]
          (is (:commit/subject commit))))))

  (testing "commits parse authors"
    (let [commits (sut/commits {:dir "russmatney/clawe" :n 10})]
      (doall
        (for [commit commits]
          (is (:commit/author-email commit))))))

  (testing "commits parse dates"
    (let [commits (sut/commits {:dir "russmatney/clawe" :n 10})]
      (doall
        (for [commit commits]
          (is (:commit/author-date commit)))))))

(deftest ->stats-header-test
  (is (= (sut/->stats-header '("commit 7c293095d1b17f821944d28fc4e3fa38f33dcf1a"
                               "Author: Russell Matney <russell.matney@gmail.com>"
                               "Date:   Fri May 6 13:36:24 2022 -0400"))
         #:commit{:hash         "7c293095d1b17f821944d28fc4e3fa38f33dcf1a"
                  :author-name  "Russell Matney"
                  :author-email "russell.matney@gmail.com"
                  :author-date  "Fri May 6 13:36:24 2022 -0400"})))

(deftest ->stat-lines
  (is (= (sut/->stats
           '("3\t5\tsrc/doctor/ui/views/screenshots.cljs" "1\t1\tsrc/{doctor/ui => hooks}/screenshots.cljc"))
         #:commit{:lines-added   4
                  :lines-removed 6
                  :files-renamed 1
                  :stat-lines
                  '(#:git.stat{:lines-added   3
                               :lines-removed 5
                               :raw-file-line "src/doctor/ui/views/screenshots.cljs"
                               :is-rename     false
                               :no-diff       false}
                     #:git.stat{:lines-added   1
                                :lines-removed 1
                                :raw-file-line "src/{doctor/ui => hooks}/screenshots.cljc"
                                :is-rename     true
                                :no-diff       false})}))


  ;; TODO restore
  #_(is (= (sut/->stats
             '("-\t-\tassets/robot.aseprite"
               "-\t-\tassets/robot_sheet.png"
               "42\t8\tlevels/Arcade.tscn"
               "25\t1\tlevels/Park.tscn"
               "7\t5\tmobs/Mobot.tscn"
               "4\t5\tplayer/Player.gd"
               "48\t35\tplayer/Player.tscn"
               "1\t1\tproject.godot"))
           nil
           ))

  )

(deftest ->stats-commit-test
  (is (= (sut/->stats-commit
           '(( "commit 7c293095d1b17f821944d28fc4e3fa38f33dcf1a" "Author: Russell Matney <russell.matney@gmail.com>" "Date:   Fri May 6 13:36:24 2022 -0400" )
             ( "    fix: update server deps" "    " "    Could use some testing on these servers too!" "    " "    Testing a body with multiple lines." ... )
             ( "2\t4\tsrc/expo/server.clj" )))
         #:commit{:hash          "7c293095d1b17f821944d28fc4e3fa38f33dcf1a"
                  :author-name   "Russell Matney"
                  :author-email  "russell.matney@gmail.com"
                  :author-date   "Fri May 6 13:36:24 2022 -0400"
                  :lines-added   2
                  :lines-removed 4
                  :files-renamed 0
                  :stat-lines
                  '(#:git.stat{:lines-added   2
                               :lines-removed 4
                               :raw-file-line "src/expo/server.clj"
                               :is-rename     false
                               :no-diff       false})}))

  (let [stats (sut/->stats-commit
                '(("commit bff02ad57e6f79e7cf3ff5a5b502eab759ac4131" "Author: Russell Matney <russell.matney@gmail.com>" "Date:   Sun May 1 20:13:53 2022 -0400")
                  ("    refactor: move doctor ui.namespaces to hooks")
                  ("3\t5\tsrc/doctor/ui/views/screenshots.cljs" "2\t2\tsrc/doctor/ui/views/todos.cljs" "7\t7\tsrc/doctor/ui/views/topbar.cljs" "3\t4\tsrc/doctor/ui/views/wallpapers.cljs" "5\t5\tsrc/doctor/ui/views/workspaces.cljs" "1\t1\tsrc/{doctor/ui => hooks}/screenshots.cljc" "1\t1\tsrc/{doctor/ui => hooks}/todos.cljc" "1\t1\tsrc/{doctor/ui => hooks}/wallpapers.cljc" "3\t6\tsrc/{doctor/ui => hooks}/workspaces.cljc")))]
    (is (= 26 (:commit/lines-added stats)))
    (is (= 32 (:commit/lines-removed stats)))
    (is (= 4 (:commit/files-renamed stats)))
    (is (= 4 (:commit/files-renamed stats)))
    (is (= {:git.stat/lines-added   3
            :git.stat/lines-removed 5
            :git.stat/raw-file-line "src/doctor/ui/views/screenshots.cljs"
            :git.stat/is-rename     false
            :git.stat/no-diff       false}
           (first (:commit/stat-lines stats))))))
