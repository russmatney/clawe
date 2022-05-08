(ns api.topbar
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [ralphie.battery :as r.battery]
   [ralphie.pulseaudio :as r.pulseaudio]
   [ralphie.spotify :as r.spotify]
   [babashka.process :as process]
   [clojure.string :as string]
   [defthing.db :as db]))

(declare update-topbar-metadata)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; background toggle
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def topbar-id #uuid "5d4a6099-4c5c-4c0d-843b-54c48c34caee")

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
  (update-topbar-metadata))

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
;; todos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn in-progress-todos []
  (some->>
    (db/query
      '[:find (pull ?e [*])
        :where
        [?e :todo/status :status/in-progress]])
    first))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; build metadata
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-topbar-metadata []
  (let [todos  (in-progress-todos)
        latest (some->> todos (sort-by :todo/last-started-at) reverse first)]
    (->
      {:microphone/muted (r.pulseaudio/input-muted?)
       :spotify/volume   (r.spotify/spotify-volume-label)
       :audio/volume     (r.pulseaudio/default-sink-volume-label)
       :hostname         (-> (process/$ hostname) process/check :out slurp string/trim)}
      (merge (r.spotify/spotify-current-song)
             (r.battery/info))
      (dissoc :spotify/album-url :spotify/album)
      (assoc :todos/in-progress todos)
      (assoc :todos/latest latest)
      (assoc :topbar/background-mode (background-mode)))))

(comment
  (build-topbar-metadata))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; systemic
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsys *topbar-metadata-stream*
  :start (s/stream)
  :stop (s/close! *topbar-metadata-stream*))

(comment
  (sys/start! `*topbar-metadata-stream*))

(defn update-topbar-metadata []
  (s/put! *topbar-metadata-stream* (build-topbar-metadata)))

(comment
  (->>
    (build-topbar-metadata)
    (sort-by :awesome.tag/index)
    first)

  (update-topbar-metadata))
