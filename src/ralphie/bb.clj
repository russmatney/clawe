(ns ralphie.bb
  (:require
   [ralphie.notify :as notify]
   [clojure.string :as string]
   [babashka.process :as process :refer [$]]
   [babashka.fs :as fs]
   [ralphie.zsh :as zsh]
   [clojure.edn :as edn]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; run command helper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn run-proc
  "Runs the passed babashka process in the dir, catching errors.
  "
  ([proc] (run-proc nil proc))
  ([{:keys [error-message dir read-key]} proc]
   (let [read-key      (or read-key :out)
         error-message (or error-message (str "Ralphie Error: "
                                              (:cmd proc) " " dir))]
     (try
       (some-> proc
               process/check read-key
               slurp
               ((fn [x]
                  (if (re-seq #"\n" x)
                    (string/split-lines x)
                    (if (empty? x) nil x)))))
       (catch Exception e
         (println error-message e)
         (notify/notify error-message e))))))

(comment
  (run-proc ^{:dir (fs/home)} ($ ls))
  (run-proc ^{:dir (fs/home)} ($ git fetch))
  (run-proc {:read-key :err}
            ^{:dir (str (fs/home) "/dotfiles")}
            ($ git "fetch" --verbose))
  (run-proc ^{:dir (str (fs/home) "/dotfiles")} ($ git status)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; tasks
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-task [line]
  (let [split       (string/split line #" +")
        cmd         (first split)
        description (apply str (string/join " " (rest split)))
        parsed      {:bb.task/cmd cmd}]
    (if (seq description)
      (assoc parsed :bb.task/description description)
      parsed)))

(comment
  (parse-task "clj-kondo")
  (parse-task "run-tests     Runs all the tests")
  (string/split "run-tests     Runs all the tests" #" +"))

;; TODO there's probably some better way, like just parsing bb.edn directly...
(defn tasks [dir]
  (when dir
    (let [dir (zsh/expand dir)]
      (when (fs/exists? (str dir "/bb.edn"))
        (try
          (->
            ^{:dir dir :out :string}
            ($ bb tasks)
            process/check
            :out
            string/split-lines
            (->>
              (drop 2)
              (map parse-task)))
          (catch Exception e
            (notify/notify "Error parsing repo bb.edn" e)
            (println "Error parsing repo bb.edn" e)))))))

(comment
  (tasks "~/russmatney/clawe")
  (tasks "~/teknql/aave"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; deps
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn deps-from [path]
  (when (fs/exists? path)
    (let [edn (edn/read-string (slurp path))]
      ;; TODO walk to aliases
      (->> (:deps edn) (into #{})))))

(comment
  (deps-from (str (fs/home) "/russmatney/clawe/deps.edn"))
  )

(defn deps [dir]
  (when dir
    (let [dir      (zsh/expand dir)
          bb-edn   (str dir "/bb.edn")
          deps-edn (str dir "/deps.edn")]
      (concat
        (deps-from bb-edn)
        (deps-from deps-edn)))))

(comment
  (deps "~/russmatney/clawe")
  (deps "~/teknql/aave"))
