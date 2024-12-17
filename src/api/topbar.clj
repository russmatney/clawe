(ns api.topbar
  (:require
   [babashka.process :as process]
   [clojure.string :as string]
   [manifold.stream :as s]
   [systemic.core :refer [defsys] :as sys]
   [taoensso.telemere :as log]
   [tick.core :as t]

   [dates.tick :as dates.tick]
   [db.core :as db]
   [ralphie.battery :as r.battery]
   [ralphie.pulseaudio :as r.pulseaudio]
   [ralphie.spotify :as r.spotify]))

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
      (string/replace ".local" "")
      (string/replace ".nyc.rr.com" "")))

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
  :start (s/stream 1)
  :stop (s/close! *topbar-metadata-stream*))

(comment
  (sys/start! `*topbar-metadata-stream*))

(def last-val (atom nil))

(defn push-topbar-metadata []
  (let [topbar-data (build-topbar-metadata)]
    (when (= @last-val topbar-data)
      (reset! last-val topbar-data)
      (let [res (s/try-put! *topbar-metadata-stream* (build-topbar-metadata) 0)]
        ;; (if @res
        ;;   (log/log! {:level :debug} "pushing topbar metadata")
        ;;   (log/log! {:level :debug} "dropping topbar update"))
        res))))

(comment
  (->>
    (build-topbar-metadata)
    (sort-by :workspace/index)
    first)
  (def st (s/sliding-stream 1 (s/stream)))
  (s/put! st "hi")
  (s/take! st)

  (sys/start!)

  (push-topbar-metadata)
  (->>
    (repeatedly push-topbar-metadata)
    (take 5))
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; subscribe to i3
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defsys ^:dynamic *i3-subscribe-stream*
;;   :start (s/stream)
;;   :stop (s/close! *i3-subscribe-stream*))

;; (comment
;;   (sys/start! `*i3-subscribe-stream*))
