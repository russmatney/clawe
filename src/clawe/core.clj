(ns clawe.core
  (:require
   [clawe.workspaces :as workspaces]
   [clawe.workspaces.create :as wsp.create]
   clawe.workrave
   clawe.defs.bindings
   clawe.defs.workspaces
   clawe.defs.local.workspaces
   clawe.doctor
   clawe.install
   [clawe.awesome :as awm]
   [clawe.awesome.restart]
   [clawe.awesome.rules]
   [ralphie.notify :as r.notify]
   [ralphie.rofi :as r.rofi]
   [ralph.defcom :as defcom :refer [defcom]]
   [ralphie.git :as r.git]))

(defcom hello-cmd
  {:defcom/name    "hello"
   :defcom/handler (fn [_config _parsed] (println "Howdy"))})

(defcom rofi-cmd
  {:defcom/name "rofi"
   :defcom/handler
   (fn [config parsed]
     (when-let [cmd (some->> (defcom/list-commands)
                             (map :name)
                             (r.rofi/rofi {:require-match? true
                                           :msg            "Clawe commands"}))]
       (defcom/call-handler cmd config parsed)))})

(defn dwim-commands
  ([] (dwim-commands nil))
  ([{:keys [wsp]}]
   (let [wsp (or wsp (workspaces/current-workspace))]
     (->>
       (concat
         [{:rofi/label     "Create Workspace Client"
           :rofi/on-select (fn [_]
                             ;; TODO detect if workspace client is already open
                             ;; wrap these nil-punning actions-list api
                             (r.notify/notify "Creating client for workspace")
                             (wsp.create/create-client wsp))}
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
           (map (partial r.rofi/defcom->rofi nil))))))))

(comment
  (->>
    (dwim-commands)
    (filter :defcom/name)
    (filter (comp #(re-seq #"key" %) :defcom/name))
    (first)
    :rofi/on-select
    ((fn [f] (f)))))

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

    (->> (dwim-commands {:wsp wsp})
         (r.rofi/rofi {:require-match? true
                       :msg            "Clawe commands"}))))

(defcom dwim-cmd
  {:defcom/name    "dwim"
   :defcom/handler (fn [_config _parsed] (dwim))})

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
