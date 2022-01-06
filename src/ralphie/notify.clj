(ns ralphie.notify
  (:require
   [babashka.process :as process :refer [$ check]]))

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
         exec-strs        (cond-> ["notify-send.py" subject]
                            body (conj body)
                            replaces-process
                            (conj "--replaces-process" replaces-process))
         _                (when print?
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

  (notify {:subject "subj" :body {:value "v" :label "laaaa"}})
  (notify {:subject "subj" :body "BODY"})
  (-> ($ notify-send subj body)
      check)
  )
