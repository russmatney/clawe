(ns clawe.workrave
  (:require
   [babashka.process :refer [$ check]]
   [clojure.string :as string]
   [ralphie.awesome :as awm]
   [defthing.defcom :refer [defcom] :as defcom]))

;; dbus-send --print-reply \
;; --dest=org.workrave.Workrave \
;; "/org/workrave/Workrave/Core" \
;; org.workrave.CoreInterface.GetTimerRemaining "string:restbreak";

(defn dbus-arg [arg]
  (cond
    (string? arg) (str "string:" arg)))

(defn workrave-dbus [cmd arg]
  (let [dest "org.workrave.Workrave"
        path "/org/workrave/Workrave/Core"
        intf "org.workrave.CoreInterface"]
    (->
      ^{:out :string}
      ($ dbus-send --print-reply ~(str "--dest=" dest) ~path
         ~(str intf "." cmd)
         ~(dbus-arg arg))
      check
      :out
      (string/split-lines)
      (second)
      (->
        (string/trim)
        (string/split #" ")
        second
        read-string))))

(comment
  (workrave-dbus "GetTimerRemaining" "restbreak")
  (workrave-dbus "GetTimerRemaining" "microbreak")
  (int
    (/ 0
       60))
  (int
    (/ (workrave-dbus "GetTimerRemaining" "microbreak")
       60)))

(defn update-workrave-widget [k]
  (case k
    :workrave/micro
    (let [secs (workrave-dbus "GetTimerRemaining" "microbreak")
          mins (int (/ secs 60))]
      (awm/fnl (update_micro_break ~mins)))
    :workrave/rest
    (let [secs (workrave-dbus "GetTimerRemaining" "restbreak")
          mins (int (/ secs 60))]
      (awm/fnl (update_rest_break ~mins)))))

(defcom update-rest-break
  (update-workrave-widget :workrave/rest))
(defcom update-micro-break
  (update-workrave-widget :workrave/micro))
