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
   [clojure.pprint :as pprint]
   [wing.core :as w]
   [notebooks.viewers.my-notebooks :as my-notebooks]))

(clerk/add-viewers! [my-notebooks/viewer])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce !channel->notebook (atom {}))

(defn channels []
  (keys @!channel->notebook))

(defn notebook->channels []
  (w/group-by second first #{} @!channel->notebook))

(comment
  (reset! !channel->notebook {})
  (channels)
  (notebook->channels))

(defn ch->src-addr [ch]
  (str (.getSourceAddress ch)))

(defn nb-ch-maps []
  (->> @!channel->notebook
       (map (fn [[ch nb]]
              {:notebook nb :channel (ch->src-addr ch)}))))

(defn path->notebook-sym [path]
  ;; convert "/notebooks/clawe" -> 'notebooks.clawe
  (-> path (string/replace-first "/" "") (string/replace-first "/" ".") symbol))

(defn msg->notebook [msg]
  (some-> msg :path path->notebook-sym))

^::clerk/no-cache
(def default-notebook 'notebooks.clerk)

(defn log-state []
  (println "[CLERK] channel->notebook:" (pprint/print-table (nb-ch-maps)))
  (println "[CLERK] default-notebook:" default-notebook))

(defn channel-visiting-notebook [msg]
  (let [notebook (msg->notebook msg)]
    (swap! !channel->notebook assoc (:channel msg) notebook)
    (log-state)))

(defn channel-left-notebook [msg]
  (swap! !channel->notebook dissoc (:channel msg))
  (log-state))

(comment
  (->>
    {:a 2 :b 3 :c 2}
    (w/group-by second first #{})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^:dynamic *send* [channel msg]
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
  (eval-notebook 'notebooks.tmux)
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

(defn doc->html [evaled-notebook]
  (->html {} {:doc (clerk-view/doc->viewer {} evaled-notebook) :error nil}))

(defn doc->static-html [evaled-notebook]
  (->html {:conn-ws? false} {:doc (clerk-view/doc->viewer {:inline-results? true} evaled-notebook)}))

(defn ns-sym->html [ns-sym]
  (some-> (eval-notebook ns-sym) doc->html))

(defn ns-sym->viewer [ns-sym]
  (some-> (eval-notebook ns-sym) (clerk-view/doc->viewer)))

(defn path+ns-sym->spit-static-html [path ns-sym]
  ;; make sure dirs exist
  (fs/create-dirs (fs/parent path))
  (spit path (doc->static-html (eval-notebook ns-sym))))

(comment
  (ns-sym->viewer 'notebooks.tmux)

  (some-> (eval-notebook 'notebooks.clerk) doc->html)
  (-> (eval-notebook 'notebooks.tmux) doc->html)
  (clerk-view/doc->viewer {} (eval-notebook 'notebooks.tmux))

  (path+ns-sym->spit-static-html "test.html" 'notebooks.tmux))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-open-notebooks
  "Evals each notebook in !channel->notebook, sending updates to each active channel."
  ;; TODO support simple name (`"wallpapers"`) and path "/notebooks/wallpapers" as input here
  ;; TODO should probably be able to updating just the passed notebook here
  ;; this is a pretty big hammer, tho probably useful for some cases
  ([] (update-open-notebooks default-notebook))
  ([fallback-notebook]
   (println "[Info]: updating open notebooks/channels")
   (doseq [[notebook channels] (notebook->channels)]
     (let [upd
           (clerk-viewer/->edn
             {:doc (ns-sym->viewer
                     (or notebook fallback-notebook))})]
       (when-not notebook
         (println "[Warning]: nil notebook detected. using: " fallback-notebook))
       (println "[Info]: updating" (count channels) "channels")
       (doseq [ch channels]
         (*send* ch upd))))))

(comment
  (update-open-notebooks)
  (update-open-notebooks 'notebooks.clerk)
  (update-open-notebooks 'notebooks.core))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
{::clerk/visibility {:result :show}}

^{::clerk/no-cache true}
(def wsp (wm/current-workspace))

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

;; ## state

;; connections

(let [data (nb-ch-maps)]
  (if (seq data)
    (clerk/table data)
    (clerk/md "### no current connections")))

