(ns ralphie.systemd
  (:require [babashka.process :as p]
            [clojure.string :as string]
            [ralphie.rofi :as rofi]
            [ralphie.tmux :as tmux]
            [defthing.defcom :refer [defcom]]))

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

(defn doctor-unit? [u]
  (comp #(string/includes? % "doctor") :name))

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
  (fn [_cmd & args]
    (restart-unit)))

(comment
  (defcom/exec systemctl-restart-unit)
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
