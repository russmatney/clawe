(ns clawe.doctor
  (:require
   [clawe.config :as clawe.config]))

(defn update-topbar-url []
  (str (clawe.config/doctor-base-url) "/topbar/update"))

(defn update-topbar
  ([] (update-topbar nil))
  ([_] (slurp (update-topbar-url))))

(defn reload []
  (when-not (clawe.config/is-mac?)
    (slurp (str (clawe.config/doctor-base-url) "/reload"))))

(defn update-screenshots []
  (slurp (str (clawe.config/doctor-base-url) "/screenshots/update")))

(defn rerender-notebook
  ([] (rerender-notebook nil))
  ([notebook-name]
   (slurp (str (clawe.config/doctor-base-url)
               (if notebook-name
                 (str "/rerender/notebooks/" notebook-name)
                 "/rerender/notebooks")))))

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

(defn clawe-mx
  ([] (clawe-mx))
  ([_] (slurp (str (clawe.config/doctor-base-url) "/clawe-mx"))))

(defn clawe-mx-fast
  ([] (clawe-mx-fast))
  ([_] (slurp (str (clawe.config/doctor-base-url) "/clawe-mx-fast"))))

(defn clawe-mx-suggestions
  ([] (clawe-mx-suggestions))
  ([_] (slurp (str (clawe.config/doctor-base-url) "/clawe-mx-suggestions"))))
