(ns ralphie.install
  (:require
   [babashka.process :refer [$ check]]
   [defthing.defcom :refer [defcom] :as defcom]
   [ralphie.notify :as notify]
   [ralphie.config :as config]
   [ralphie.util :as util]
   [clojure.string :as string]))

(defn symlink
  [source target]
  (-> ($ ln -s ~source ~target)
      check))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Install zsh completion
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn zsh-completion-path [bin-name]
  (str (config/home-dir) "/.zsh/completion/_" bin-name))

(defn install-zsh-completion
  "Creates and installas a zsh-completion file for all required `defcom`s,
  at the supplied bin-name. (ex. `ralphie` or `clawe`)."
  [bin-name]
  (let [cmds-string
        (->> (defcom/list-commands)
             (map :name)
             (string/join " \\
"))
        completion-file (str "#compdef _" bin-name " " bin-name "
_arguments -C \\
  \"1: :(
" cmds-string ")\"")]
    (spit (zsh-completion-path bin-name) completion-file)))

(defcom install-ralphie-zsh-completion
  (install-zsh-completion "ralphie"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Build uberscript
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom build-uberjar
  (let [proc "rebuilding-ralphie-uberjar"]
    (notify/notify {:subject          "Ralphie Uberjar: Rebuilding"
                    :replaces-process proc})
    (let [cp (util/get-cp (config/project-dir))]
      (->
        ^{:dir (config/project-dir)}
        ($ bb -cp ~cp --uberjar ralphie.jar -m ralphie.core )
        check)
      (notify/notify {:subject          "Ralphie Uberjar: Rebuild Complete"
                      :replaces-process proc}))))
