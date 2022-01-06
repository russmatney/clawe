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
