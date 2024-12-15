(ns doctor.ui.views.focus
  (:require
   ["react-icons/fa6" :as FA]
   [taoensso.telemere :as log]
   [uix.core :as uix :refer [$ defui]]

   [util :as util]
   [doctor.ui.handlers :as handlers]
   [doctor.ui.db :as ui.db]
   [doctor.ui.hooks.use-db :as hooks.use-db]
   [components.actions :as components.actions]
   [components.garden :as components.garden]
   [components.todo :as todo]
   [components.item :as item]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current task (for topbar)

(defui current-task [_opts]
  (let [{:keys [data]} (hooks.use-db/use-query
                         {:db->data (fn [db] (ui.db/current-todos db))})
        current-todos  data
        current-todos  (todo/infer-actions {:no-popup true} current-todos)
        [n set-n]      (uix/use-state 0)]
    (log/log! {:data {:current-todos (count current-todos)}} "current task rendering")
    (uix/use-effect
      (fn [] (set-n #(util/clamp % 0 (dec (count current-todos)))))
      [n current-todos])
    (if-not (seq current-todos)
      ($ :span "--")
      (let [todos   (todo/sort-todos current-todos)
            current (get (into [] todos) n)
            ct      (count todos)]
        ($ :div
           {:class ["flex" "flex-row" "place-self-center"
                    "items-center" "space-x-4" "px-3"
                    "h-full"
                    "bg-city-orange-light-900"
                    "border"
                    "rounded-lg"
                    "border-city-orange-light-500"
                    ]}

           ($ todo/status current)

           ($ todo/priority-label current)

           ($ :div {:class ["ml-auto"]}
              ($ todo/tags-list current))

           ($ :div
              {:class ["font-mono pr-3" "whitespace-nowrap"]}
              (:org/name-string current))

           ($ :span
              {:class ["font-mono"]}
              (str (inc n) "/" ct))

           ($ components.actions/actions-list
              {:n 2 :hide-disabled true :nowrap true
               :actions
               (concat
                 (when (> ct 0)
                   [{:action/label    "next"
                     :action/icon     FA/FaChevronUp
                     :action/disabled (>= n (dec ct))
                     :action/on-click (fn [_] (set-n (inc n)))
                     :action/priority 5}
                    {:action/label    "prev"
                     :action/icon     FA/FaChevronDown
                     :action/disabled (zero? n)
                     :action/on-click (fn [_] (set-n (dec n)))
                     :action/priority 5}])
                 (handlers/->actions current))}))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current item header

(defui item-header [it]
  ($ :div
     {:class ["flex" "flex-col" "px-4"
              "text-city-green-400" "text-xl"]}

     ($ :div {:class ["pb-2" "flex" "flex-row"
                      "items-center" "justify-between"]}
        ($ item/parent-names it)
        ($ components.actions/actions-list
           {:actions       (handlers/->actions it)
            :nowrap        true
            :hide-disabled true}))

     ($ :div {:class ["flex" "flex-row" "items-center"]}
        ($ todo/level it)
        ($ todo/status it)
        ($ item/db-id it)
        ($ item/id-hash it)
        ($ :div {:class ["ml-auto"]}
           ($ todo/tags-list it))
        ($ todo/priority-label it))

     ($ :div {:class ["flex" "flex-row"]}
        ($ :span {:class ["font-nes"]}
           (:org/name-string it)))))

(defui item-body [it]
  ($ :div
     {:class ["text-xl" "p-4" "flex" "flex-col"]}
     ($ :div
        {:class ["text-yo-blue-200" "font-mono"]}
        ;; TODO include sub items + bodies
        #_ ($ :pre (:org/body-string it))
        ($ components.garden/org-body it))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current stack

(defui current-stack [todos]
  (when (seq todos)
    ($ :div
       (for [[i c] (->> todos todo/sort-todos (map-indexed vector))]
         ($ :div
            {:class ["bg-city-blue-800"]
             :key   i}
            ($ :hr {:class ["border-city-blue-900" "pb-4"]})
            ($ item-header c)
            ($ todo/card-or-card-group
               {:filter-by
                (comp not #{:status/skipped :status/done} :org/status)
                :item c})
            ($ item-body c))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; main widget

(defui widget [opts]
  (let [todos (ui.db/current-todos (:conn opts))
        todos (todo/infer-actions todos)]
    (if (seq todos)
      ($ current-stack todos)
      ($ :div
         {:class ["text-center" "my-36" "text-slate-200"]}
         ($ :div {:class ["font-nes"]} "No current task!")
         ($ :div {:class ["font-mono"]} "Maybe take a load off?")))))
