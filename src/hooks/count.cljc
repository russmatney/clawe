(ns hooks.count
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[systemic.core :refer [defsys] :as sys]
             [manifold.stream :as s]
             [org-crud.core :as org-crud]
             [babashka.fs :as fs]
             [util]]
       :cljs [[wing.core :as w]
              [plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:clj
   (defn org->count [{:org/keys [source-file items] :as item}]
     (merge
       ;; don't send alllll the data over the wire until we need it
       (dissoc item :org/items)
       {:count/id    (str "items-" (fs/file-name source-file))
        :count/label (fs/file-name source-file)
        :count/value (count items)})))

#?(:clj
   (defn todo-dir-files []
     (->>
       (str (fs/home) "/todo")
       (org-crud/dir->nested-items {:recursive? true})
       (map org->count))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; get-counts
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:clj
   (defn get-counts []
     (concat
       [{:count/id    "example"
         :count/sort  (str 0)
         :count/value 999
         :count/label "Example Count"}]
       (->>
         (todo-dir-files)
         (sort-by :count/value)
         (reverse)
         (map-indexed (fn [i it]
                        (assoc it :count/sort (str "item-count-" (util/zp i 3)))))))))

#?(:clj
   (defsys ^:dynamic *counts-stream*
     :start (s/stream)
     :stop (s/close! *counts-stream*)))

#?(:clj
   (comment
     (sys/start! `*counts-stream*)))

#?(:clj
   (defn update-counts []
     (s/put! *counts-stream* (get-counts))))

(defhandler get-counts-handler []
  (get-counts))

(defstream counts-stream [] *counts-stream*)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-counts []
     (let [counts      (plasma.uix/state [])
           handle-resp (fn [items]
                         (swap! counts
                                (fn [_cts]
                                  (->>
                                    ;; (concat (or cts []) items)
                                    items
                                    (w/distinct-by :count/id)
                                    (sort-by :count/sort)
                                    (sort-by (comp nil? :count/sort))))))]

       (with-rpc [] (get-counts-handler) handle-resp)
       (with-stream [] (counts-stream) handle-resp)

       {:items @counts
        :count (count @counts)})))
