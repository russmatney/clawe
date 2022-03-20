(ns doctor.ui.components.debug
  (:require
   [uix.core.alpha :as uix]))


(defn colorized-metadata
  ([m] (colorized-metadata 0 m))
  ([level m]
   (cond
     (map? m)
     (->> m (map (partial colorized-metadata (inc level))))

     :else
     (let [[k v] m]
       (println "k" k "v" v)
       ^{:key k}
       [:div.font-mono
        {:class ["text-city-gray-400" (str "pl-" (* 2 level))]}
        "["
        [:span {:class ["text-city-pink-400"]} (str k)]
        " "

        (cond
          (and (map? v) (seq v))
          (->> v (map (partial colorized-metadata (inc level))))

          (and (list? v) (seq v))
          (->> v (map (partial colorized-metadata (inc level))))

          :else
          [:span {:class ["text-city-green-400"]}
           (cond
             (and (map? v) (empty? v))  "{}"
             (and (list? v) (empty? v)) "[]"

             (nil? v) "nil"

             :else
             (str v))])

        "] "]))))


(defn raw-metadata
  ([metadata] [raw-metadata nil metadata])
  ([{:keys [label initial-show?]} metadata]
   (let [label                    (or label "Show raw metadata")
         show-raw-metadata?       (uix/state initial-show?)
         toggle-show-raw-metadata #(swap! show-raw-metadata? (comp boolean not))]
     [:div
      [:span.text-sm
       {:class    ["hover:text-city-pink-400" "cursor-pointer"]
        :on-click toggle-show-raw-metadata}
       label]

      (when @show-raw-metadata?
        [:div
         {:class ["mt-auto"]}
         (when metadata
           (->>
             metadata
             (map colorized-metadata)))])])))
