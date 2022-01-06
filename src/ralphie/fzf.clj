(ns ralphie.fzf
  (:require
   [defthing.defcom :refer [defcom] :as defcom]
   [babashka.process :refer [process]]
   [clojure.string :as string]))

(defn fzf [xs]
  (let [labels         (->> xs (map :fzf/label))
        proc           (process ["fzf"]
                                {:in  (string/join "\n" labels)
                                 :err :inherit
                                 :out :string})
        selected-label (-> @proc :out string/trim)]
    (when (seq selected-label)
      (some->> xs
               (filter (comp #{selected-label} :fzf/label))
               first))))

(defcom fzf-cmd
  (fn [_cmd args]
    (when-let [cmd (->> (defcom/list-commands)
                        (filter (comp seq :name))
                        (map (fn [cmd] (assoc cmd :fzf/label (:name cmd))))
                        fzf)]
      (defcom/exec cmd (rest args)))))
