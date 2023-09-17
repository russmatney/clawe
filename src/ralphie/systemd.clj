(ns ralphie.systemd
  (:require [babashka.process :as p]
            [clojure.string :as string]
            [ralphie.rofi :as rofi]
            [ralphie.tmux :as tmux]
            [defthing.defcom :refer [defcom] :as defcom]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn line->unit [s]
  (-> s
      string/trim
      (string/replace #"\s\s+" " ")
      (string/split #" ")
      ((fn [[name loaded active sub & rest]]
         {:name        (when name name)
          :loaded      loaded
          :active      active
          :sub         sub
          :description (when (seq rest) (string/join " " rest))}))))

(comment
  (line->unit "")
  )

(defn unit->rofi [u]
  (-> u
      (assoc :rofi/label (:name u))
      (assoc :rofi/description (:description u))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-units-raw []
  (->
    ^{:out :string}
    (p/$ systemctl --user list-units)
    p/check
    :out
    string/split-lines))

(defn list-units []
  (->>
    (list-units-raw)
    (map line->unit)))

(defn select-unit
  ([] (select-unit nil))
  ([msg]
   (->>
     (list-units)
     (map unit->rofi)
     (rofi/rofi {:msg (or msg "Which unit?")}))))

(defn restart-unit
  ([] (restart-unit nil))
  ([u]
   (when-let [u (or u (select-unit))]
     (tmux/fire (str "systemctl --user restart " (:name u))))))

(defcom systemctl-restart-unit
  "Restart a systemctl --user unit."
  (fn [_cmd & _args]
    (restart-unit)))

(comment
  ;; (defcom/exec systemctl-restart-unit)
  )

(defn list-service-units []
  (->>
    (list-units)
    (filter (comp #{"active"} :active))
    (filter (comp (fn [x]
                    (re-seq #"(\.service)" x)
                    ) :name))
    (remove (comp (fn [x]
                    (re-seq #"(sys|\.mount|\.socket|\.target|\.slice|\.swap)" x)
                    ) :name))))

(def systemd-operations
  {:systemd/restart "restart"
   :systemd/stop    "stop"
   :systemd/start   "start"})

(defn rofi-service-opts [->label operation]
  (if-let [op (systemd-operations operation)]
    (->> (list-service-units)
         (map (fn [u]
                {:rofi/label       (-> u :name ->label)
                 ;; TODO support tags for rofi search
                 :rofi/description "#systemd #sc"
                 :rofi/on-select
                 (fn [_]
                   (tmux/fire {:tmux.fire/cmd
                               (str "systemctl --user " op " " (:name u))}))})))
    (println (str "unsupported systemd operation: " operation))))

(comment
  (rofi-service-opts #(str "Restart " %) :systemd/restart)
  (rofi-service-opts #(str "Stop " %) :systemd/stop)
  (rofi-service-opts #(str "Start " %) :systemd/start)
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (->
    ^{:out :string}
    (p/$ systemctl --user list-units)
    p/check
    :out
    string/split-lines
    (->>
      (filter #(string/includes? % "doctor"))
      (map (comp #(string/split % #" ") #(string/replace % #"\s\s+" " ") string/trim))
      (map (fn [[name loaded active running & rest]]
             {:name        name
              :loaded      loaded
              :active      active
              :running     running
              :description (string/join " " rest)}))))
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
