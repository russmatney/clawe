(ns expo.ui.views.counts
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[systemic.core :refer [defsys] :as sys]
             [manifold.stream :as s]
             [org-crud.core :as org-crud]
             [ralphie.zsh :as r.zsh]
             [babashka.fs :as fs]
             ]
       :cljs [[wing.core :as w]
              [uix.core.alpha :as uix]
              [plasma.uix :refer [with-rpc with-stream]]])))

#?(:clj
   (defn zp
     "Zero Pad numbers - takes a number and the length to pad to as arguments"
     [n c]
     (format (str "%0" c "d") n)))

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
       "~/todo"
       r.zsh/expand
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
                        (assoc it :count/sort (str "item-count-" (zp i 3)))))))))

#?(:clj
   (defsys *counts-stream*
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
   (defn use-counts []
     (let [counts      (plasma.uix/state [])
           handle-resp (fn [items]
                         (swap! counts
                                (fn [cts]
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

#?(:cljs
   (defn count-comp
     ([item] (count-comp nil item))
     ([_opts item]
      (let [{:count/keys [id value label color]
             :org/keys   [source-file body]
             ;; :keys       [index]
             }        item
            label     (or label id)
            hovering? (uix/state false)]
        [:div
         {:class          ["m-1" "p-4"
                           "border" "border-city-blue-600"
                           "bg-yo-blue-700"
                           "text-white"
                           (when @hovering? "cursor-pointer")]
          :on-mouse-enter #(reset! hovering? true)
          :on-mouse-leave #(reset! hovering? false)}

         [:div
          {:class ["font-nes"
                   "flex"
                   "justify-center"]
           :style (when color {:color color})}
          value]

         [:div
          {:class ["font-mono" "text-lg"
                   (when @hovering? "text-city-blue-400")]
           :style (when color {:color color})}
          label]

         (when @hovering?
           [:div
            {:class    ["font-mono"
                        "hover:text-city-blue-400"]
             :on-click (fn [_]
                         (let [res (open-in-emacs item)]
                           (println res)
                           res))}
            source-file])

         (when @hovering?
           [:div
            {:class ["font-mono" "text-city-blue-400"
                     "flex" "flex-col"
                     "p-2"
                     "bg-yo-blue-500"]}
            (for [[i line] (map-indexed vector body)]
              ^{:key i}
              (let [{:keys [text]} line]
                (cond
                  (= "" text) [:span {:class ["py-1"]} " "]

                  :else
                  [:span text])))])]))))


#?(:cljs
   (defn widget []
     (let [{:keys [items count]} (use-counts)
           ]
       (println "Count: " count)
       (when (seq items) (println (last items)))
       ;; TODO think about a :count/break or hydra-ish api for splitting here
       ;; TODO pull tags into suggestions for hide/show filters
       [:div
        {:class ["flex" "flex-row" "flex-wrap"
                 "justify-center"
                 "overflow-hidden"]}

        (for [[i it] (->> items (map-indexed vector))]
          ^{:key i}
          [count-comp nil (assoc it :index i)])])))
