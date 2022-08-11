(ns clawe.debug
  "Helpers for debugging"
  (:require
   [clojure.pprint :as pprint]
   [babashka.cli :as cli]
   [clawe.wm :as wm]
   [clawe.client :as client]
   [clawe.workspace :as workspace]))

(comment
  (cli/parse-opts
    ["--port" "1339"
     "--headers" "some"
     "--headers" "keys"
     ]
    {:coerce {:port    :long
              :headers [:string]}}))

(defn print-threads
  {:org.babashka/cli
   {:coerce {:headers [:keyword]}}}
  ([] (print-threads nil))
  ([{:keys [headers pre-fn]
     :or   {pre-fn identity}
     :as   opts}]
   (println opts)
   (let [thread-set  (keys (Thread/getAllStackTraces))
         thread-data (mapv bean thread-set)
         headers     (or headers (-> thread-data first keys))]
     (clojure.pprint/print-table headers (pre-fn thread-data)))))

;; (defn print-threads-str [& args]
;;   (with-out-str (apply print-threads args)))


(comment
  ;;print all properties for all threads
  (print-threads)

  ;;print name,state,alive,daemon properties for all threads
  (print-threads
    {:headers [:name :state :alive :daemon]})

  ;; run in terminal like:
  ;; clawebb -x clawe.debug/print-threads --headers name --headers alive --headers state

  ;;print name,state,alive,daemon properties for non-daemon threads
  (print-threads
    {:headers [:name :state :alive :daemon]
     :pre-fn  (partial filter #(false? (:daemon %)))}))


(comment
  (cli/parse-opts
    ["--all"
     "--headers" "some"
     "--headers" "keys"
     ]
    {:coerce {:all     :boolean
              :headers [:string]}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clients
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn print-active-clients
  {:org.babashka/cli
   {:coerce {:headers [:keyword]
             :all     :boolean}}}
  ([] (print-active-clients nil))
  ([{:keys [headers] :as opts}]
   (println "opts" opts)
   (cond->> (wm/active-clients)

     (not (:all opts))
     (map client/strip)

     (seq headers)
     (map (fn [m]
            (select-keys m headers)))

     true
     (clojure.pprint/print-table))))

(comment
  (print-active-clients))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn print-active-workspaces
  {:org.babashka/cli
   {:coerce {:headers [:keyword]
             :all     :boolean}}}
  ([] (print-active-workspaces nil))
  ([{:keys [headers] :as opts}]
   (println "opts" opts)
   (cond->> (wm/active-workspaces)

     (not (:all opts))
     (map workspace/strip)

     (seq headers)
     (map (fn [m]
            (select-keys m headers)))

     true
     (clojure.pprint/print-table))))

(comment
  (print-active-workspaces))
