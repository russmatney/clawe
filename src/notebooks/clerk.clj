(ns notebooks.clerk
  {:nextjournal.clerk/visibility {:code :hide :result :hide}
   :nextjournal.clerk/no-queue   true
   :nextjournal.clerk/toc        true}
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [hiccup.page :as hiccup]
   [nextjournal.clerk :as clerk]
   [nextjournal.clerk.analyzer :as clerk-analyzer]
   [nextjournal.clerk.eval :as clerk-eval]
   [nextjournal.clerk.view :as clerk-view]
   [nextjournal.clerk.viewer :as clerk-viewer]
   [ring.adapter.undertow.websocket :as undertow.ws]

   [clawe.wm :as wm]
   [clojure.pprint :as pprint]))

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
  (println "[CLERK] channels-by-notebook:" (->> @!channels-by-notebook
                                                (mapcat (fn [[notebook chs]]
                                                          (->> chs
                                                               (map (fn [ch]
                                                                      {:notebook     notebook
                                                                       :chSourceAddr (str (.getSourceAddress ch))})))))
                                                (pprint/print-table)))
  (println "[CLERK] default-notebook:" default-notebook))

(declare path->notebook-sym)

(defn msg->notebook [msg]
  (some-> msg :path path->notebook-sym))

(defn channel-visiting-notebook [msg]
  (let [notebook (msg->notebook msg)]
    (swap! !channels-by-notebook
           (fn [nb->chs]
             (cond->
                 nb->chs

               true ;; always init or add
               (update notebook (fn [chs]
                                  (let [ch (:channel msg)]
                                    (if chs (conj chs ch) #{ch}))))

               notebook ;; if notebook had a value, remove chs from nil
               ;; remove from nil
               ;; TODO really, remove from anywhere else it is found,
               ;; which will happen on every navigation
               (update nil (fn [chs]
                             (disj chs (:channel msg)))))))
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

(defn ->html [{:keys [conn-ws?] :or {conn-ws? true}} state]
  (hiccup/html5
    {:class "overflow-hidden min-h-screen"}
    [:head
     [:meta {:charset "UTF-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     (clerk-view/include-css+js)]
    [:body.dark:bg-gray-900
     [:div#clerk]
     [:script "let viewer = nextjournal.clerk.sci_viewer
let state = " (-> state clerk-viewer/->edn pr-str) "
viewer.set_state(viewer.read_string(state))
viewer.mount(document.getElementById('clerk'))\n"
      (when conn-ws?
        "const ws = new WebSocket(document.location.origin.replace(/^http/, 'ws') + '/_ws')
ws.onmessage = msg => viewer.set_state(viewer.read_string(msg.data));
window.ws_send = msg => ws.send(msg);
ws.onopen = () => ws.send('{:path \"' + document.location.pathname + '\"}'); ")]]))

(defn path->notebook-sym [path]
  ;; convert "/notebooks/clawe" -> 'notebooks.clawe
  (-> path (string/replace-first "/" "") (string/replace-first "/" ".") symbol))

(defn doc->html [doc]
  (->html {} {:doc (clerk-view/doc->viewer {} doc) :error nil}))

(defn ns-sym->html [ns-sym]
  (some-> (eval-notebook ns-sym) doc->html))

(defn ns-sym->viewer [ns-sym]
  (some-> (eval-notebook ns-sym) (clerk-view/doc->viewer)))

(comment
  (ns-sym->viewer 'notebooks.core))

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
  (update-open-notebooks 'notebooks.core))
