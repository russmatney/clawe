(ns notebooks.clerk
  {:nextjournal.clerk/visibility {:code :hide :result :hide}
   :nextjournal.clerk/no-queue   true}
  (:require
   [nextjournal.clerk :as clerk]
   [clawe.wm :as wm]

   [nextjournal.clerk.viewer :as clerk-viewer]

   [ring.adapter.undertow.websocket :as undertow.ws]
   [nextjournal.clerk.analyzer :as clerk-analyzer]
   [clojure.java.io :as io]
   [nextjournal.clerk.eval :as clerk-eval]
   [nextjournal.clerk.view :as clerk-view]
   [clojure.string :as string]
   [babashka.fs :as fs]))

^{::clerk/no-cache true}
(def wsp (wm/current-workspace))

^{::clerk/visibility {:result :show}}
(clerk/html
  [:div
   [:h3 {:class ["text-sm" "font-mono"]}
    "current wsp"]
   [:div
    {:class ["flex" "flex-col"]}
    [:span
     {:class ["px-4" "font-mono"]}
     (-> wsp :workspace/title (#(str "title: " %)))]
    [:span
     {:class ["px-4" "font-mono"]}
     (-> wsp
         :workspace/directory
         (string/replace (str (fs/home)) "~")
         (#(str "dir: " %)))]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce !channels-by-notebook (atom {}))
(comment (reset! !channels-by-notebook {}))
^::clerk/no-cache
(def default-notebook 'notebooks.clerk)

(defn log-state []
  (println "[clerk.clj] channels-by-notebook:" @!channels-by-notebook)
  (println "[clerk.clj] default-notebook:" default-notebook))

(defn msg->notebook [msg]
  ;; TODO can we get this from the msg?
  nil)

(defn channel-visiting-notebook [msg]
  (let [notebook (msg->notebook msg)]
    (swap! !channels-by-notebook
           #(update % notebook
                    (fn [chs]
                      (let [ch (:channel msg)]
                        (if chs (conj chs ch) #{ch})))))
    (log-state)))

(defn channel-left-notebook [msg]
  (swap! !channels-by-notebook #(update % (msg->notebook msg) disj (:channel msg)))
  (log-state))

(defn notebooks-by-channel []
  @!channels-by-notebook)

(defn channels []
  (concat (vals @!channels-by-notebook)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; ## health report

^{::clerk/visibility {:result :show}}
(->
  (notebooks-by-channel)
  (get nil)
  count
  (#(str "### status report: `" % "` channels have a 'nil' notebook"))
  clerk/md)

;; notebooks:
^{::clerk/visibility {:result :show}}
(->>
  (notebooks-by-channel)
  keys
  (map #(str "- " (or % "nil")))
  (apply str)
  (clerk/md))

;; channels:
^{::clerk/visibility {:result :show}}
(->>
  (channels)
  (map #(str "- " (or % "nil")))
  (apply str)
  (clerk/md))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^:dynamic *send* [channel msg]
  (println "*send*ing msg to channel")
  ;; TODO argument order is killing me here
  (undertow.ws/send msg channel))

(defn eval-notebook
  "Evaluates the notebook identified by its `ns-sym`"
  [ns-sym]
  (try
    (-> ns-sym clerk-analyzer/ns->path (str ".clj") io/resource clerk-eval/eval-file)
    (catch Throwable e
      (println "error evaling notebook", ns-sym)
      (println e))))

(comment
  (eval-notebook 'notebooks.clerk)
  (eval-notebook 'notebooks.core)
  (eval-notebook 'notebooks.wallpapers)
  (eval-notebook 'notebooks.clawe)
  (eval-notebook 'notebooks.dice))

(defn ns-sym->html [ns-sym]
  (some-> (eval-notebook ns-sym) (clerk-view/doc->html nil)))

(defn ns-sym->viewer [ns-sym]
  (some-> (eval-notebook ns-sym) (clerk-view/doc->viewer)))

(comment
  (ns-sym->viewer 'notebooks.core)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-open-notebooks
  "The big one.

  Evals each notebook in !channels-by-notebook,
  sending updates to each channel viewing it."
  ([] (update-open-notebooks default-notebook))
  ([fallback-notebook]
   (println "\n\n[Info]: updating open notebooks/channels")
   (println @!channels-by-notebook)
   (->>
     (notebooks-by-channel)
     (map
       (fn [[notebook channels]]
         (let [upd
               (clerk-viewer/->edn
                 {:doc (ns-sym->viewer
                         (or notebook fallback-notebook))})]
           (when-not notebook
             (println "[Warning]: nil notebook detected. using: " fallback-notebook))
           (println "[Info]: updating " (count channels) " channels")
           (->> channels
                (map (fn [ch]
                       (*send* ch upd)))
                seq))))
     seq)))

(comment
  (update-open-notebooks)
  (update-open-notebooks 'notebooks.clerk)
  (update-open-notebooks 'notebooks.core)
  )
