(ns api.topbar
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [ralphie.battery :as r.battery]
   [ralphie.pulseaudio :as r.pulseaudio]
   [ralphie.spotify :as r.spotify]
   [babashka.process :as process]
   [clojure.string :as string]
   [db.core :as db]
   [dates.tick :as dates.tick]
   [tick.core :as t]))

(declare push-topbar-metadata)

(def topbar-id #uuid "5d4a6099-4c5c-4c0d-843b-54c48c34caee")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; topbar timers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn active-timer []
  (some->>
    (db/query '[:find (pull ?e [*])
                :in $ ?topbar-id
                :where
                [(not (nil? ?started-at))]
                [?e :topbar/started-at ?started-at]
                [?e :topbar/id ?topbar-id]]
              topbar-id)
    first))

(defn start-timer []
  (db/transact {:topbar/started-at (t/inst (dates.tick/now))
                :topbar/id         topbar-id})
  (push-topbar-metadata))

(comment
  (active-timer)

  (start-timer)

  (db/query '[:find (pull ?e [*])
              :in $ ?topbar-id
              :where
              [?e :topbar/id ?topbar-id]]
            topbar-id))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; background toggle
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn background-mode []
  (some->>
    (db/query '[:find [?bg-mode]
                :in $ ?topbar-id
                :where
                [?e :topbar/background-mode ?bg-mode]
                [?e :topbar/id ?topbar-id]]
              topbar-id)
    first))

(defn set-background-mode [bg-mode]
  (db/transact {:topbar/background-mode bg-mode
                :topbar/id              topbar-id})
  (push-topbar-metadata))

(comment
  (set-background-mode :bg/dark)
  (set-background-mode :bg/light)
  (background-mode)

  (db/query '[:find (pull ?e [*])
              :in $ ?topbar-id
              :where
              [?e :topbar/id ?topbar-id]]
            topbar-id))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; build metadata
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn hostname []
  (-> (process/$ hostname) process/check :out slurp string/trim
      (string/replace ".local" "")))

(defn build-topbar-metadata []
  (->
    {:microphone/muted (try (r.pulseaudio/input-muted?) (catch Exception _e #_(println "topbar mute status error")))
     :spotify/volume   (try (r.spotify/spotify-volume-label) (catch Exception _e #_(println "topbar spotify volume error")))
     :audio/volume     (try (r.pulseaudio/default-sink-volume-label) (catch Exception _e #_(println "topbar volume error")))
     :hostname         (hostname)}
    (merge #_(r.spotify/spotify-current-song)
           (try (r.battery/info) (catch Exception _e #_(println "topbar battery info error"))))
    (dissoc :spotify/album-url :spotify/album)
    (assoc :topbar/background-mode (background-mode))))

(comment
  (build-topbar-metadata))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; push data to frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsys ^:dynamic *topbar-metadata-stream*
  :start (s/stream)
  :stop (s/close! *topbar-metadata-stream*))

(comment
  (sys/start! `*topbar-metadata-stream*))

(defn push-topbar-metadata []
  (s/put! *topbar-metadata-stream* (build-topbar-metadata)))

(comment
  (->>
    (build-topbar-metadata)
    (sort-by :workspace/index)
    first)

  (push-topbar-metadata))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; subscribe to i3
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defsys ^:dynamic *i3-subscribe-stream*
;;   :start (s/stream)
;;   :stop (s/close! *i3-subscribe-stream*))

;; (comment
;;   (sys/start! `*i3-subscribe-stream*))
