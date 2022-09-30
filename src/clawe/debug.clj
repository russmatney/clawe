(ns clawe.debug
  "Helpers for debugging"
  (:require
   [clojure.pprint :as pprint]
   [babashka.cli :as cli]
   [clawe.wm :as wm]
   [clawe.client :as client]
   [clawe.workspace :as workspace]
   [ralphie.tmux :as tmux]))

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

(def default-client-sort :client/key)

(def default-workspace-headers
  #{:workspace/title
    :workspace/index
    :yabai.space/windows
    :yabai.space/has-focus
    :workspace/focused
    :yabai.space/label})

(def default-workspace-sort :workspace/index)

;; TODO reach for an ls multi-method/protocol here
;; debug/ls defmethod, with :type, :strip, :default-headers, and :fetch methods
(defn ls
  "A general debug-helper and lister of things.

  Supports passed `:xs`, but mostly intends to support context-less `:type` inputs.

  Impled for types: `:clients`, `:workspaces`, `:tmux`, `:tmux-panes`.

  Consumed and exposed by the clerk clawe notebook: `/notebooks/clawe.clj`. "
  {:org.babashka/cli
   {:coerce {:type          :keyword
             :headers       [:keyword]
             :extra-headers [:keyword]
             :strip         :boolean
             :all           :boolean
             :sort          :keyword}}}
  ([] (ls {:type :clients}))
  ([{:keys [extra-headers headers strip type all sort xs]
     :or   {strip false all false}
     :as   opts}]
   (let [headers (cond
                   (seq headers) headers
                   all           nil
                   :else
                   (case type
                     :clients    default-client-headers
                     :workspaces default-workspace-headers
                     nil))
         headers (if (seq extra-headers)
                   (concat headers extra-headers)
                   headers)
         s-by    (or sort (case type
                            :clients    default-client-sort
                            :workspaces default-workspace-sort
                            :tmux-panes #(str (:tmux.session/name %) "-" (:tmux.window/index %))
                            nil))]
     (cond->> (case type
                :clients (wm/active-clients)
                :workspaces (wm/active-workspaces)
                :tmux (vals (tmux/list-sessions))
                :tmux-panes (tmux/list-panes)
                xs)

       s-by
       (sort-by s-by)

       ;; NOTE strip can remove fields that we might expect in `headers`
       (and (not all) strip)
       (map
         (case type
           :clients    client/strip
           :workspaces workspace/strip
           identity))

       (seq headers)
       (map (fn [m] (select-keys m headers)))))))

(comment
  (ls {:type    :clients
       :headers (conj default-client-headers :yabai.window/space)}))

(defn ls-print
  [args]
  (-> (ls args)
      (clojure.pprint/print-table)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ls consumers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ls-clients
  {:org.babashka/cli
   {:coerce {:headers [:keyword] :all :boolean}}}
  ([] (ls-clients nil))
  ([opts] (ls-print (assoc opts :type :clients))))

(defn ls-workspaces
  {:org.babashka/cli
   {:coerce {:headers [:keyword] :all :boolean}}}
  ([] (ls-clients nil))
  ([opts] (ls-print (assoc opts :type :workspaces))))

(comment
  (ls-clients)
  (ls-clients {:headers (conj default-client-headers :yabai.window/space)})
  (ls-workspaces))

