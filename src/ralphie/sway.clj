(ns ralphie.sway
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
   [babashka.process :as process]))

(comment :hi (+ 3 4))

(defn ensure-emacs [{:keys [name] :as wsp-data}]
  (try
    (let [initial-file nil
          eval-str     (str
                         "(progn "
                         " (russ/open-workspace \"" name "\") "
                         (when initial-file
                           (str " (find-file \"" initial-file "\") " " "))
                         " )")]

      (-> (process/$ emacsclient --no-wait --create-frame
                     -F ~(str "((name . \"" name "\"))")
                     --eval ~eval-str)
          process/check))
    (catch Exception e (println e))))

(defn ensure-workspace
  "Create instances for the passed workspace data"
  [{:keys [name] :as data}]
  ;; ensure emacs workspace
  (ensure-emacs data)
  ;; TODO ensure tmux workspace
  ;; TODO ensure sway workspace
  name)

(comment
  (ensure-workspace {:name "clawe"}))

(defn sway-msg! [msg]
  (let [cmd (str "swaymsg " msg)]
    (-> (process/process
          {:cmd cmd :out :string})
        ;; throws when error occurs
        (process/check)
        :out
        (json/parse-string
          (fn [k] (keyword "sway" k))))))

(def workspaces
  {1 {:workspace/name      "dotfiles"
      :workspace/directory "~/dotfiles"
      :workspace/num       1}
   2 {:workspace/name      "clawe"
      :workspace/directory "~/russmatney/clawe"
      :workspace/num       2}
   3 {:workspace/name      "advent-of-code"
      :workspace/directory "~/russmatney/advent-of-code"
      :workspace/num       3}})

(def ix->wsp (->> workspaces
                  ;; (map (fn [wsp] [(:workspace/num wsp) wsp]))
                  ;; (into {})
                  ))

(defn current-workspace []
  (some->>
    (sway-msg! "-t get_workspaces --raw")
    (filter :sway/focused)
    first
    :sway/num
    ix->wsp))

(comment
  (sway-msg! "-t get_tree --raw")
  (sway-msg! "-t get_workspaces --raw")
  (current-workspace)
  )

(defn ensure-current-workspace [opts]
  (println "ensuring in current workspace" opts)
  (let [wsp (current-workspace)]
    (println wsp)
    (when (#{"emacs"} (:app opts))
      (ensure-workspace {:name (:workspace/name wsp)}))
    )
  )
