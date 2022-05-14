(ns components.debug
  (:require
   [uix.core.alpha :as uix]))

(defn colls-last [m]
  (->> m
       (sort-by (fn [k-v]
                  (and (vector? k-v)
                       (-> k-v second coll?))))))

(defn map-key-sort [m]
  (->> m (sort-by first) colls-last))

(comment
  (colls-last {:some-v 5 :some-coll [5]})
  )


(defn colorized-metadata
  "TODO figure out why data is getting double printed/leaking in here"
  ([m] (colorized-metadata {} m))
  ([opts m] (colorized-metadata 0 opts m))
  ([level {:keys [exclude-key] :as opts} m]
   (let [exclude-key (or exclude-key #{})]
     (cond
       (map? m)
       (->> m
            map-key-sort
            (map (partial colorized-metadata (inc level) opts)))

       :else
       (let [[k v] m]
         (when-not (exclude-key k)
           ^{:key k}
           [:div.font-mono
            {:class ["text-city-gray-400" (str "px-" (* 2 level))]}
            "["
            [:span {:class ["text-city-pink-400"]} (str k)]
            " "

            (cond
              (and (map? v) (seq v))
              (->> v
                   map-key-sort
                   (map (partial colorized-metadata (inc level) opts)))

              (and (list? v) (seq v))
              (->> v (map (partial colorized-metadata (inc level) opts)))

              :else
              [:span {:class ["text-city-green-400"]}
               (cond
                 (and (map? v) (empty? v))  "{}"
                 (and (list? v) (empty? v)) "[]"

                 (nil? v) "nil"

                 :else
                 (str v))])

            "] "]))))))


(defn raw-metadata
  ([metadata] [raw-metadata nil metadata])
  ([{:keys [label initial-show? initial-show] :as opts} metadata]
   (let [label                    (if (= false label) nil (or label "Toggle raw metadata"))
         show-raw-metadata        (uix/state (or initial-show? initial-show))
         toggle-show-raw-metadata #(swap! show-raw-metadata (comp boolean not))]
     [:div
      {:class ["pb-4"]}
      (when label
        [:span.text-sm
         {:class    ["hover:text-city-pink-400" "cursor-pointer"]
          :on-click toggle-show-raw-metadata}
         label])

      ;; TODO consider a modal, or floating/popover support
      (when @show-raw-metadata
        [:div
         {:class ["mt-auto"]}
         (when metadata
           (->> metadata
                map-key-sort
                (map #(colorized-metadata opts %))))])])))
