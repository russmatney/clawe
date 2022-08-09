(ns hooks.todos
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[api.todos :as todos]]
       :cljs [[hiccup-icons.fa :as fa]
              [doctor.ui.handlers :as handlers]
              [plasma.uix :refer [with-rpc with-stream]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Todos data api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-todos-handler [] (todos/get-todos))
(defstream todos-stream [] todos/*todos-stream*)

#?(:cljs
   (defn use-todos []
     (let [items       (plasma.uix/state [])
           handle-resp (fn [its] (reset! items its))]

       (with-rpc [] (get-todos-handler) handle-resp)
       (with-stream [] (todos-stream) handle-resp)

       {:items     @items
        :db-todos  (->> @items
                        (filter :db/id))
        :org-todos (->> @items
                        (remove :db/id))})))

#?(:clj
   (comment
     (set! *print-length* 100)
     (todos/get-todos)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; todo actions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler add-to-db [todo]
  (println "upserting-to-db" todo)
  (todos/upsert-todo-db todo)
  (todos/update-todos)
  :ok)

(defhandler mark-complete [todo]
  (println "marking-complete" todo)
  (-> todo
      (assoc :todo/status :status/done)
      (assoc :todo/last-completed-at (System/currentTimeMillis))
      todos/upsert-todo-db)
  (todos/update-todos)
  :ok)

(defhandler mark-in-progress [todo]
  (println "marking-in-progress" todo)
  (-> todo
      (assoc :todo/status :status/in-progress)
      (assoc :todo/last-started-at (System/currentTimeMillis))
      todos/upsert-todo-db)
  (todos/update-todos)
  :ok)

(defhandler mark-not-started [todo]
  (println "marking-not-started" todo)
  (-> todo
      (assoc :todo/status :status/not-started)
      (assoc :todo/last-stopped-at (System/currentTimeMillis))
      todos/upsert-todo-db)
  (todos/update-todos)
  :ok)

(defhandler mark-cancelled [todo]
  (println "marking-cancelled" todo)
  (-> todo
      (assoc :todo/status :status/cancelled)
      (assoc :todo/last-cancelled-at (System/currentTimeMillis))
      todos/upsert-todo-db)
  (todos/update-todos)
  :ok)

#?(:cljs
   (defn ->actions [todo]
     (let [{:keys [todo/status]} todo]
       (->>
         [{:action/label    "open-in-emacs"
           :action/on-click #(handlers/open-in-emacs todo)
           :action/icon     fa/arrow-circle-down-solid}
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
