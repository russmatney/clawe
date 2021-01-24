(ns clawe.core
  (:require
   [clawe.workspaces :as workspaces]
   [clawe.workrave]
   [babashka.process :refer [$ check]]

   [ralphie.install :as r.install]
   [ralphie.util :as r.util]
   [ralphie.notify :as r.notify]
   [ralphie.sh :as r.sh]
   [ralphie.rofi :as r.rofi]
   [ralphie.scratchpad :as r.scratchpad]

   [ralph.defcom :as defcom :refer [defcom]]
   [clawe.awesome :as awm]
   [ralphie.notify :as notify]))

(defcom hello-cmd
  {:name    "hello"
   :handler (fn [_config _parsed]
              (println "Howdy"))})

(defcom rofi-cmd
  {:name "rofi"
   :handler
   (fn [config parsed]
     (when-let [cmd (some->> (defcom/list-commands)
                             (map :name)
                             (r.rofi/rofi {:require-match? true
                                           :msg            "Clawe commands"}))]
       (defcom/call-handler cmd config parsed)))})

(defcom dwim-cmd
  {:name "dwim"
   :handler
   (fn [_config parsed]
     (some->>
       (concat
         [{:rofi/label     "Create Workspace Client"
           :rofi/on-select (fn [_]
                             ;; TODO detect if workspace client is already open
                             ;; wrap these nil-punning actions-list api
                             (notify/notify "Creating client for workspace")
                             (r.scratchpad/create-client (workspaces/current-workspace)))}
          {:rofi/label     "Suggest more things here! <small> but don't get distracted </small>"
           ;; TODO fix this arity thing
           :rofi/on-select (fn [_] (notify/notify
                                     "A quick doctor checkup?"
                                     "Or the time of day?"))}

          {:rofi/label     "Some Label"
           :rofi/on-select (fn [_] (notify/notify "Some Action"))}]
         (->>
           (defcom/list-commands)
           (map (partial r.rofi/defcom->rofi parsed))))
       (r.rofi/rofi {:require-match? true
                     :msg            "Clawe commands"})
       ))})

(comment
  ;; TODO fill in missing arities for defcom functions
  ;; and clean up this 2 arg bullshit (config/parsed/ugh)
  (dwim-cmd nil nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Install Awesome config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom install-cmd
  {:name "install"
   :handler
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
  {:name    "rebuild-clawe"
   :handler (fn [_config _parsed] (build-uberscript))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; awm-cli wrapper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom awm-cli-cmd
  {:name     "awm-cli"
   ;; this doesn't do anything, but felt easier than documentation....
   :validate (fn [arguments] (-> arguments first string?))
   :handler
   (fn [_config {:keys [arguments]}]
     (let [res (awm/awm-cli (first arguments))]
       (println res)))})

(defcom collect-garbage
  {:name    "awm-collect-garbage"
   :handler (fn [_ _] (awm/awm-cli "handle_garbage();"))})

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
