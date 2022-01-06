(ns expo.ui.views.garden
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[systemic.core :refer [defsys] :as sys]
             [manifold.stream :as s]
             [org-crud.core :as org-crud]
             [ralphie.zsh :as r.zsh]
             [tick.alpha.api :as t]
             [babashka.fs :as fs]]
       :cljs [[wing.core :as w]
              [uix.core.alpha :as uix]
              [plasma.uix :refer [with-rpc with-stream]]])
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:clj
   (defn parse-created-at [x]
     x)
   )

#?(:clj
   (comment
     (parse-created-at "20210712:163730")
     (t/parse "20210712:163730" (t/format "yyyyMMdd:hhmmss"))
     )
   )

#?(:clj
   (defn org->garden-node
     [{:org/keys      [source-file]
       :org.prop/keys [title created-at]
       :as            item}]
     (def i item)
     (->
       item
       (dissoc :org/items)
       (assoc :garden/file-name (fs/file-name source-file)
              :org/source-file (string/replace-first source-file "/home/russ/todo/" "")
              :org.prop/created-at (parse-created-at created-at)
              :org.prop/title (or title (fs/file-name source-file))))))

#?(:clj
   (comment
     (org->garden-node i)
     ))


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
                   (string/includes? source-file "/daily/"))))
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
   (defn use-garden []
     (let [items       (plasma.uix/state [])
           handle-resp (fn [its] (reset! items its))]

       (with-rpc [] (get-garden-handler) handle-resp)
       (with-stream [] (garden-stream) handle-resp)

       {:items @items})))

#?(:cljs
   (defn garden-node
     [{:keys [on-select is-selected?]} item]
     (let [{:org/keys      [source-file body]
            :org.prop/keys [title created-at]
            :garden/keys   [file-name]}

           item
           hovering? (uix/state false)]
       [:div
        {:class          ["m-1" "p-4"
                          "border" "border-city-blue-600"
                          "bg-yo-blue-700"
                          "text-white"
                          (when @hovering? "cursor-pointer")]
         :on-click       #(on-select)
         :on-mouse-enter #(reset! hovering? true)
         :on-mouse-leave #(reset! hovering? false)}

        title

        (when created-at
          [:div
           {:class ["font-mono"]}
           created-at])

        [:div
         {:class    ["font-mono"
                     "hover:text-city-blue-400"]
          :on-click (fn [_]
                      (let [res (open-in-emacs item)]
                        (println res)
                        res))}
         source-file]])))

#?(:cljs
   (defn selected-node
     [{:org/keys      [source-file body]
       :org.prop/keys [title created-at]
       :garden/keys   [file-name]}]

     [:div
      {:class ["flex" "flex-col" "p-2"]}
      [:span
       {:class ["font-nes" "text-xl" "text-city-green-200" "p-2"]}
       title]

      [:span
       {:class ["font-mono" "text-xl" "text-city-green-200" "p-2"]}
       source-file]

      [:div
       {:class ["font-mono" "text-city-blue-400"
                "flex" "flex-col" "p-2"
                "bg-yo-blue-500"]}
       (for [[i line] (map-indexed vector body)]
         (let [{:keys [text]} line]
           (cond
             (= "" text)
             ^{:key i} [:span {:class ["py-1"]} " "]

             :else
             ^{:key i} [:span text])))]]))

#?(:cljs
   (defn view []
     (let [{:keys [items]} (use-garden)
           selected        (uix/state (first items)) ]
       [:div
        {:class ["flex" "flex-col" "flex-wrap"
                 "overflow-hidden"
                 "min-h-screen"]}

        (when-not items
          [:div
           {:class ["p-6" "text-lg" "text-white"]}
           "Loading...................."
           ])

        (when @selected
          (selected-node @selected))

        [:div
         {:class ["flex" "flex-row" "flex-wrap"
                  "justify-center"

                  ]}
         (for [[i it] (->> items (map-indexed vector))]
           ^{:key i}
           [garden-node
            {:on-select    (fn [_] (reset! selected it))
             :is-selected? (= @selected it)}
            (assoc it :index i)])]])))
