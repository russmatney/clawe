(ns pages.counter
  (:require
   [uix.core.alpha :as uix]
   [wing.uix.router :as router]
   [components.debug]))

(defn page [_opts]
  (let [page-name     (-> router/*match* uix/context :data :name)
        query-params  (router/use-route-parameters [:query])
        the-count     (router/use-route-parameters [:query :count])
        the-count-int (try (js/parseInt @the-count) (catch js/Error _e 0))]

    [:div
     {:class ["p-6" "bg-city-orange-light-800"
              "text-city-orange-light-200"
              "flex" "flex-col"
              "text-4xl"]}

     [components.debug/raw-metadata {:initial-show true
                                     :label        false}
      (assoc (js->clj @query-params)
             :page-name page-name
             :the-count-int the-count-int)]

     [:button {:class    ["bg-city-red-dark-700" "rounded" "shadow" "py-2" "px-4" "m-2"]
               :on-click #(let [new-count (inc the-count-int)]
                            (reset! the-count (if (int? new-count) new-count 0)))}
      (or @the-count "the-count!")]]))
