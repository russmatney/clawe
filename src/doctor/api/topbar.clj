(ns doctor.api.topbar
  (:require
   [systemic.core :refer [defsys] :as sys]
   [manifold.stream :as s]
   [ralphie.battery :as r.battery]
   [ralphie.pulseaudio :as r.pulseaudio]
   [ralphie.spotify :as r.spotify]
   [babashka.process :as process]
   [clojure.string :as string]
   [clawe.db.core :as db]))

(defn in-progress-todos []
  (some->>
    (db/query
      '[:find (pull ?e [*])
        :where
        [?e :todo/status :status/in-progress]])
    first))

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
      (assoc :todos/latest latest))))

(defsys *topbar-metadata-stream*
  :start (s/stream)
  :stop (s/close! *topbar-metadata-stream*))

(comment
  (sys/start! `*topbar-metadata-stream*))

(defn update-topbar-metadata []
  (println "pushing to topbar-metadata stream (updating topbar-metadata)!")
  (s/put! *topbar-metadata-stream* (build-topbar-metadata)))

(comment
  (->>
    (build-topbar-metadata)
    (sort-by :awesome.tag/index)
    first)

  (update-topbar-metadata)
  )
