(ns clawe.mx-fast
  (:require
   [babashka.process :as process]

   [ralphie.rofi :as rofi]
   [ralphie.cache :as cache]
   [ralphie.config :as config]

   [timer :as timer]
   ))

(timer/print-since "clawe.mx-fast\tNamespace (and deps) Loaded")

(defn cmds-cache-file [cache-id]
  (cache/cache-file (str "mx-commands-" cache-id)))

(defn read-cache [{:keys [cache-id file]}]
  (when (or cache-id file)
    (let [file (or file (cmds-cache-file cache-id))]
      (or (slurp file) ""))))

(defn write-cache [{:keys [cache-id file content]}]
  (let [file (or file (cmds-cache-file cache-id))]
    (spit file content)))

(defn clear-cache [{:keys [cache-id]}]
  (cache/clear-file (cmds-cache-file cache-id)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mx-cache-file []
  (cmds-cache-file :mx-fast))
(defn read-mx-cache []
  (read-cache {:cache-id :mx-fast}))
(defn clear-mx-cache []
  (clear-cache {:cache-id :mx-fast}))
(defn write-mx-cache [rofi-input]
  (write-cache {:cache-id :mx-fast :content rofi-input}))

(defn mx-fast
  "Run rofi with commands created in `mx-commands-fast`."
  ([] (mx-fast nil))
  ([_]
   (timer/print-since "clawe.mx-fast/mx-fast\tstart")

   (let [cached-mx-rofi-input (read-mx-cache)]
     (if (seq cached-mx-rofi-input)
       (let [cmd (rofi/rofi
                   {:require-match? true
                    :msg            "Clawe commands (fast)"
                    :cache-id       "clawe-mx-fast"}
                   (mx-cache-file))]
         (when cmd
           (->
             (process/process
               {:dir (config/project-dir) :out :string :err :string}
               (str "bb -x clawe.mx/call-selected-label --label "
                    "\"" (:rofi/og-label cmd) "\""))
             process/check
             :out
             println)))

       ;; invoke proper mx-fast and cache rebuild
       (->
         (process/process
           {:dir (config/project-dir)}
           "bb -x clawe.mx/mx-fast")
         (process/check))))))

(comment
  (mx-fast))
