(ns ralphie.notify
  (:require
   [babashka.process :as process :refer [$ check]]
   [ralphie.zsh :as zsh]))


(def is-mac? (boolean (#{"darwin21"} (zsh/expand "$OSTYPE"))))

(comment
  is-mac?)

(defn notify
  ([notice]
   (cond (string? notice) (notify notice nil)

         (map? notice)
         (let [subject (some notice [:subject :notify/subject])
               body    (some notice [:body :notify/body])]
           (notify subject body notice))

         :else
         (notify "Malformed ralphie.notify/notify call"
                 "Expected string or map.")))
  ([subject body & args]
   (let [opts             (or (some-> args first) {})
         print?           (:notify/print? opts)
         replaces-process (some opts [:notify/id :replaces-process :notify/replaces-process])
         exec-strs
         (if is-mac?
           ["osascript" "-e" (str "display notification \""
                                  (cond
                                    (string? body) body
                                    ;; TODO escape stringified bodies for osascript's standards
                                    (not body)     "no body"
                                    :else          "unsupported body")
                                  "\""
                                  (when subject
                                    (str " with title \"" subject "\"")))]
           (cond->
               ["notify-send.py" subject]
             body (conj body)
             replaces-process
             (conj "--replaces-process" replaces-process)))
         ;; _
         ;; (println exec-strs)
         _                (when print?
                            ;; TODO use dynamic global bool to print all notifs
                            (println subject (when body (str "\n" body))))
         proc             (process/process (conj exec-strs) {:out :string})]

     ;; we only check when --replaces-process is not passed
     ;; ... skips error messages if bad data is passed
     ;; ... also not sure when these get dealt with. is this a memory leak?
     (when-not replaces-process
       (-> proc check :out))
     nil)))


(comment
  (notify "subj" "body\nbody\nbody")
  (notify {:subject "subj"})
  (notify "subj" "body\nbodybodddd" {:replaces-process "blah"})

  (some {:blah "nope" :notify/subject 3} [:notify/subject :subject])

  (notify "nice")

  (notify {:subject "subj" :body {:value "v" :label "laaaa"}})
  (notify {:subject "subj" :body "BODY"})
  (-> ($ notify-send subj body)
      check)

  (->
    ^{:out :string}
    ($ echo $OSTYPE)
    check
    :out)


  (->
    ($ osascript -e "display notification \"Lorem ipsum dolor sit amet\" with title \"Title\"")
    check
    )

  (->
    (process/process ["osascript" "-e" "display notification \"Lorem ipsum\ndolor sit amet\" with title \"Title\""])
    check
    )

  )
