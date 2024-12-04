(ns components.charts
  (:require
   [clojure.string :as string]
   [uix.core :as uix :refer [$ defui]]))

(defn- show-chart-fn [canvas-id chart-data]
  (fn []
    (let [ctx (.. js/document
                  (getElementById canvas-id)
                  (getContext "2d"))]

      (when js/Chart
        (js/Chart. ctx (clj->js chart-data))))))

(defui chart-component [chart-data]
  (let [canvas-id         (str (gensym))
        show-chart        (show-chart-fn canvas-id chart-data)
        [chart set-chart] (uix/use-state nil)]
    (uix/use-effect
      (fn []
        (let [c (show-chart)]
          (set-chart c)))
      [show-chart])

    (uix/use-effect
      (fn []
        (when-let [data (some-> chart-data :data :datasets first :data)]
          (when-let [^js/Chart c chart]
            (aset c "data" "datasets" 0 "data"  (clj->js data))
            (.update c))))
      [chart chart-data])

    ($ :canvas {:id canvas-id})))

(defn parse-int [str]
  (-> str
      (string/replace "%" "")
      js/parseInt))

(comment
  (parse-int "81%"))

(defui pie-chart
  "Assumes value is a percentage string like '81%'."
  [{:keys [label value color]}]
  (let [value     (parse-int value)
        remaining (- 100 value)]
    ($ :div
       {:class ["p-1"]}
       ($ chart-component
          {:type    :doughnut
           :data    {:labels [label]
                     :datasets
                     [{:label           label
                       :data            [value remaining]
                       :backgroundColor [color "rgba(0, 0, 0, 0)"]
                       :borderWidth     1
                       :rotation        270
                       :circumference   180}]}
           :options {:cutout    "40%"
                     :plugins   {:legend {:display false}}
                     :animation {:animateScale  true
                                 :animateRotate true}}}))))
