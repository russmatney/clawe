(ns doctor.ui.components.todos
  (:require
   [plasma.core :refer [defhandler]]
   #?@(:clj [[doctor.api.todos :as d.todos]]
       :cljs [[hiccup-icons.fa :as fa]
              [doctor.ui.components.icons :as icons]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API interactions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler open-in-emacs [todo]
  (println "open in emacs!!!")
  (println "opening file:" todo)
  (println "(TODO)")
  :ok)

(defhandler add-to-db [todo]
  (println "upserting-to-db" todo)
  (d.todos/upsert-todo-db todo)
  (d.todos/update-todos)
  :ok)

(defhandler mark-complete [todo]
  (println "marking-complete" todo)
  (-> todo
      (assoc :todo/status :status/done)
      (assoc :todo/last-completed-at (System/currentTimeMillis))
      d.todos/upsert-todo-db)
  (d.todos/update-todos)
  :ok)

(defhandler mark-in-progress [todo]
  (println "marking-in-progress" todo)
  (-> todo
      (assoc :todo/status :status/in-progress)
      (assoc :todo/last-started-at (System/currentTimeMillis))
      d.todos/upsert-todo-db)
  (d.todos/update-todos)
  :ok)

(defhandler mark-not-started [todo]
  (println "marking-not-started" todo)
  (-> todo
      (assoc :todo/status :status/not-started)
      (assoc :todo/last-stopped-at (System/currentTimeMillis))
      d.todos/upsert-todo-db)
  (d.todos/update-todos)
  :ok)

(defhandler mark-cancelled [todo]
  (println "marking-cancelled" todo)
  (-> todo
      (assoc :todo/status :status/cancelled)
      (assoc :todo/last-cancelled-at (System/currentTimeMillis))
      d.todos/upsert-todo-db)
  (d.todos/update-todos)
  :ok)

#?(:cljs
   (defn ->actions [todo]
     (let [{:keys [todo/status]} todo]
       (->>
         [;; TODO impl
          ;; {:action/label    "open-in-emacs"
          ;;  :action/on-click #(open-in-emacs todo)
          ;;  ;; :action/icon     fa/arrow-circle-down-solid
          ;;  }
          (when-not (:db/id todo)
            {:action/label    "add-to-db"
             :action/on-click #(add-to-db todo)})
          (when-not (#{:status/done} status)
            {:action/label    "mark-complete"
             :action/on-click #(mark-complete todo)
             :action/icon     fa/check-circle})
          (when-not (#{:status/in-progress} status)
            {:action/label    "mark-in-progress"
             :action/on-click #(mark-in-progress todo)
             :action/icon     fa/pencil-alt-solid})
          (when-not (#{:status/not-started} status)
            {:action/label    "mark-not-started"
             :action/on-click #(mark-not-started todo)
             :action/icon     fa/sticky-note})
          (when-not (#{:status/cancelled} status)
            {:action/label    "mark-cancelled"
             :action/on-click #(mark-cancelled todo)
             :action/icon     fa/ban-solid})]
         (remove nil?)))))


#?(:cljs
   (defn action-list [todo]
     (when-let [actions (->actions todo)]
       [:div
        {:class ["flex" "flex-row" "flex-wrap"]}
        (for [[i ax] (map-indexed vector actions)]
          ^{:key i}
          [icons/action-icon ax])])))
