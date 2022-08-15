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
;; ls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-client-headers
  #{:client/key
    :client/window-title
    :client/app-name
    :yabai.window/title
    :yabai.window/id})

(def default-workspace-headers
  #{:workspace/title
    :workspace/index
    :yabai.space/windows
    :yabai.space/has-focus
    :workspace/focused
    :yabai.space/label})

(defn ls
  {:org.babashka/cli
   {:coerce {:type          :keyword
             :headers       [:keyword]
             :extra-headers [:keyword]
             :strip         :boolean
             :all           :boolean}}}
  ([] (ls {:type :clients}))
  ([{:keys [extra-headers headers strip type all]
     :or   {strip false all false}
     :as   opts}]
   (println "ls opts" opts)
   (let [headers (cond
                   (seq headers) headers
                   all           nil
                   :else
                   (case type
                     :clients    default-client-headers
                     :workspaces default-workspace-headers))
         headers (if (seq extra-headers)
                   (concat headers extra-headers)
                   headers)]
     (cond->> (case type
                :clients (wm/active-clients)
                :workspaces (wm/active-workspaces))

       ;; NOTE strip can remove fields that we might expect in `headers`
       (and (not all) strip)
       (map
         (case type
           :clients    client/strip
           :workspaces workspace/strip))

       (seq headers)
       (map (fn [m] (select-keys m headers)))

       true
       (clojure.pprint/print-table)))))

(comment
  (ls {:type    :clients
       :headers (conj default-client-headers :yabai.window/space)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ls consumers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ls-clients
  {:org.babashka/cli
   {:coerce {:headers [:keyword] :all :boolean}}}
  ([] (ls-clients nil))
  ([opts] (ls (assoc opts :type :clients))))

(defn ls-workspaces
  {:org.babashka/cli
   {:coerce {:headers [:keyword] :all :boolean}}}
  ([] (ls-clients nil))
  ([opts] (ls (assoc opts :type :workspaces))))

(comment
  (ls-clients)
  (ls-clients {:headers (conj default-client-headers :yabai.window/space)})
  (ls-workspaces))

