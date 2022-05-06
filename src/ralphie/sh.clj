(ns ralphie.sh
  "This namespace should mostly go away, especially now that bb/process provides
  a better/more direct interface for processes."
  (:require
   [babashka.process :refer [$ process check]]
   [clojure.java.shell :as clj-sh]
   [clojure.string :as string]))

(comment
  (-> (process '[ls]) :out slurp)
  (slurp (:out (process '[ls])))
  (-> ($ ls) :out slurp)
  (slurp (:out ($ "ls"))))

(defn- first-or-list
  [x]
  (cond
    (string? x)     x
    (= 1 (count x)) (first x)
    (= [""] x)      nil
    :else           x))

(defn- on-fail [msg]
  (println "ralphie/sh. Err: " msg))

(defn sh
  [& args]
  (let [result
        (try
          (apply clj-sh/sh args)
          (catch Exception e
            {:err (.getMessage e)}))
        out  (some-> result :out string/split-lines first-or-list)
        err  (some-> result :err string/split-lines first-or-list)
        exit (:exit result)]
    (when (not= "" err) (on-fail err))
    {:out out :err err :exit exit}))

(defn bash [command]
  (clj-sh/sh "bash" "-c" command))

(defn zsh [command]
  (println command)
  (clj-sh/sh "zsh" "-c" command))

(comment
  (zsh (str "echo " "hi"))
  )

(defn expand
  [path & parts]
  (let [path (apply str path parts)]
    (-> (str "echo -n " path)
        (bash)
        :out)))

(comment
  (expand "~")
  (expand "~/" "russmatney")
  (-> ($ echo -n "~")
      check :out slurp))

(defn expand-many [path]
  (->
    (expand path)
    (string/split #" ")))

(comment
  (expand-many "~/russmatney/clawe/*"))
