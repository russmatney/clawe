^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(ns notebooks.system
  (:require
   [systemic.core :as sys :refer [defsys]]
   [nextjournal.clerk :as clerk]
   [clojure.string :as string]
   [babashka.fs :as fs]))

(defn ->f [path]
  {:path path
   :name (fs/file-name path)})

^{::clerk/visibility {:code :hide :result :hide}}
(defn clerk-files
  []
  (try
    (some->> (str (fs/home) "/russmatney/clawe" "/src/notebooks")
             fs/list-dir (map str) (map ->f))
    (catch Exception _e
      (println "nothing at *file* " *file*)
      ;; (println e)
      nil)))

^{::clerk/visibility {:code :hide :result :hide}}
(def ^:dynamic *current-clerk-file*
  (atom (->f *file*)))


^{::clerk/visibility {:code   :hide
                      :result :hide}}
(defn rerender []
  (clerk/show! (:path @*current-clerk-file*)))

^{::clerk/visibility {:code :hide :result :hide}}
(defn set-file [f]
  (println "set file called" f)
  (reset! *current-clerk-file* f)
  (rerender))

^{::clerk/visibility {:result :hide}}
(def port 8888)

^{::clerk/visibility {:result :hide :code :hide}}
(defsys *clerk-server*
  :start (clerk/serve! {:port        port
                        :watch-paths (->> (clerk-files)
                                          (map :path))}))

(defn restart []
  (if (sys/running? `*clerk-server*)
    (sys/restart! `*clerk-server*)
    (sys/start! `*clerk-server*)))

(comment
  (restart))

;; # Clerk System

(clerk/md (str "- port: " (:port *clerk-server*)))

(clerk/md
  (->>
    (:watch-paths *clerk-server*)
    (map (fn [path] (str "- watching: " path)))
    (string/join "\n")))
