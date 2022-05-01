(ns hooks.garden
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[systemic.core :refer [defsys] :as sys]
             [manifold.stream :as s]
             [org-crud.core :as org-crud]
             [ralphie.zsh :as r.zsh]
             [babashka.fs :as fs]
             [clojure.string :as string]]
       :cljs [[plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:clj
   (defn parse-created-at [x]
     x)
   )

#?(:clj
   (defn get-last-modified
     [item]
     ;; TODO include this format in parser
     (def i item)
     (-> item :org/source-file fs/last-modified-time str)))


#?(:clj
   (comment
     (-> i :org/source-file fs/last-modified-time str)

     (-> i :org/source-file fs/file-name)
     ))

#?(:clj
   (defn org->garden-node
     [{:org/keys      [source-file]
       :org.prop/keys [title created-at]
       :as            item}]
     (let [last-modified (get-last-modified item)]
       (->
         item
         (dissoc :org/items)
         (assoc :garden/file-name (fs/file-name source-file)
                :org/source-file (-> source-file
                                     (string/replace-first "/home/russ/todo/" "")
                                     (string/replace-first "/Users/russ/todo/" ""))
                :org.prop/created-at (parse-created-at created-at)
                :org.prop/title (or title (fs/file-name source-file))
                :time/last-modified last-modified)))))

#?(:clj
   (defn todo-dir-files []
     (->>
       "~/todo"
       r.zsh/expand
       (org-crud/dir->nested-items {:recursive? true})
       (remove (fn [{:org/keys [source-file]}]
                 (or
                   (string/includes? source-file "/journal/")
                   (string/includes? source-file "/urbint/")
                   (string/includes? source-file "/archive/")
                   (string/includes? source-file "/old/")
                   (string/includes? source-file "/old-nov-2020/")
                   (string/includes? source-file "/kata/")
                   (string/includes? source-file "/standup/")
                   (string/includes? source-file "/drafts-journal/")
                   ;; (string/includes? source-file "/daily/")
                   )))
       (map org->garden-node)
       (sort-by :org/source-file))))

#?(:clj
   (comment
     (->>
       (todo-dir-files)
       (take 3)
       )

     ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; get-garden
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:clj
   (defn get-garden []
     (todo-dir-files)))

#?(:clj
   (comment
     (->>
       (todo-dir-files)
       (count))))

#?(:clj
   (defsys *garden-stream*
     :start (s/stream)
     :stop (s/close! *garden-stream*)))

#?(:clj
   (comment
     (sys/start! `*garden-stream*)))

#?(:clj
   (defn update-garden []
     (s/put! *garden-stream* (get-garden))))

(defhandler get-garden-handler []
  (get-garden))

(defstream garden-stream [] *garden-stream*)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; open-in-emacs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler open-in-emacs [item]
  (println "open in emacs!!!")
  (println "opening file:" item)
  :ok
  )


(comment
  (open-in-emacs {}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   ;; TODO dry up vs views/garden.cljc
   (defn use-garden []
     (let [items       (plasma.uix/state [])
           handle-resp (fn [its] (reset! items its))]

       (with-rpc [] (get-garden-handler) handle-resp)
       (with-stream [] (garden-stream) handle-resp)

       {:items @items})))
