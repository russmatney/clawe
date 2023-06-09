(ns ralphie.notify
  (:require
   [babashka.process :as process :refer [$]]
   [clojure.string :as string]
   [ralphie.zsh :as zsh]))


(def osx? (boolean (string/includes? (zsh/expand "$OSTYPE") "darwin")))
(comment osx?)

;; osx notify ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn osx-notif-cmd [{:keys [subject body]}]
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

(defn linux-notif-cmd [{:keys [subject body notif-id]}]
  ;; TODO use notif-id to look up a cached proc-id, pass to -r
  (cond->
      ["notify-send.py" subject]
    body (conj body)

    ;; goes away
    notif-id
    (conj "--replaces-process" notif-id)))

;; notify ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO move all ralphie.notify/is-mac? usage to ralphie.config/osx?
;; TODO move all :notify/replaces-process usage to :notify/id
;; TODO refactor arity to emphasise the map usage

(defn notify
  "Create a notification.

  Attrs:
  - :notify/subject
  - :notify/body
  - :notify/id, :notify/replaces-process
  - :notify/print?
  "
  ([opts]
   (let [opts     (if (string? opts) {:notify/subject opts} opts)
         subject  (some opts [:subject :notify/subject])
         body     (some opts [:body :notify/body])
         notif-id (some opts [:id :notify/id])
         print?   (:notify/print? opts)
         exec-strs
         (if osx?
           (osx-notif-cmd {:subject subject :body body})
           (linux-notif-cmd {:subject subject :body body :notif-id notif-id}))

         _ (when print? (println subject (when body (str "\n" body))))]
     (try
       (cond->
           (process/process exec-strs {:out :string}) process/check

           (and notif-id (not osx?))
           ((fn [proc]
              (let [proc-id (:out proc)]
                ;; TODO write proc-id to cache with notif-id
                (println "todo: cache proc-id with notif-id")))))
       (catch Exception e
         (println e)
         (println "ERROR ralphie.notify/notify error.")
         (println "Tried to execute:" exec-strs)))
     nil)))


(comment
  (notify "subj")
  (notify {:subject "subj"})

  (some {:blah "nope" :notify/subject 3} [:notify/subject :subject])

  (notify {:subject "subj" :body {:value "v" :label "laaaa"}})
  (notify {:subject "subj" :body "BODY"})
  (notify {:notify/subject "subj" :notify/body "BODY"})
  (-> ($ notify-send subj body)
      process/check)

  (->
    ($ osascript -e "display notification \"Lorem ipsum dolor sit amet\" with title \"Title\"")
    process/check)

  (->
    (process/process ["osascript" "-e" "display notification \"Lorem ipsum\ndolor sit amet\" with title \"Title\""])
    process/check))
