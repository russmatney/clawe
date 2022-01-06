(ns ralphie.bb
  (:require [ralphie.notify :as notify]
            [clojure.string :as string]
            [babashka.process :as process :refer [$]]
            [ralphie.zsh :as zsh]))

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
  (run-proc ^{:dir (zsh/expand "~")} ($ ls))
  (run-proc ^{:dir (zsh/expand "~")} ($ git fetch))
  (run-proc {:read-key :err}
            ^{:dir (zsh/expand "~/dotfiles")}
            ($ git "fetch" --verbose))
  (run-proc ^{:dir (zsh/expand "~/dotfiles")} ($ git status))
  )
