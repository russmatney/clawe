(ns clawe.doctor
  (:require
   [clawe.config :as clawe.config]
   [timer :as timer]))

(timer/print-since "clawe.doctor\tNamespace (and deps) Loaded")

(defn update-topbar-url []
  (str (clawe.config/doctor-base-url) "/topbar/update"))

(defn update-topbar
  ([] (update-topbar nil))
  ([_] (slurp (update-topbar-url))))

(defn reload
  ([] (reload nil))
  ([_] (slurp (str (clawe.config/doctor-base-url) "/reload"))))

(defn update-screenshots []
  (slurp (str (clawe.config/doctor-base-url) "/screenshots/update")))

(defn start-pomodoro []
  (slurp (str (clawe.config/doctor-base-url) "/pomodoros/start")))

(defn refresh-git-status []
  (slurp (str (clawe.config/doctor-base-url) "/git/refresh")))

;; ingest

(defn ingest-file [{:keys [path]}]
  ;; TODO url-encode the path?
  (slurp (str (clawe.config/doctor-base-url) "/ingest?file=" path)))

;; blog

(defn rebuild-blog
  ([] (rebuild-blog nil))
  ([_] (slurp (str (clawe.config/doctor-base-url) "/blog/rebuild"))))

(defn rebuild-blog-indexes
  ([] (rebuild-blog-indexes nil))
  ([_] (slurp (str (clawe.config/doctor-base-url) "/blog/rebuild-indexes"))))

(defn rebuild-blog-open-pages
  ([] (rebuild-blog-open-pages nil))
  ([_] (slurp (str (clawe.config/doctor-base-url) "/blog/rebuild-open-pages"))))

(defn restart-blog-systems
  ([] (restart-blog-systems nil))
  ([_] (slurp (str (clawe.config/doctor-base-url) "/blog/restart-systems"))))

;; clawe

(defn clawe-mx
  ([] (clawe-mx))
  ([_] (slurp (str (clawe.config/doctor-base-url) "/clawe-mx"))))

(defn clawe-mx-fast
  ([] (clawe-mx-fast))
  ([_] (slurp (str (clawe.config/doctor-base-url) "/clawe-mx-fast"))))

(defn clawe-mx-suggestions
  ([] (clawe-mx-suggestions))
  ([_] (slurp (str (clawe.config/doctor-base-url) "/clawe-mx-suggestions"))))

(defn clawe-mx-open
  ([] (clawe-mx-open))
  ([_] (slurp (str (clawe.config/doctor-base-url) "/clawe-mx-open"))))

(defn clawe-toggle
  {:org.babashka/cli {:alias {:key :client/key}}}
  [opts]
  (let [client-key (or (:client/key opts) (:key opts))]
    (timer/print-since "clawe.doctor/clawe-toggle\tslurping")
    (slurp (str (clawe.config/doctor-base-url) "/clawe-toggle" "?" client-key))))
