(ns ralphie.pulseaudio
  (:require
   [babashka.process :as process]
   [clojure.string :as string]
   [clojure.edn :as edn]))


;; https://askubuntu.com/questions/878853/change-spotify-volume-with-dbus
;; #!/bin/bash
;; spotify=$(pacmd list-sink-inputs | tr '\n' '\r' | perl -pe 's/ *index: ([0-9]+).+?application\.process\.binary = "([^\r]+)"\r.+?(?=index:|$)/\2 : \1\r/g' | tr '\r' '\n' | awk '/spotify/{print $3}')
;; step=2
;; pactl set-sink-input-volume $spotify +${step}%

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->sink-input-props
  "Parses the properties: section of `pacmd list-sink-inputs`.
  Assumes it is passed a list of trimmed strs,
  with the properties: section as the tail."
  [strs]
  (->>
    strs
    (partition-by #(re-seq #"properties:" %))
    last
    (map (fn [s]
           (-> s
               (string/replace " =" "")
               (#(str ":" %)))))
    seq
    (string/join " ")
    (#(str "{" % "}"))
    edn/read-string))

(defn ->sink-data
  "Parses the non-properties section of `pacmd list-sink-inputs`.
  "
  [strs]
  (->>
    strs
    (partition-by #(re-seq #"properties:" %))
    first
    ;; here we drop lines without colons,
    ;; then parse into key/values using the first colon found
    (keep #(when (re-seq #":" %)
             (string/split % #": " 2)))
    (map (fn [[k v]]
           [(-> k
                (string/replace " " "-")
                keyword)
            v]))
    (into {})))

(defn ->sink-input [strs]
  (merge
    (->sink-data strs)
    {:properties (->sink-input-props strs)}))

(defn pacmd-sink-inputs []
  (->
    ^{:out :string}
    (process/$ pacmd list-sink-inputs)
    (process/check)
    :out
    (string/split #"\n")
    (->>
      (drop 1) ;; drop summary line
      (map string/trim)
      (partition-by #(re-seq #"^index:" %))
      (partition 2 2)
      (map (comp ->sink-input flatten)))))

(comment
  (pacmd-sink-inputs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pactl sinks
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-sink
  "Very basic parse of a few lines of the 'pactl list sinks' output"
  [lines]
  (->> lines
       (map string/trim)
       (keep #(when (re-seq #":" %)
                (string/split % #": " 2)))
       (map (fn [[k v]]
              [(-> k
                   (string/replace " " "-")
                   keyword)
               v]))
       (into {})))

(defn pactl-list-sinks
  []
  (->
    ^{:out :string}
    (process/$ pactl list sinks)
    (process/check)
    :out
    (string/split #"\n")
    (->>
      (partition-by #{""})
      (remove (comp #{""} first))
      (map parse-sink))))

(defn default-sink-volume
  "Returns the volume of the _last_ sink in `pactl list sinks`.
  TODO pick a sink by card/something other than order.
  "
  []
  (some->> (pactl-list-sinks)
           last
           :Volume))

(defn default-sink-volume-label []
  (some->> (default-sink-volume) (re-seq #"\d?\d\d%") first))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; pactl set-sink-input-volume $spotify +${step}%
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pactl-set-sink-input-volume
  "Updates a passed sinks volume with the indicated value.
  Expects to be handed a sink parsed by `pacmd-sink-inputs`.

  Volume-str should be something like +5, -10, where the int corresponds to a
  percentage increase or decrease."
  [{:keys [index]} volume-str]
  (->
    ^{:out :string}
    (process/$ pactl set-sink-input-volume ~index ~(str volume-str "%"))
    (process/check)
    :out))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn input-muted?
  "Returns true if the input microphone is muted."
  []
  (let [capture-output (-> ^{:out :string}
                           (process/$ amixer get Capture)
                           process/check :out)
        output-lines   (string/split capture-output #"\n")
        output-lines   (some->>
                         output-lines
                         (map string/trim)
                         (filter #(or
                                    (string/includes? % "[on]")
                                    (string/includes? % "[off]"))))]
    (some->
      output-lines
      first
      (string/includes? "[off]"))))
