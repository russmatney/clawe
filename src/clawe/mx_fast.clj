(ns clawe.mx-fast
  (:require
   [clojure.edn :as edn]
   [babashka.process :as process]

   [ralphie.rofi :as rofi]
   [ralphie.cache :as cache]
   [ralphie.config :as config]

   [timer :as timer]
   ))

(timer/print-since "clawe.mx-fast\tNamespace (and deps) Loaded")

(defn cmds-cache-file [cache-id]
  (cache/cache-file (str "mx-commands-" cache-id ".edn")))

(defn read-cache [{:keys [cache-id file]}]
  (when (or cache-id file)
    (let [file (or file (cmds-cache-file cache-id))]
      (or (edn/read-string (slurp file)) []))))

(defn write-cache [{:keys [cache-id file commands]}]
  (let [file (or file (cmds-cache-file cache-id))]
    (spit file (seq commands))))

(defn clear-cache [{:keys [cache-id]}]
  (cache/clear-file (cmds-cache-file cache-id)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn read-mx-cache []
  (read-cache {:cache-id :mx-fast}))
(defn clear-mx-cache []
  (clear-cache {:cache-id :mx-fast}))
(defn write-mx-cache [commands]
  (write-cache {:cache-id :mx-fast :commands commands}))

(defn mx-fast
  "Run rofi with commands created in `mx-commands-fast`."
  ([] (mx-fast nil))
  ([_]
   (timer/print-since "clawe.mx-fast/mx-fast\tstart")

   (let [cached-mx-rofi-commands (read-mx-cache)]
     (if (seq cached-mx-rofi-commands)
       (do
         (let [cmd (->> cached-mx-rofi-commands
                        (map #(assoc % :rofi/og-label (:rofi/label %)))
                        (rofi/rofi
                          {:require-match? true
                           :msg            "Clawe commands (fast)"
                           :cache-id       "clawe-mx-fast"}))]
           (when cmd
             (->
               (process/process
                 {:dir (config/project-dir) :out :string :err :string}
                 (str "bb -x clawe.mx/call-selected-label --label "
                      "\"" (:rofi/og-label cmd) "\""))
               process/check
               :out
               println))))

       (do
         (->
           (process/process
             {:dir (config/project-dir)}
             "bb -x clawe.mx/mx-fast")
           (process/check)))))))

(comment
  (mx-fast))
