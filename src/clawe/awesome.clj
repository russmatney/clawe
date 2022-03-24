(ns clawe.awesome
  ;; DEPRECATED this namespace should be completely excised in favor of
  ;; ralphie.awesome or some other awm-adapter/library
  (:require
   [clojure.string :as string]
   [ralphie.notify :as notify]
   [ralphie.awesome :as awm]
   [clawe.db.scratchpad :as db.scratchpad]))


(defn ignore-client?
  "These clients should not be buried or restored...
  (or interacted with at all, really, but that's going
  to take some work...)"
  [{:keys [name]}]
  (or
    (string/includes? name "meet.google.com")
    (string/includes? name "tauri/doctor-topbar")
    (string/includes? name "tauri/twitch-chat")))

(defn mark-buried-clients []
  (let [floating-clients
        (awm/awm-fnl
          {:quiet? true}
          '(-> (awful.screen.focused)
               (. :clients)
               (lume.filter (fn [c] c.floating))
               (lume.map (fn [c] {:window   c.window
                                  :name     c.name
                                  :class    c.class
                                  :instance c.instance
                                  :pid      c.pid
                                  :role     c.role}))
               view))]
    (->> floating-clients
         (remove ignore-client?)
         (map #(db.scratchpad/mark-buried (str (:window %)) %))
         doall)))

(defn focus-client
  "
  Focuses the passed client.
  Expects client as a map with `:window` or `:client/window`.

  Options:
  - :bury-all? - default: true.
    Sets all other clients ontop and floating to false
  - :float? - default: true.
    Set this client ontop and floating to true
  - :center? - default: true.
    Centers this client with awful
  "
  ([client] (focus-client nil client))
  ([opts client]
   (let [window    ((some-fn :window :client/window :awesome.client/window) client)
         bury-all? (:bury-all? opts true)
         float?    (:float? opts true)
         center?   (:center? opts true)]
     (if-not window
       (notify/notify "Set Focused called with no client :window" {:client client
                                                                   :opts   opts})
       (do
         (when bury-all? (mark-buried-clients))

         ^{:quiet? false}
         (awm/fnl
           (when ~bury-all?
             (each [c (awful.client.iterate (fn [c] (. c :ontop)))]
                   ;; TODO filter things to bury/not-bury?
                   (tset c :ontop false)
                   (tset c :floating false)))

           (each [c (awful.client.iterate (fn [c] (= (. c :window) ~window)))]

                 (when ~float?
                   (tset c :ontop true)
                   (tset c :floating true))

                 (when ~center?
                   (awful.placement.centered c))

                 ;; TODO set minimum height/width?
                 (tset _G.client :focus c))))))))

(comment
  (def c
    (->>
      (awm/fetch-tags)
      (filter (comp #{"clawe"} :awesome.tag/name))
      first
      :awesome.tag/clients
      first
      ))

  (focus-client {:center? false} c)

  )
