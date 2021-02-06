(ns clawe.core
  (:require
   [clawe.workspaces :as workspaces]
   [clawe.workrave]
   [clawe.yodo]
   [clawe.awesome :as awm]
   [babashka.process :refer [$ check]]

   [ralphie.install :as r.install]
   [ralphie.util :as r.util]
   [ralphie.notify :as r.notify]
   [ralphie.sh :as r.sh]
   [ralphie.rofi :as r.rofi]
   [ralphie.scratchpad :as r.scratchpad]

   [ralph.defcom :as defcom :refer [defcom]]
   [ralphie.git :as r.git]))

(defcom hello-cmd
  {:defcom/name    "hello"
   :defcom/handler (fn [_config _parsed]
                     (println "Howdy"))})

(defcom rofi-cmd
  {:defcom/name "rofi"
   :defcom/handler
   (fn [config parsed]
     (when-let [cmd (some->> (defcom/list-commands)
                             (map :name)
                             (r.rofi/rofi {:require-match? true
                                           :msg            "Clawe commands"}))]
       (defcom/call-handler cmd config parsed)))})

(defn dwim []
  (let [wsp (workspaces/current-workspace)]

    ;; Notify with git status
    (when (r.git/repo? (workspaces/workspace-repo wsp))
      (r.notify/notify (str "Git Status: " (workspaces/workspace-repo wsp))
                       (->>
                         (r.git/status (workspaces/workspace-repo wsp))
                         (filter second)
                         (map first)
                         seq)))

    (some->>
      (concat
        [{:rofi/label     "Create Workspace Client"
          :rofi/on-select (fn [_]
                            ;; TODO detect if workspace client is already open
                            ;; wrap these nil-punning actions-list api
                            (r.notify/notify "Creating client for workspace")
                            (r.scratchpad/create-client wsp))}
         {:rofi/label     "Suggest more things here! <small> but don't get distracted </small>"
          :rofi/on-select (fn [_] (r.notify/notify "A quick doctor checkup?"
                                                   "Or the time of day?"))}

         (when (r.git/repo? (workspaces/workspace-repo wsp))
           {:rofi/label     "Fetch repo upstream"
            :rofi/on-select (fn [_]
                              (r.notify/notify "Fetching upstream for workspace")
                              ;; TODO support fetching via ssh-agent
                              (r.git/fetch (workspaces/workspace-repo wsp)))})]
        (->>
          (defcom/list-commands)
          (map (partial r.rofi/defcom->rofi nil))))
      (r.rofi/rofi {:require-match? true
                    :msg            "Clawe commands"}))))

(defcom dwim-cmd
  {:defcom/name    "dwim"
   :defcom/handler (fn [_config _parsed] (dwim))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Install Awesome config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom install-cmd
  {:defcom/name "install"
   :defcom/handler
   (fn [_config _parsed]
     (println "Symlinking repo/awesome to your ~/.config/awesome")
     (r.install/symlink
       (r.sh/expand "~/russmatney/clawe/awesome")
       ;; TODO use ~WDG_CONFIG~ or whatever that thing is
       (r.sh/expand "~/.config/awesome")))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Build Clawe Uberscript
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-uberscript []
  (r.notify/notify "Re-Building Clawe Uberscript")
  (let [cp (r.util/get-cp (r.sh/expand "~/russmatney/clawe"))]
    (->
      ^{:dir (r.sh/expand "~/russmatney/clawe")}
      ($ bb -cp ~cp -m clawe.core --uberscript clawe-script.clj)
      check)
    (r.notify/notify "Clawe Uberscript Rebuilt.")))

(defcom build-clawe
  {:defcom/name    "rebuild-clawe"
   :defcom/handler (fn [_config _parsed] (build-uberscript))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; awm-cli wrapper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom awm-cli-cmd
  {:defcom/name "awm-cli"
   ;; this doesn't do anything, but felt easier than documentation....
   :validate    (fn [arguments] (-> arguments first string?))
   :defcom/handler
   (fn [_config {:keys [arguments]}]
     (let [res (awm/awm-cli (first arguments))]
       (println res)))})

(defcom collect-garbage
  {:defcom/name    "awm-collect-garbage"
   :defcom/handler (fn [_ _] (awm/awm-cli "handle_garbage();"))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; main
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main [& args]
  (println "[CLAWE] start" args)
  (let [start-time (System/currentTimeMillis)
        res        (apply defcom/run args)
        dt         (- (System/currentTimeMillis) start-time)]
    (println "[CLAWE] complete" args "in" dt "ms")
    res))
