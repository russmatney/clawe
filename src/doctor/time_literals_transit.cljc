(ns doctor.time-literals-transit
  "Connect time-literals to transit.

  Sourced and edited from:
  https://gist.github.com/jjttjj/6bc0b62ef1dbf29c1c69ea22f8eb7f55

  "
  (:require
   [time-literals.read-write]
   [cognitect.transit :as transit]
   #?(:cljs [java.time :refer [Period
                               LocalDate
                               LocalDateTime
                               ZonedDateTime
                               Instant
                               ZoneId
                               DayOfWeek
                               LocalTime
                               Month
                               Duration
                               Year
                               YearMonth]]))
  #?(:clj (:import (java.io ByteArrayOutputStream ByteArrayInputStream)
                   (java.time Period
                              LocalDate
                              LocalDateTime
                              ZonedDateTime
                              Instant
                              ZoneId
                              DayOfWeek
                              LocalTime
                              Month
                              Duration
                              Year
                              YearMonth))))

(def time-classes
  {'period          Period
   'date            LocalDate
   'date-time       LocalDateTime
   'zoned-date-time ZonedDateTime
   'instant         Instant
   'time            LocalTime
   'duration        Duration
   'year            Year
   'year-month      YearMonth
   'zone            ZoneId
   'day-of-week     DayOfWeek
   'month           Month})

(def write-handlers
  (into {}
        (for [[tick-class host-class] time-classes]
          [host-class (transit/write-handler (constantly (name tick-class)) str)])))

(def read-handlers
  (into {} (for [[sym fun] time-literals.read-write/tags]
             [(name sym) (transit/read-handler fun)])))

(defn ->transit "Encode data structure to transit."
  [arg]
  #?(:clj (let [out    (ByteArrayOutputStream.)
                writer (transit/writer out :json {:handlers write-handlers})]
            (transit/write writer arg)
            (.toString out))
     :cljs (transit/write (transit/writer :json {:handlers write-handlers}) arg)))

(defn <-transit "Decode data structure from transit."
  [json]
  #?(:clj (try (let [in     (ByteArrayInputStream. (.getBytes json))
                     reader (transit/reader in :json {:handlers read-handlers})]
                 (transit/read reader))
               (catch Exception e
                 ;;(log/warn "Invalid message" json (:cause (Throwable->map e)))
                 :invalid-message))
     :cljs (transit/read (transit/reader :json {:handlers read-handlers}) json)))
                                        ; TODO catch js errors
