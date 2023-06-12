(ns ralphie.notify
  (:require
   [babashka.process :as process :refer [$]]
   [ralphie.config :as config]
   [ralphie.cache :as cache]
   [clojure.edn :as edn]
   [clojure.string :as string]))

;; id/replaces-process cache ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn process-cache []
  (cache/cache-file "notify-process-cache.edn"))

(defn read-process-cache
  ([] (read-process-cache nil))
  ([f]
   (let [file (or f (process-cache))
         raw  (slurp file)]
     (or (edn/read-string raw) {}))))

(defn existing-id [notify-id]
  (let [file  (process-cache)
        cache (read-process-cache file)]
    (get cache notify-id)))

(defn write-proc-id [proc-id notify-id]
  (when (int? proc-id)
    (let [file  (process-cache)
          cache (read-process-cache file)
          cache (assoc cache notify-id proc-id)]
      (spit file cache))))

(comment
  (existing-id "my-notif")
  (write-proc-id 9 "my-notif"))

;; osx notify ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn osx-notif-cmd [{:keys [subject body]}]
  ;; TODO support similar replaces-id feature (tho osx more or less does this already)
  ["osascript" "-e" (str "display notification \""
                         (cond
                           (string? body) body
                           ;; TODO escape stringified bodies for osascript's standards
                           (not body)     "no body"
                           :else          "unsupported body")
                         "\""
                         (when subject
                           (str " with title \"" subject "\"")))])

;; linux notify ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn linux-notif-cmd [{:keys [subject body notify-id]}]
  (let [proc-id (when notify-id (existing-id notify-id))]
    (cond-> ["notify-send" subject]
      body      (conj body)
      ;; replace process when one is found in the cache
      proc-id   (conj "-r" proc-id)
      ;; print proc-id when notify-id passed
      notify-id (conj "-p"))))

;; notify ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn notify
  "Create a notification. Linux depends on `notify-send`, osx on `osascript`.

  Non-string OSX bodies are not yet escaped, so they may be ignored or crash the notif.

  This function should never crash/stop execution.

  Attrs:
  - :notify/subject
  - :notify/body
  - :notify/id - an id used to replace an existing notification, rather than create a new one.
  - :notify/print? - also print the subject/body of the notification
  "
  ([subject body] (notify {:notify/subject subject :notify/body body}))
  ([subject body opts] (notify (merge {:notify/subject subject :notify/body body} opts)))
  ([opts]
   (let [opts      (if (string? opts) {:notify/subject opts} opts)
         subject   (some opts [:subject :notify/subject])
         body      (some opts [:body :notify/body])
         notify-id (some opts [:id :notify/id])
         print?    (some opts [:print? :notify/print?])
         cmd-str
         (if (config/osx?)
           (osx-notif-cmd {:subject subject :body body})
           (linux-notif-cmd {:subject subject :body body :notify-id notify-id}))

         _ (when print? (println subject (when body (str "\n" body))))]
     (try
       (cond-> (process/process cmd-str {:out :string})
         true process/check
         (and notify-id (not (config/osx?)))
         (#(-> % :out string/trim read-string (write-proc-id notify-id))))
       (catch Exception e
         (println e)
         (println "ERROR ralphie.notify/notify error.")
         (println "Tried to execute:" cmd-str)))
     nil)))


(comment
  (notify "subj")
  (notify {:subject "subj"})

  (some {:blah "nope" :notify/subject 3} [:notify/subject :subject])

  (notify {:subject "subj" :body {:value "v" :label "laaaa"}})
  (notify {:subject "subj" :body "BODY"})
  (notify {:notify/subject "subj" :notify/body "BODY"})
  (notify {:notify/subject "subj" :notify/body "BYdddD"
           :notify/id      "replace-me"})

  (-> ($ notify-send subj body)
      process/check)

  (-> ($ osascript -e "display notification \"Lorem ipsum dolor sit amet\" with title \"Title\"")
      process/check)

  (-> (process/process ["osascript" "-e" "display notification \"Lorem ipsum\ndolor sit amet\" with title \"Title\""])
      process/check))
