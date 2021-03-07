(ns clawe.install
  (:require
   [babashka.process :refer [$ check]]
   [clojure.java.shell :as sh :refer [with-sh-dir]]

   [ralphie.install :as r.install]
   [ralphie.util :as r.util]
   [ralphie.notify :as r.notify]
   [ralphie.sh :as r.sh]
   [ralphie.config :as r.config]
   [ralphie.rofi :as r.rofi]

   [ralph.defcom :as defcom :refer [defcom]]))

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
;; Mini-uberscripts
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO dry up into reusable util (this was ported from ralphie)

(defn temp-ns-path
  [_cmd] "/home/russ/russmatney/clawe/src/clawe/temp.clj")

(defn project-dir
  [] "/home/russ/russmatney/clawe")

(defn temp-uberscript-path
  ([cmd]
   (temp-uberscript-path cmd :abs))
  ([cmd abs-or-rel]
   (str
     (when (= :abs abs-or-rel)
       (str (project-dir) "/"))
     "uberscripts/" (:defcom/name cmd) ".clj")))

(defn command-bin-path [cmd]
  (str (r.config/local-bin-dir) "/" "clawe-" (:defcom/name cmd)))

(defn write-temp-main-ns [cmd]
  (r.notify/notify "Writing temp ns" (:defcom/name cmd))
  (spit (temp-ns-path cmd)
        (str "(ns clawe.temp (:require ["
             (:ns cmd) "])) "
             "(defn -main [& args] ("
             ;; call str on the fn-name keyword to drop the `:`
             (:defcom/handler-name cmd)
             " nil {:arguments args}))"))
  (r.notify/notify "Wrote temp ns" (:defcom/name cmd)))

(comment
  (apply str (next (str :some.name/space))))

(defn create-temp-uberscript [cmd]
  (r.notify/notify "Creating temp uberscript" (:defcom/name cmd))
  (-> ^{:dir (project-dir)}
      ($ bb -cp ~(r.util/get-cp (project-dir))
         -m clawe.temp
         --uberscript (temp-uberscript-path cmd))
      check
      :out
      slurp)
  (r.notify/notify "Created temp uberscript" (:defcom/name cmd)))

(defn carve-temp-uberscript [cmd]
  (r.notify/notify "Carving temp uberscript" (:defcom/name cmd))
  (let [opts {:paths            [(temp-uberscript-path cmd :relative)]
              :aggressive       true
              :clj-kondo/config {:skip-comments true}}]
    (with-sh-dir (project-dir)
      (sh/sh "clj" "-M:carve" "--opts" (str opts))))
  ;; (-> ^{:dir }
  ;;     ($ clj -M:carve --opts ~opts)
  ;;     check :out slurp)
  (r.notify/notify "Carved temp uberscript" (:defcom/name cmd)))

(defn install-temp-uberscript [cmd]
  (spit (command-bin-path cmd)
        (str "#!/bin/sh
exec bb " (temp-uberscript-path cmd) " $@"))
  ($ chmod +x (command-bin-path cmd))
  (r.notify/notify "Created wrapper script" (:defcom/name cmd)))

(defn select-command []
  (r.rofi/rofi
    {:msg "Select command to install"}
    (->> (defcom/list-commands)
         (map (partial r.rofi/defcom->rofi nil))
         ;; remove callbacks so these don't actually run
         ;; TODO probably no need to attach these callbacks
         ;; in the first place
         (map #(dissoc % :rofi/on-select)))))

(comment
  (select-command))

(defn install-micro-handler
  ([] (install-micro-handler nil nil))
  ([config {:keys [arguments]}]
   (let [cmd (some-> arguments
                     first
                     (#(defcom/find-command (:commands config) %)))
         cmd (or cmd (select-command))]
     (if cmd
       (do
         (r.notify/notify (str "Installing clawe micro handler for: " (:defcom/name cmd)) cmd)
         (try
           ;; write dummy file with -main fn calling command's handler
           (write-temp-main-ns cmd)
           ;; create uberscript for new-file's namespace
           (create-temp-uberscript cmd)
           ;; carve file
           ;; TODO restore this - for some reason
           ;; carve is failing on me
           ;; (carve-temp-uberscript cmd)
           ;; install bash wrapper to local/bin
           (install-temp-uberscript cmd)
           (catch Exception e
             (r.notify/notify "Error!" e)
             (println e)))
         (println "Clawe micro installation complete")
         (r.notify/notify "Clawe micro installation complete"))
       (r.notify/notify (str "No command selected for clawe micro installation"))))))

(defcom install-micro-cmd
  {:defcom/name    "install-clawe-micro"
   :defcom/handler install-micro-handler})
